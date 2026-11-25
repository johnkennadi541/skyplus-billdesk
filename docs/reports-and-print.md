# Statements, exports and printing

Status: decided. Code for CSV exists and is unit tested; PDF and the A4 print
route are written in Step 4.

## Two formats, two jobs

| | PDF | CSV |
|---|---|---|
| Purpose | The record | The working copy |
| Editable | No | Yes, by anyone |
| Prints on A4 | Yes | Through Excel |
| Proves the numbers | Yes | Only with the hash line |

Both are generated from the same data at the same moment, so they agree at the
moment of export.

### Why not Word or Excel

- **Word (DOCX)** - nobody needs an editable statement, and it only creates a way
  for the numbers to change after the fact.
- **Excel (XLSX)** - needs a heavy library on Android (Apache POI adds several MB
  and is slow on cheap devices). CSV does the same job for almost nothing.

## The CSV rules

1. **UTF-8 with a BOM**, so Excel on Windows shows Tamil and the rupee sign.
2. **Amounts as plain numbers** - `1234.50`, never `1,234.50`. Excel splits on
   commas, and a formatted amount silently becomes two columns.
3. **Cells quoted only when needed**, with inner quotes doubled. An item called
   "Paneer Tikka, dry" stays one field.
4. **A trailing hash line**: `#sha256,<hex>` computed over everything above it.
   Edit a number and the hash no longer matches. `CsvWriter.isUnmodified()` checks
   it. Note that Excel will show this line as a final row - that is expected.
5. **Dates** as `dd/MM/yyyy HH:mm` in Asia/Kolkata.

## Printing

### 58mm thermal bill

Separate path: `BillPrinter` + ESC/POS, or the Sunmi / iMin SDK. Not the Android
print framework, which cannot see a POS terminal's built-in printer.

### A4 statement

1. **Android print framework** - `PrintManager` with a `PrintDocumentAdapter` and
   `PrintAttributes.MediaSize.ISO_A4`. Part of the OS, no library, reaches any
   printer the phone can see.
2. **PDF share as fallback** - if the ROM has no print service (common on cheap
   POS terminals), the PDF is still generated and can be shared to WhatsApp or
   email. A print button that fails silently is worse than no print button.

## What the statement contains

- Header: restaurant name, address, GSTIN if registered, statement period
- Sales summary: bill count, gross, discount, voids, net collected
- Payment split: cash, card, UPI
- Item-wise sales: quantity and amount
- Tax summary: taxable value and tax split, from settings - never hardcoded,
  because GST rules for restaurants have changed more than once recently
- Audit lines: who applied each discount, who voided each bill

## Not in v0.1

- Scheduled or emailed statements
- XLSX export
- Direct printing to a network printer by IP (the OS print service handles it)
