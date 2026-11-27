package com.skyplus.billdesk.sync

import com.skyplus.billdesk.data.entity.EventLogEntity
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException

/** Versioned bounded binary framing for a trusted, paired local Wi-Fi connection. */
object EventWireCodec {
    private const val MAGIC = 0x534B5945 // SKYE
    private const val VERSION = 1
    private const val MAX_EVENTS = 20_000

    fun write(output: DataOutputStream, events: Collection<EventLogEntity>) {
        require(events.size <= MAX_EVENTS) { "Event batch too large" }
        output.writeInt(MAGIC)
        output.writeInt(VERSION)
        output.writeInt(events.size)
        EventOrdering.sort(events).forEach { event ->
            output.writeUTF(event.eventId)
            output.writeUTF(event.deviceId)
            output.writeLong(event.seq)
            output.writeLong(event.timestampEpochMillis)
            output.writeUTF(event.eventType)
            output.writeUTF(event.payload)
        }
        output.flush()
    }

    fun read(input: DataInputStream): List<EventLogEntity> {
        try {
            require(input.readInt() == MAGIC) { "Not a SKYPLUS event stream" }
            require(input.readInt() == VERSION) { "Unsupported event stream version" }
            val count = input.readInt()
            require(count in 0..MAX_EVENTS) { "Invalid event count" }
            val events = ArrayList<EventLogEntity>(count)
            repeat(count) {
                events += EventLogEntity(
                    eventId = input.readUTF(), deviceId = input.readUTF(), seq = input.readLong(),
                    timestampEpochMillis = input.readLong(), eventType = input.readUTF(), payload = input.readUTF()
                )
            }
            return EventOrdering.sort(events)
        } catch (e: EOFException) {
            throw IllegalArgumentException("Truncated event stream", e)
        }
    }
}
