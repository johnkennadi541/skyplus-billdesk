package com.skyplus.billdesk.printing

import com.skyplus.billdesk.data.dao.PrintQueueDao
import com.skyplus.billdesk.data.entity.PrintJobEntity
import kotlinx.coroutines.delay
import java.util.UUID

sealed interface PrinterConnection {
    data class Tcp(val host: String, val port: Int = 9100) : PrinterConnection
    data class BluetoothSpp(val address: String) : PrinterConnection
    data class Usb(val vendorId: Int, val productId: Int) : PrinterConnection
}

enum class PrintStatus { QUEUED, SENDING, SENT_TO_PRINTER, RETRYING, FAILED }
data class PrintJobStatus(val jobId: String, val status: PrintStatus, val operatorMessage: String)

/** Transport writes one whole job over one connection, pacing each chunk. */
interface PrinterTransport {
    suspend fun write(connection: PrinterConnection, chunks: List<ByteArray>, pacingMillis: Long)
}

interface BluetoothSppAdapter { suspend fun writeChunks(address: String, chunks: List<ByteArray>, pacingMillis: Long) }
interface UsbPrinterAdapter { suspend fun writeChunks(vendorId: Int, productId: Int, chunks: List<ByteArray>, pacingMillis: Long) }

object EscPosChunker {
    fun chunks(payload: ByteArray, chunkSize: Int = 128): List<ByteArray> {
        require(chunkSize > 0)
        return payload.asList().chunked(chunkSize).map { it.toByteArray() }
    }
}

class EscPosPrinterTransport(
    private val bluetooth: BluetoothSppAdapter? = null,
    private val usb: UsbPrinterAdapter? = null,
    private val connectTimeoutMillis: Int = 3_000
) : PrinterTransport {
    override suspend fun write(connection: PrinterConnection, chunks: List<ByteArray>, pacingMillis: Long) {
        require(pacingMillis >= 0)
        when (connection) {
            is PrinterConnection.Tcp -> withContext(kotlinx.coroutines.Dispatchers.IO) {
                java.net.Socket().use { socket ->
                    socket.connect(java.net.InetSocketAddress(connection.host, connection.port), connectTimeoutMillis)
                    val output = socket.getOutputStream()
                    chunks.forEachIndexed { index, chunk ->
                        output.write(chunk); output.flush()
                        if (pacingMillis > 0 && index < chunks.lastIndex) delay(pacingMillis)
                    }
                }
            }
            is PrinterConnection.BluetoothSpp -> requireNotNull(bluetooth) { "Bluetooth SPP adapter is not installed" }
                .writeChunks(connection.address, chunks, pacingMillis)
            is PrinterConnection.Usb -> requireNotNull(usb) { "USB printer adapter is not installed" }
                .writeChunks(connection.vendorId, connection.productId, chunks, pacingMillis)
        }
    }
}

interface PrintQueueStore {
    suspend fun enqueue(job: PrintJobEntity)
    suspend fun nextReady(now: Long): PrintJobEntity?
    suspend fun update(id: String, status: String, attempts: Int, nextAt: Long, error: String?)
}

class RoomPrintQueueStore(private val dao: PrintQueueDao) : PrintQueueStore {
    override suspend fun enqueue(job: PrintJobEntity) = dao.enqueue(job)
    override suspend fun nextReady(now: Long) = dao.claimable(now)
    override suspend fun update(id: String, status: String, attempts: Int, nextAt: Long, error: String?) = dao.update(id, status, attempts, nextAt, error)
}

/** Bounded at-least-once queue. A retry after partial output can duplicate a ticket; show status, never claim physical printing. */
class PrintQueueWorker(
    private val store: PrintQueueStore,
    private val transport: PrinterTransport,
    private val connectionFor: (String) -> PrinterConnection,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val chunkSize: Int = 128,
    private val pacingMillis: Long = 20,
    private val maxAttempts: Int = 4
) {
    init { require(chunkSize > 0 && pacingMillis >= 0 && maxAttempts > 0) }

    suspend fun enqueue(printerId: String, payload: ByteArray, jobId: String = UUID.randomUUID().toString()) {
        require(printerId.isNotBlank() && payload.isNotEmpty())
        store.enqueue(PrintJobEntity(jobId, printerId, payload.copyOf()))
    }

    suspend fun runOne(): PrintJobStatus? {
        val job = store.nextReady(nowMillis()) ?: return null
        val attempt = job.attempts + 1
        store.update(job.jobId, "SENDING", job.attempts, job.nextAttemptAtEpochMillis, null)
        return try {
            transport.write(connectionFor(job.printerId), EscPosChunker.chunks(job.payload, chunkSize), pacingMillis)
            store.update(job.jobId, "SENT_TO_PRINTER", attempt, 0, null)
            PrintJobStatus(job.jobId, PrintStatus.SENT_TO_PRINTER, "Sent to Printer")
        } catch (failure: Exception) {
            val failed = attempt >= maxAttempts
            val status = if (failed) PrintStatus.FAILED else PrintStatus.RETRYING
            val next = if (failed) 0 else nowMillis() + RETRY_BASE_MILLIS * attempt
            store.update(job.jobId, status.name, attempt, next, failure.message)
            PrintJobStatus(job.jobId, status, if (failed) "Print Failed — Check Paper/Connection" else "Retrying...")
        }
    }

    companion object { private const val RETRY_BASE_MILLIS = 1_000L }
}
