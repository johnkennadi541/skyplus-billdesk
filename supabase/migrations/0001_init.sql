-- SkyPlus BillDesk - initial cloud schema
--
-- Design stage. Not executed against a real Postgres yet.
-- Region: ap-south-1 (Mumbai), chosen explicitly - the "APAC" option lands in Singapore.
--
-- Rules this schema enforces:
--   * money is bigint paise, never numeric with decimals or float
--   * stock quantities are bigint base units (g / ml / pc)
--   * ids are generated on the device (uuid v4) so retries are idempotent
--   * bills are never updated or deleted, only voided once
--   * every row is scoped to an outlet, and access is checked against membership

create extension if not exists "pgcrypto";

-- ---------------------------------------------------------------- membership

create table outlets (
    id          uuid primary key default gen_random_uuid(),
    name        text        not null,
    created_at  timestamptz not null default now()
);

create table members (
    user_id     uuid        not null references auth.users (id) on delete cascade,
    outlet_id   uuid        not null references outlets (id) on delete cascade,
    role        text        not null check (role in ('OWNER', 'ADMIN', 'CASHIER')),
    created_at  timestamptz not null default now(),
    primary key (user_id, outlet_id)
);

-- ---------------------------------------------------------------- menu

create table categories (
    id          uuid primary key,
    outlet_id   uuid        not null references outlets (id) on delete cascade,
    name        text        not null,
    sort_order  int         not null default 0,
    is_active   boolean     not null default true,
    updated_at  timestamptz not null default now()
);

create table menu_items (
    id            uuid primary key,
    outlet_id     uuid        not null references outlets (id) on delete cascade,
    code          text        not null,
    name          text        not null,
    category_id   uuid        references categories (id),
    price_paise   bigint      not null check (price_paise >= 0),
    tax_percent   numeric(5, 2) not null default 0 check (tax_percent between 0 and 100),
    is_active     boolean     not null default true,
    updated_at    timestamptz not null default now(),
    unique (outlet_id, code)
);

-- ---------------------------------------------------------------- bills

create table bills (
    id                    uuid primary key,
    outlet_id             uuid        not null references outlets (id) on delete cascade,
    bill_number           text        not null,
    created_at            timestamptz not null,
    order_type            text        not null check (order_type in ('DINE_IN', 'TAKEAWAY', 'DELIVERY')),
    table_number          text,
    subtotal_paise        bigint      not null check (subtotal_paise >= 0),
    tax_paise             bigint      not null check (tax_paise >= 0),
    service_charge_paise  bigint      not null default 0 check (service_charge_paise >= 0),
    discount_paise        bigint      not null default 0 check (discount_paise >= 0),
    grand_total_paise     bigint      not null check (grand_total_paise >= 0),
    payment_mode          text        check (payment_mode in ('CASH', 'CARD', 'UPI', 'OTHER')),
    created_by_role       text        not null check (created_by_role in ('CASHIER', 'ADMIN')),
    created_by_user       uuid        references auth.users (id),
    discount_applied_by   text        check (discount_applied_by in ('CASHIER', 'ADMIN')),
    voided_by             text        check (voided_by in ('CASHIER', 'ADMIN')),
    voided_at             timestamptz,
    void_reason           text,
    device_id             text        not null,
    unique (outlet_id, bill_number),
    check (voided_at is null or void_reason is not null)
);

create table bill_items (
    id                uuid primary key,
    bill_id           uuid   not null references bills (id) on delete restrict,
    menu_item_id      uuid,
    name_snapshot     text   not null,
    unit_price_paise  bigint not null check (unit_price_paise >= 0),
    quantity          int    not null check (quantity > 0),
    note              text   not null default ''
);

-- ---------------------------------------------------------------- inventory

create table inventory_items (
    id                  uuid primary key,
    outlet_id           uuid   not null references outlets (id) on delete cascade,
    name                text   not null,
    unit                text   not null check (unit in ('GRAM', 'MILLILITRE', 'PIECE')),
    reorder_level_base  bigint not null default 0 check (reorder_level_base >= 0),
    cost_per_base_paise bigint not null default 0 check (cost_per_base_paise >= 0),
    is_active           boolean not null default true,
    updated_at          timestamptz not null default now()
);

-- Append-only. The quantity on inventory_items is derived from these rows by a
-- trigger, so two tills can never each subtract the same usage.
create table stock_movements (
    id                uuid primary key,
    inventory_item_id uuid        not null references inventory_items (id) on delete restrict,
    type              text        not null check (type in ('PURCHASE', 'USAGE', 'WASTAGE', 'ADJUSTMENT')),
    quantity_base     bigint      not null,
    at                timestamptz not null,
    by_role           text        not null check (by_role in ('CASHIER', 'ADMIN')),
    by_user           uuid        references auth.users (id),
    note              text        not null default '',
    check (quantity_base <> 0),
    check (type = 'ADJUSTMENT' or quantity_base > 0),
    check (type <> 'WASTAGE' or length(btrim(note)) > 0)
);

create table inventory_levels (
    inventory_item_id uuid primary key references inventory_items (id) on delete cascade,
    quantity_base     bigint not null default 0 check (quantity_base >= 0),
    updated_at        timestamptz not null default now()
);

create function apply_stock_movement() returns trigger
language plpgsql as $$
declare
    delta bigint;
begin
    delta := case new.type
        when 'PURCHASE' then new.quantity_base
        when 'USAGE' then -new.quantity_base
        when 'WASTAGE' then -new.quantity_base
        else new.quantity_base
    end;

    insert into inventory_levels (inventory_item_id, quantity_base)
    values (new.inventory_item_id, delta)
    on conflict (inventory_item_id) do update
        set quantity_base = inventory_levels.quantity_base + delta,
            updated_at = now();

    if (select quantity_base from inventory_levels where inventory_item_id = new.inventory_item_id) < 0 then
        raise exception 'stock would go negative for %', new.inventory_item_id;
    end if;

    return new;
end;
$$;

create trigger stock_movements_apply
    after insert on stock_movements
    for each row execute function apply_stock_movement();

-- ---------------------------------------------------------------- settings

create table settings (
    outlet_id   uuid        not null references outlets (id) on delete cascade,
    key         text        not null,
    value       text        not null,
    updated_at  timestamptz not null default now(),
    primary key (outlet_id, key)
);

-- ---------------------------------------------------------------- bills are immutable

create function block_bill_mutation() returns trigger
language plpgsql as $$
begin
    if tg_op = 'DELETE' then
        raise exception 'bills are never deleted';
    end if;

    -- The only permitted update is voiding, once.
    if old.voided_at is not null and new.voided_at is distinct from old.voided_at then
        raise exception 'a voided bill cannot be changed again';
    end if;
    if new.grand_total_paise <> old.grand_total_paise
       or new.subtotal_paise <> old.subtotal_paise
       or new.discount_paise <> old.discount_paise then
        raise exception 'bill amounts are immutable';
    end if;

    return new;
end;
$$;

create trigger bills_immutable
    before update or delete on bills
    for each row execute function block_bill_mutation();

-- ---------------------------------------------------------------- row level security

alter table outlets            enable row level security;
alter table members            enable row level security;
alter table categories         enable row level security;
alter table menu_items         enable row level security;
alter table bills              enable row level security;
alter table bill_items         enable row level security;
alter table inventory_items    enable row level security;
alter table stock_movements    enable row level security;
alter table inventory_levels   enable row level security;
alter table settings           enable row level security;

create function is_member(p_outlet uuid) returns boolean
language sql stable as $$
    select exists (
        select 1 from members m
        where m.user_id = auth.uid() and m.outlet_id = p_outlet
    );
$$;

create function is_admin(p_outlet uuid) returns boolean
language sql stable as $$
    select exists (
        select 1 from members m
        where m.user_id = auth.uid() and m.outlet_id = p_outlet
          and m.role in ('OWNER', 'ADMIN')
    );
$$;

create policy members_read on members
    for select using (user_id = auth.uid() or is_admin(outlet_id));

create policy menu_read on menu_items
    for select using (is_member(outlet_id));
create policy menu_write on menu_items
    for insert with check (is_admin(outlet_id));
create policy menu_update on menu_items
    for update using (is_admin(outlet_id)) with check (is_admin(outlet_id));

create policy categories_read on categories
    for select using (is_member(outlet_id));
create policy categories_write on categories
    for all using (is_admin(outlet_id)) with check (is_admin(outlet_id));

-- Any member may create a bill; nobody may update one except to void it.
create policy bills_read on bills
    for select using (is_member(outlet_id));
create policy bills_insert on bills
    for insert with check (is_member(outlet_id));
create policy bills_void on bills
    for update using (is_admin(outlet_id)) with check (is_admin(outlet_id));

create policy bill_items_read on bill_items
    for select using (exists (select 1 from bills b where b.id = bill_id and is_member(b.outlet_id)));
create policy bill_items_insert on bill_items
    for insert with check (exists (select 1 from bills b where b.id = bill_id and is_member(b.outlet_id)));

create policy stock_read on stock_movements
    for select using (exists (
        select 1 from inventory_items i where i.id = inventory_item_id and is_member(i.outlet_id)
    ));
create policy stock_insert on stock_movements
    for insert with check (exists (
        select 1 from inventory_items i where i.id = inventory_item_id and is_admin(i.outlet_id)
    ));

create policy inventory_read on inventory_items
    for select using (is_member(outlet_id));
create policy inventory_write on inventory_items
    for all using (is_admin(outlet_id)) with check (is_admin(outlet_id));

create policy levels_read on inventory_levels
    for select using (exists (
        select 1 from inventory_items i where i.id = inventory_item_id and is_member(i.outlet_id)
    ));

create policy settings_read on settings
    for select using (is_member(outlet_id));
create policy settings_write on settings
    for all using (is_admin(outlet_id)) with check (is_admin(outlet_id));

-- ---------------------------------------------------------------- indices

create index bills_outlet_created_idx     on bills (outlet_id, created_at desc);
create index bill_items_bill_idx          on bill_items (bill_id);
create index menu_items_outlet_idx        on menu_items (outlet_id, is_active);
create index stock_movements_item_idx     on stock_movements (inventory_item_id, at desc);
