package org.claudroide.app.feature.control.music

import java.io.ByteArrayOutputStream
import java.io.OutputStream

/**
 * Single MIDI note event with pitch, timing, duration, and velocity.
 */
data class MidiNote(
    val pitch: Int,             // 0-127 (e.g., 60 = Middle C)
    val startTick: Long,        // Position in ticks from track start
    val durationTicks: Long,    // Length in ticks
    val velocity: Int = 100,    // 1-127
    val channel: Int = 0        // 0-15
) {
    val endTick: Long get() = startTick + durationTicks
}

/**
 * A track within a MIDI sequence containing notes and metadata.
 */
data class MidiTrack(
    val name: String,
    val notes: List<MidiNote>,
    val instrumentProgram: Int = 0  // General MIDI program (0 = Acoustic Grand Piano, 33 = Electric Bass, etc.)
)

/**
 * Complete multi-track MIDI sequence ready for SMF binary serialization.
 */
data class MidiSequence(
    val name: String = "Claudroide Sequence",
    val bpm: Int = 128,
    val ticksPerQuarterNote: Int = 480,
    val timeSignatureNumerator: Int = 4,
    val timeSignatureDenominator: Int = 4,
    val tracks: List<MidiTrack>
) {
    /**
     * Serializes this sequence to standard binary Standard MIDI File (.mid Format 1).
     */
    fun toMidiByteArray(): ByteArray {
        val out = ByteArrayOutputStream()
        MidiWriter.writeSequence(this, out)
        return out.toByteArray()
    }
}

/**
 * Standard MIDI File (SMF) binary format encoder.
 */
object MidiWriter {

    fun writeSequence(sequence: MidiSequence, out: OutputStream) {
        // 1. Header chunk: MThd
        out.write("MThd".toByteArray(Charsets.US_ASCII))
        writeInt32(out, 6) // Header chunk size is always 6 bytes
        writeInt16(out, 1) // Format 1: multiple simultaneous tracks
        val totalTracks = sequence.tracks.size + 1 // Track 0 (tempo/time sig) + content tracks
        writeInt16(out, totalTracks)
        writeInt16(out, sequence.ticksPerQuarterNote)

        // 2. Conductor / Tempo Track (Track 0)
        writeConductorTrack(sequence, out)

        // 3. Content Tracks
        for (track in sequence.tracks) {
            writeNoteTrack(track, sequence.ticksPerQuarterNote, out)
        }
    }

    private fun writeConductorTrack(sequence: MidiSequence, out: OutputStream) {
        val trackBytes = ByteArrayOutputStream()

        // Time Signature: Delta 0, FF 58 04 nn dd cc bb
        writeVarLen(trackBytes, 0)
        trackBytes.write(0xFF)
        trackBytes.write(0x58)
        trackBytes.write(0x04)
        trackBytes.write(sequence.timeSignatureNumerator)
        // denominator as power of 2 (4 = 2^2 -> 2)
        val denomPower = (Math.log(sequence.timeSignatureDenominator.toDouble()) / Math.log(2.0)).toInt()
        trackBytes.write(denomPower)
        trackBytes.write(24) // 24 MIDI clocks per metronome click
        trackBytes.write(8)  // 8 32nd notes per quarter note

        // Set Tempo: Delta 0, FF 51 03 tt tt tt (microseconds per quarter note)
        val microsecondsPerQuarter = (60_000_000L / sequence.bpm).toInt()
        writeVarLen(trackBytes, 0)
        trackBytes.write(0xFF)
        trackBytes.write(0x51)
        trackBytes.write(0x03)
        trackBytes.write((microsecondsPerQuarter shr 16) and 0xFF)
        trackBytes.write((microsecondsPerQuarter shr 8) and 0xFF)
        trackBytes.write(microsecondsPerQuarter and 0xFF)

        // Track Name: FF 03 len name
        writeVarLen(trackBytes, 0)
        trackBytes.write(0xFF)
        trackBytes.write(0x03)
        val nameBytes = sequence.name.toByteArray(Charsets.UTF_8)
        writeVarLen(trackBytes, nameBytes.size.toLong())
        trackBytes.write(nameBytes)

        // End of Track: Delta 0, FF 2F 00
        writeVarLen(trackBytes, 0)
        trackBytes.write(0xFF)
        trackBytes.write(0x2F)
        trackBytes.write(0x00)

        // Write Track Chunk Header: MTrk + length
        out.write("MTrk".toByteArray(Charsets.US_ASCII))
        writeInt32(out, trackBytes.size())
        trackBytes.writeTo(out)
    }

    private fun writeNoteTrack(track: MidiTrack, tpqn: Int, out: OutputStream) {
        val trackBytes = ByteArrayOutputStream()

        // Track Name
        writeVarLen(trackBytes, 0)
        trackBytes.write(0xFF)
        trackBytes.write(0x03)
        val nameBytes = track.name.toByteArray(Charsets.UTF_8)
        writeVarLen(trackBytes, nameBytes.size.toLong())
        trackBytes.write(nameBytes)

        // Program Change: Delta 0, C0 + channel, program
        val channel = track.notes.firstOrNull()?.channel ?: 0
        writeVarLen(trackBytes, 0)
        trackBytes.write(0xC0 or (channel and 0x0F))
        trackBytes.write(track.instrumentProgram and 0x7F)

        // Collect all NoteOn and NoteOff events sorted by tick
        data class RawEvent(val tick: Long, val isNoteOn: Boolean, val note: MidiNote)

        val events = mutableListOf<RawEvent>()
        for (note in track.notes) {
            events.add(RawEvent(note.startTick, true, note))
            events.add(RawEvent(note.endTick, false, note))
        }
        // NoteOff before NoteOn at identical tick
        events.sortWith(compareBy({ it.tick }, { if (it.isNoteOn) 1 else 0 }))

        var currentTick = 0L
        for (event in events) {
            val delta = event.tick - currentTick
            writeVarLen(trackBytes, delta.coerceAtLeast(0L))
            currentTick = event.tick

            val ch = event.note.channel and 0x0F
            if (event.isNoteOn) {
                trackBytes.write(0x90 or ch)
                trackBytes.write(event.note.pitch.coerceIn(0, 127))
                trackBytes.write(event.note.velocity.coerceIn(1, 127))
            } else {
                trackBytes.write(0x80 or ch)
                trackBytes.write(event.note.pitch.coerceIn(0, 127))
                trackBytes.write(0)
            }
        }

        // End of Track: Delta 0, FF 2F 00
        writeVarLen(trackBytes, 0)
        trackBytes.write(0xFF)
        trackBytes.write(0x2F)
        trackBytes.write(0x00)

        // Write Track Chunk Header: MTrk + length
        out.write("MTrk".toByteArray(Charsets.US_ASCII))
        writeInt32(out, trackBytes.size())
        trackBytes.writeTo(out)
    }

    private fun writeInt32(out: OutputStream, value: Int) {
        out.write((value shr 24) and 0xFF)
        out.write((value shr 16) and 0xFF)
        out.write((value shr 8) and 0xFF)
        out.write(value and 0xFF)
    }

    private fun writeInt16(out: OutputStream, value: Int) {
        out.write((value shr 8) and 0xFF)
        out.write(value and 0xFF)
    }

    private fun writeVarLen(out: OutputStream, value: Long) {
        var v = value
        val buffer = ByteArray(4)
        var i = 0
        buffer[i++] = (v and 0x7F).toByte()
        v = v shr 7
        while (v > 0) {
            buffer[i++] = ((v and 0x7F) or 0x80).toByte()
            v = v shr 7
        }
        while (i > 0) {
            out.write(buffer[--i].toInt())
        }
    }
}
