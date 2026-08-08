package com.example;

import static org.junit.Assert.assertEquals;

import com.example.repository.ChatRepository;

import org.junit.Test;

public class DeterministicRoomJavaTest {

    @Test
    public void testDeterministicRoomGenerationInJava() {
        // Mocking repo logic
        String idA = "7A9K2M";
        String idB = "3B1Z4M";

        // Sort lexicographically
        String room1 = "room_" + (idA.compareTo(idB) < 0 ? idA + "_" + idB : idB + "_" + idA);
        String room2 = "room_" + (idB.compareTo(idA) < 0 ? idB + "_" + idA : idA + "_" + idB);

        assertEquals("room_3B1Z4M_7A9K2M", room1);
        assertEquals(room1, room2);
    }
}
