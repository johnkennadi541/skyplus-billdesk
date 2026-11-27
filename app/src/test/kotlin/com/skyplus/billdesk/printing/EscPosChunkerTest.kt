package com.skyplus.billdesk.printing

import org.junit.Assert.*
import org.junit.Test

class EscPosChunkerTest {
    @Test fun splitsInto128ByteChunksWithoutLoss() {
        val input = ByteArray(513) { (it % 256).toByte() }
        val parts = EscPosChunker.chunks(input)
        assertEquals(listOf(128, 128, 128, 128, 1), parts.map { it.size })
        assertArrayEquals(input, parts.fold(byteArrayOf()) { all, chunk -> all + chunk })
    }
    @Test fun emptyPayloadCreatesNoPackets() { assertTrue(EscPosChunker.chunks(byteArrayOf()).isEmpty()) }
}
