package com.skyplus.billdesk.sync

import com.skyplus.billdesk.data.dao.EventSyncDao
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException

/** Two-peer, pull-and-push exchange. Billing/KOT writes remain local and never wait for this service. */
class LocalWifiEventSync(private val dao: EventSyncDao) {
    fun syncWith(host: String, port: Int, timeoutMillis: Int = DEFAULT_TIMEOUT_MILLIS): Int {
        require(host.isNotBlank() && port in 1..65535)
        Socket().use { socket ->
            socket.connect(java.net.InetSocketAddress(host, port), timeoutMillis)
            socket.soTimeout = timeoutMillis
            EventWireCodec.write(java.io.DataOutputStream(socket.getOutputStream().buffered()), dao.allEventsBlocking())
            val remote = EventWireCodec.read(java.io.DataInputStream(socket.getInputStream().buffered()))
            return dao.mergeIncomingBlocking(remote)
        }
    }

    /** Handles one peer; keep accepting on a background executor for an always-available host. */
    fun serveOnce(server: ServerSocket, timeoutMillis: Int = DEFAULT_TIMEOUT_MILLIS): Int {
        server.soTimeout = timeoutMillis
        val socket = try { server.accept() } catch (e: SocketTimeoutException) { return 0 }
        socket.use {
            it.soTimeout = timeoutMillis
            val incoming = EventWireCodec.read(java.io.DataInputStream(it.getInputStream().buffered()))
            val inserted = dao.mergeIncomingBlocking(incoming)
            EventWireCodec.write(java.io.DataOutputStream(it.getOutputStream().buffered()), dao.allEventsBlocking())
            return inserted
        }
    }

    companion object { const val DEFAULT_TIMEOUT_MILLIS = 10_000 }
}
