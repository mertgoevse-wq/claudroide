package org.claudroide.app.control

import org.claudroide.app.feature.control.music.MidiNote
import org.claudroide.app.feature.control.music.MidiSequence
import org.claudroide.app.feature.control.music.MidiTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MidiWriterTest {

    @Test
    fun `generates valid MIDI file bytes with MThd and MTrk chunks`() {
        val notes = listOf(
            MidiNote(pitch = 60, startTick = 0, durationTicks = 480, velocity = 100),
            MidiNote(pitch = 64, startTick = 480, durationTicks = 480, velocity = 100),
            MidiNote(pitch = 67, startTick = 960, durationTicks = 480, velocity = 100)
        )
        val track = MidiTrack(name = "Lead Synth", notes = notes, instrumentProgram = 81)
        val sequence = MidiSequence(bpm = 128, tracks = listOf(track))

        val bytes = sequence.toMidiByteArray()

        // 1. Check header "MThd"
        val headerTag = String(bytes.sliceArray(0..3), Charsets.US_ASCII)
        assertEquals("MThd", headerTag)

        // 2. Check header length (6 bytes)
        val headerLength = (bytes[4].toInt() shl 24) or (bytes[5].toInt() shl 16) or (bytes[6].toInt() shl 8) or bytes[7].toInt()
        assertEquals(6, headerLength)

        // 3. Check format (Format 1)
        val format = (bytes[8].toInt() shl 8) or (bytes[9].toInt() and 0xFF)
        assertEquals(1, format)

        // 4. Check track count (conductor track + 1 note track = 2 tracks)
        val trackCount = (bytes[10].toInt() shl 8) or (bytes[11].toInt() and 0xFF)
        assertEquals(2, trackCount)

        // 5. Check track chunk "MTrk"
        val track1Tag = String(bytes.sliceArray(14..17), Charsets.US_ASCII)
        assertEquals("MTrk", track1Tag)

        // Ensure total bytes > 40
        assertTrue(bytes.size > 40)
    }
}
