package com.skyplus.billdesk.report

/**
 * The two shapes a statement leaves the app in.
 *
 * They are not duplicates - they do different jobs:
 *  - [PDF] is the record. Not editable, prints on A4, and is what you send when
 *    someone asks for proof.
 *  - [CSV] is the working copy. The accountant opens it in Excel or Sheets and
 *    does whatever they need. It is editable by anyone, so on its own it proves
 *    nothing - which is why the CSV carries a hash line.
 */
enum class ReportFormat {
    PDF,
    CSV
}
