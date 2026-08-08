package com.example

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class DeterministicRoomTest {

    private fun generateDeterministicRoomId(id1: String, id2: String): String {
        val clean1 = id1.trim().uppercase(Locale.ROOT)
        val clean2 = id2.trim().uppercase(Locale.ROOT)
        val sorted = listOf(clean1, clean2).sorted()
        return "room_${sorted[0]}_${sorted[1]}"
    }

    @Test
    fun deterministicRoomId_sameRoomRegardlessOfOrder() {
        val idA = "7A9K2M"
        val idB = "3B1Z4M"

        val roomFromUserA = generateDeterministicRoomId(idA, idB)
        val roomFromUserB = generateDeterministicRoomId(idB, idA)

        assertEquals("room_3B1Z4M_7A9K2M", roomFromUserA)
        assertEquals(roomFromUserA, roomFromUserB)
    }
}
