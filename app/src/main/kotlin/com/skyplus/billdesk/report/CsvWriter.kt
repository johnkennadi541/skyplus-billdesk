package com.skyplus.billdesk.report

import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

/**
 * Builds a CSV an accountant can actually open.
 *
 * Three decisions, each of which is a support call if it is done wrong:
 *
 *  1. **UTF-8 with a BOM.** Without the BOM, Excel on Windows shows Tamil and the
 *     rupee sign as garbage.
 *  2. **Amounts as plain numbers.** "1234.50", never "1,234.50" and never with a
 *     currency symbol - Excel splits on commas, so a formatted amount silently
 *     becomes two columns and every total is wrong.
 *  3. **A hash line at the end.** A CSV can be edited by anyone, so a statement
 *     that has been changed has to be detectable. The last line carries the
 *     SHA-256 of everything above it.
 */
object CsvWriter {

    /** Byte order mark. Invisible, and the difference between readable and garbled. */
    const val BOM: String = "\uFEFF"

    private const val NEWLINE = "\r\n"
    private const val HASH_PREFIX = "#sha256,"

    /**
     * One complete CSV document.
     *
     * @param withHash append the trailing integrity line (default true)
     */
    fun write(
        header: List<String>,
        rows: List<List<String>>,
        withHash: Boolean = true
    ): String {
        require(header.isNotEmpty()) { "A CSV needs a header row" }
        rows.forEachIndexed { index, row ->
            require(row.size == header.size) {
                "Row ${index + 1} has ${row.size} cells but the header has ${header.size}"
            }
        }

        val body = buildString {
            append(line(header))
            rows.forEach { row ->
                append(NEWLINE)
                append(line(row))
            }
        }

        if (!withHash) return BOM + body
        return BOM + body + NEWLINE + HASH_PREFIX + sha256Hex(body)
    }

    /** SHA-256 of the body, lowercase hex. */
    fun sha256Hex(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }

    /** The hash carried by a document built here, or null if it has none. */
    fun hashOf(csv: String): String? {
        val withoutBom = csv.removePrefix(BOM)
        val index = withoutBom.lastIndexOf(HASH_PREFIX)
        if (index < 0) return null
        return withoutBom.substring(index + HASH_PREFIX.length).trim()
    }

    /** True when the document has not been edited since it was written. */
    fun isUnmodified(csv: String): Boolean {
        val withoutBom = csv.removePrefix(BOM)
        val index = withoutBom.lastIndexOf(HASH_PREFIX)
        if (index < 0) return false
        val body = withoutBom.substring(0, index - NEWLINE.length)
        return sha256Hex(body) == withoutBom.substring(index + HASH_PREFIX.length).trim()
    }

    /** Paise to a plain two-decimal amount: 123450 -> "1234.50". */
    fun amount(paise: Long): String {
        val negative = paise < 0
        val value = abs(paise)
        val text = (value / 100).toString() + "." + (value % 100).toString().padStart(2, '0')
        return if (negative) "-$text" else text
    }

    /** A whole number with no grouping, for quantities and counts. */
    fun number(value: Long): String = value.toString()

    /** dd/MM/yyyy HH:mm in the given zone, because that is how the bill reads. */
    fun timestamp(epochMillis: Long, timeZoneId: String = "Asia/Kolkata"): String {
        val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US)
        format.timeZone = TimeZone.getTimeZone(timeZoneId)
        return format.format(Date(epochMillis))
    }

    /** Quotes a cell only when it has to be quoted, doubling inner quotes. */
    fun cell(value: String): String {
        val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuotes) return value
        return '"' + value.replace("\"", "\"\"") + '"'
    }

    private fun line(cells: List<String>): String =
        cells.joinToString(",") { cell(it) }
}
