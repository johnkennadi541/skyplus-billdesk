package com.skyplus.billdesk.printing

import com.skyplus.billdesk.model.Bill

/**
 * One interface, one implementation per device.
 *
 * Step 1 ships [PreviewBillPrinter] only. Android's print framework cannot see
 * a POS terminal's built-in thermal printer, so Sunmi / iMin ESC/POS adapters
 * are written against this interface once the exact terminal model is known.
 */
interface BillPrinter {
    suspend fun print(bill: Bill): PrintResult
}

sealed interface PrintResult {
    data class Success(val message: String) : PrintResult
    data class Failure(val reason: String) : PrintResult
}

/** Default for Step 1: nothing leaves the device. */
class PreviewBillPrinter : BillPrinter {
    override suspend fun print(bill: Bill): PrintResult =
        PrintResult.Success("Preview only - no printer configured")
}
