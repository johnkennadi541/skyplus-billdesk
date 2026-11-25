# Login and accounts

Status: decided and partly implemented. No Google Cloud, no Firebase.

## Who logs in with what

| Profile | Login | Where it lives |
|---|---|---|
| Cashier | 4-digit PIN | On the device only |
| Admin | Email + password | Supabase Auth (Postgres, region `ap-south-1` Mumbai) |

## Why not Sign in with Google

Google sign-in was the first plan. It was dropped on the owner's instruction, and it
was the right call for four reasons:

1. It needs a **Google Cloud project**, an OAuth consent screen and an OAuth client.
2. The Android OAuth client is bound to the **SHA-1 of the signing key**, so it breaks
   the moment the key changes, and it needs one client per key.
3. It needs **Google Play services on the device**. Google's own documentation says
   devices without the Play Store do not have Play services, and many Android POS
   terminals are exactly that. On those tills the button would simply not work.
4. Google requires **brand verification** of the consent screen before the app name is
   shown, which can take days.

Email and password through Supabase removes all four. No Google account, no
fingerprint, no Play services, no brand review. The admin can sign in on any device,
including a POS terminal with no Play Store.

## Why the cashier stays on a PIN

- Speed. A password at 8pm on a busy Friday costs seconds on every bill.
- The cashier does not need a verified identity; the admin does.
- A PIN works with no network. A cloud sign-in does not.

The cashier's PIN is hashed on the device (PBKDF2, 120,000 iterations, per-profile
salt) and never leaves it.

## Rules

1. **Sign-ups are disabled in Supabase.** The owner's account is created by hand, in
   the Supabase dashboard. Nobody can register into this app.
2. **A cloud account alone never grants admin.** The account must also be on the
   device's admin allow-list.
3. **The admin password is never stored in the app.** Supabase issues a session token;
   only the refresh token is kept, in encrypted storage.
4. **Offline:** if the admin cannot reach the network at open, the device falls back to
   the admin PIN, and sync resumes after the next successful sign-in. A till that
   cannot be opened is worse than one with a weaker login.
5. **Password reset** goes through Supabase's email reset link, which returns through
   the deep link `com.skyplus.billdesk://login-callback`.

## Setup still needed

1. A Supabase project, region `ap-south-1` (Mumbai). Free plan is enough to build.
2. Authentication → Providers → Email: enabled, "Confirm email" on, "Allow new users to
   sign up" **off**.
3. Create the owner's account in the dashboard.
4. Add the project URL and the **anon** key to the app. The anon key is public by
   design and safe in a client; the **service_role key is never** put in the app.

No Google Cloud project, no OAuth client, no SHA-1 fingerprint, no Firebase.
