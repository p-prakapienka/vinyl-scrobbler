package io.github.pprakapienka.vinylscrobbler.recognition.chromaprint

import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.round
import kotlin.math.sqrt

/**
 * Chromaprint algorithm 2 (the AcoustID default): 11025 Hz, 4096-sample frames,
 * no interpolation, no silence trimming.
 */
internal object ChromaprintEngine {
    const val ALGORITHM = 1
    const val SAMPLE_RATE = 11025
    const val FRAME_SIZE = 4096
    const val HOP = FRAME_SIZE / 3
    private const val BANDS = 12
    private const val MIN_FREQ = 28
    private const val MAX_FREQ = 3520
    private const val NORMAL_BITS = 3
    private const val EXCEPTIONAL_BITS = 5
    private const val NORMAL_ESCAPE = 7
    private val FILTER = doubleArrayOf(0.25, 0.75, 1.0, 0.75, 0.25)
    private val GRAY = intArrayOf(0, 1, 3, 2)

    private val classifiers = arrayOf(
        Classifier(0, 4, 3, 15, 1.98215, 2.35817, 2.63523),
        Classifier(4, 4, 6, 15, -1.03809, -0.651211, -0.282167),
        Classifier(1, 0, 4, 16, -0.298702, 0.119262, 0.558497),
        Classifier(3, 8, 2, 12, -0.105439, 0.0153946, 0.135898),
        Classifier(3, 4, 4, 8, -0.142891, 0.0258736, 0.200632),
        Classifier(4, 0, 3, 5, -0.826319, -0.590612, -0.368214),
        Classifier(1, 2, 2, 9, -0.557409, -0.233035, 0.0534525),
        Classifier(2, 7, 3, 4, -0.0646826, 0.00620476, 0.0784847),
        Classifier(2, 6, 2, 16, -0.192387, -0.029699, 0.215855),
        Classifier(2, 1, 3, 2, -0.0397818, -0.00568076, 0.0292026),
        Classifier(5, 10, 1, 15, -0.53823, -0.369934, -0.190235),
        Classifier(3, 6, 2, 10, -0.124877, 0.0296483, 0.139239),
        Classifier(2, 1, 1, 14, -0.101475, 0.0225617, 0.231971),
        Classifier(3, 5, 6, 4, -0.0799915, -0.00729616, 0.063262),
        Classifier(1, 9, 2, 12, -0.272556, 0.019424, 0.302559),
        Classifier(3, 4, 2, 14, -0.164292, -0.0321188, 0.0846339),
    )

    private val maxFilterWidth = classifiers.maxOf { it.width }

    fun rawFingerprint(mono11025: ShortArray): IntArray {
        val fft = KissRealFft(FRAME_SIZE)
        val window = hammingWindow(FRAME_SIZE, 1.0 / 32767.0)
        val notes = noteTable()
        val integral = RollingIntegralImage(maxRows = 256, columns = BANDS)
        val chromaBuffer = Array(8) { DoubleArray(BANDS) }
        var chromaOffset = 0
        var chromaFilled = 1
        val fingerprint = ArrayList<Int>()
        val frame = FloatArray(FRAME_SIZE)
        var position = 0
        while (position + FRAME_SIZE <= mono11025.size) {
            for (i in 0 until FRAME_SIZE) {
                frame[i] = mono11025[position + i] * window[i]
            }
            val magnitudes = fft.magnitudeSquared(frame)
            val features = chroma(magnitudes, notes)
            features.copyInto(chromaBuffer[chromaOffset])
            chromaOffset = (chromaOffset + 1) % chromaBuffer.size
            if (chromaFilled >= FILTER.size) {
                val filtered = DoubleArray(BANDS)
                val start = (chromaOffset + chromaBuffer.size - FILTER.size) % chromaBuffer.size
                for (band in 0 until BANDS) {
                    var sum = 0.0
                    for (tap in FILTER.indices) {
                        sum += chromaBuffer[(start + tap) % chromaBuffer.size][band] * FILTER[tap]
                    }
                    filtered[band] = sum
                }
                normalize(filtered)
                integral.addRow(filtered)
                if (integral.rows >= maxFilterWidth) {
                    fingerprint += subfingerprint(integral, integral.rows - maxFilterWidth)
                }
            } else {
                chromaFilled++
            }
            position += HOP
        }
        return fingerprint.toIntArray()
    }

    fun encode(raw: IntArray, algorithm: Int = ALGORITHM): String {
        val normal = ArrayList<Int>()
        val exceptional = ArrayList<Int>()
        if (raw.isNotEmpty()) {
            packSubfingerprint(raw[0].toUInt(), normal, exceptional)
            for (index in 1 until raw.size) {
                packSubfingerprint(raw[index].toUInt() xor raw[index - 1].toUInt(), normal, exceptional)
            }
        }
        val bytes = ByteArray(4 + packedSize(normal.size, 3) + packedSize(exceptional.size, 5))
        bytes[0] = (algorithm and 255).toByte()
        bytes[1] = ((raw.size shr 16) and 255).toByte()
        bytes[2] = ((raw.size shr 8) and 255).toByte()
        bytes[3] = (raw.size and 255).toByte()
        var cursor = 4
        cursor = packBits(normal, 3, bytes, cursor)
        packBits(exceptional, 5, bytes, cursor)
        return base64(bytes)
    }

    fun decode(encoded: String): IntArray {
        val bytes = decodeBase64(encoded)
        require(bytes.size >= 4) { "Truncated fingerprint" }
        val size = ((bytes[1].toInt() and 255) shl 16) or
            ((bytes[2].toInt() and 255) shl 8) or
            (bytes[3].toInt() and 255)
        if (size == 0) return IntArray(0)
        require(bytes.size > 4) { "Truncated fingerprint" }
        val packedNormals = unpackBits(bytes, 4, (bytes.size - 4) * 8 / NORMAL_BITS, NORMAL_BITS)
        var terminators = 0
        var exceptionalCount = 0
        var used = 0
        for (index in packedNormals.indices) {
            used = index + 1
            when (packedNormals[index]) {
                0 -> terminators++
                NORMAL_ESCAPE -> exceptionalCount++
            }
            if (terminators == size) break
        }
        require(terminators == size) { "Truncated fingerprint" }
        val exceptionalOffset = 4 + packedSize(used, NORMAL_BITS)
        val exceptional = if (exceptionalCount == 0) {
            IntArray(0)
        } else {
            require(bytes.size >= exceptionalOffset + packedSize(exceptionalCount, EXCEPTIONAL_BITS)) {
                "Truncated fingerprint"
            }
            unpackBits(bytes, exceptionalOffset, exceptionalCount, EXCEPTIONAL_BITS)
        }
        var exceptionalIndex = 0
        val raw = IntArray(size)
        var value = 0
        var lastBit = 0
        var rawIndex = 0
        for (index in 0 until used) {
            val gap = if (packedNormals[index] == NORMAL_ESCAPE) {
                packedNormals[index] + exceptional[exceptionalIndex++]
            } else {
                packedNormals[index]
            }
            if (gap == 0) {
                raw[rawIndex++] = value
                lastBit = 0
            } else {
                lastBit += gap
                require(lastBit in 1..32) { "Invalid fingerprint" }
                value = value xor (1 shl (lastBit - 1))
            }
        }
        require(rawIndex == size) { "Invalid fingerprint" }
        return raw
    }

    private fun subfingerprint(image: RollingIntegralImage, offset: Int): Int {
        var bits = 0
        for (classifier in classifiers) {
            val value = classifier.apply(image, offset)
            bits = (bits shl 2) or GRAY[classifier.quantize(value)]
        }
        return bits
    }

    private fun chroma(magnitudes: DoubleArray, notes: NoteTable): DoubleArray {
        val features = DoubleArray(BANDS)
        for (bin in notes.minIndex until notes.maxIndex) {
            features[notes.note[bin]] += magnitudes[bin]
        }
        return features
    }

    private fun noteTable(): NoteTable {
        val note = IntArray(FRAME_SIZE)
        val minIndex = maxOf(1, freqToIndex(MIN_FREQ.toDouble()))
        val maxIndex = minOf(FRAME_SIZE / 2, freqToIndex(MAX_FREQ.toDouble()))
        for (bin in minIndex until maxIndex) {
            val freq = bin.toDouble() * SAMPLE_RATE / FRAME_SIZE
            val octave = ln(freq / (440.0 / 16.0)) / ln(2.0)
            note[bin] = (BANDS * (octave - floor(octave))).toInt()
        }
        return NoteTable(note, minIndex, maxIndex)
    }

    private fun freqToIndex(freq: Double): Int = round(FRAME_SIZE * freq / SAMPLE_RATE).toInt()

    private fun normalize(features: DoubleArray) {
        var squares = 0.0
        for (value in features) squares += value * value
        val norm = if (squares > 0.0) sqrt(squares) else 0.0
        if (norm < 0.01) {
            features.fill(0.0)
        } else {
            for (index in features.indices) features[index] /= norm
        }
    }

    private fun hammingWindow(size: Int, scale: Double): FloatArray {
        val window = FloatArray(size)
        for (index in 0 until size) {
            window[index] = (scale * (0.54 - 0.46 * cos(index * 2.0 * kotlin.math.PI / (size - 1)))).toFloat()
        }
        return window
    }

    private fun packSubfingerprint(value: UInt, normal: MutableList<Int>, exceptional: MutableList<Int>) {
        var bits = value
        var bit = 1
        var lastBit = 0
        while (bits != 0u) {
            if ((bits and 1u) != 0u) {
                val gap = bit - lastBit
                if (gap >= 7) {
                    normal += 7
                    exceptional += gap - 7
                } else {
                    normal += gap
                }
                lastBit = bit
            }
            bits = bits shr 1
            bit++
        }
        normal += 0
    }

    private fun packedSize(count: Int, bits: Int): Int = (count * bits + 7) / 8

    private fun packBits(values: List<Int>, bits: Int, dest: ByteArray, start: Int): Int {
        var cursor = start
        var accumulator = 0
        var filled = 0
        for (value in values) {
            accumulator = accumulator or ((value and ((1 shl bits) - 1)) shl filled)
            filled += bits
            while (filled >= 8) {
                dest[cursor++] = (accumulator and 255).toByte()
                accumulator = accumulator ushr 8
                filled -= 8
            }
        }
        if (filled > 0) {
            dest[cursor++] = (accumulator and 255).toByte()
        }
        return cursor
    }

    private fun unpackBits(bytes: ByteArray, offset: Int, count: Int, bits: Int): IntArray {
        if (count == 0) return IntArray(0)
        val out = IntArray(count)
        var cursor = offset
        var accumulator = 0
        var filled = 0
        val mask = (1 shl bits) - 1
        for (index in 0 until count) {
            while (filled < bits) {
                require(cursor < bytes.size) { "Truncated fingerprint" }
                accumulator = accumulator or ((bytes[cursor].toInt() and 255) shl filled)
                cursor++
                filled += 8
            }
            out[index] = accumulator and mask
            accumulator = accumulator ushr bits
            filled -= bits
        }
        return out
    }

    private fun decodeBase64(text: String): ByteArray {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
        val values = IntArray(128) { -1 }
        alphabet.forEachIndexed { index, symbol -> values[symbol.code] = index }
        val out = ArrayList<Byte>(text.length * 3 / 4)
        var accumulator = 0
        var filled = 0
        for (symbol in text) {
            require(symbol.code < 128) { "Invalid fingerprint" }
            val value = values[symbol.code]
            require(value >= 0) { "Invalid fingerprint" }
            accumulator = (accumulator shl 6) or value
            filled += 6
            if (filled >= 8) {
                filled -= 8
                out += ((accumulator shr filled) and 255).toByte()
            }
        }
        return out.toByteArray()
    }

    private fun base64(bytes: ByteArray): String {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
        val out = StringBuilder((bytes.size * 4 + 2) / 3)
        var index = 0
        while (index + 3 <= bytes.size) {
            val b0 = bytes[index].toInt() and 255
            val b1 = bytes[index + 1].toInt() and 255
            val b2 = bytes[index + 2].toInt() and 255
            out.append(alphabet[(b0 shr 2) and 63])
            out.append(alphabet[((b0 shl 4) or (b1 shr 4)) and 63])
            out.append(alphabet[((b1 shl 2) or (b2 shr 6)) and 63])
            out.append(alphabet[b2 and 63])
            index += 3
        }
        val remaining = bytes.size - index
        if (remaining == 2) {
            val b0 = bytes[index].toInt() and 255
            val b1 = bytes[index + 1].toInt() and 255
            out.append(alphabet[(b0 shr 2) and 63])
            out.append(alphabet[((b0 shl 4) or (b1 shr 4)) and 63])
            out.append(alphabet[(b1 shl 2) and 63])
        } else if (remaining == 1) {
            val b0 = bytes[index].toInt() and 255
            out.append(alphabet[(b0 shr 2) and 63])
            out.append(alphabet[(b0 shl 4) and 63])
        }
        return out.toString()
    }
}

private data class NoteTable(val note: IntArray, val minIndex: Int, val maxIndex: Int)

private class Classifier(
    val type: Int,
    val y: Int,
    val height: Int,
    val width: Int,
    val t0: Double,
    val t1: Double,
    val t2: Double,
) {
    fun quantize(value: Double): Int = if (value < t1) {
        if (value < t0) 0 else 1
    } else {
        if (value < t2) 2 else 3
    }

    fun apply(image: RollingIntegralImage, x: Int): Double {
        val w = width
        val h = height
        val compared = when (type) {
            0 -> image.area(x, y, x + w, y + h) to 0.0
            1 -> {
                val half = h / 2
                image.area(x, y + half, x + w, y + h) to image.area(x, y, x + w, y + half)
            }
            2 -> {
                val half = w / 2
                image.area(x + half, y, x + w, y + h) to image.area(x, y, x + half, y + h)
            }
            3 -> {
                val halfW = w / 2
                val halfH = h / 2
                val a = image.area(x, y + halfH, x + halfW, y + h) + image.area(x + halfW, y, x + w, y + halfH)
                val b = image.area(x, y, x + halfW, y + halfH) + image.area(x + halfW, y + halfH, x + w, y + h)
                a to b
            }
            4 -> {
                val third = h / 3
                image.area(x, y + third, x + w, y + 2 * third) to
                    (image.area(x, y, x + w, y + third) + image.area(x, y + 2 * third, x + w, y + h))
            }
            5 -> {
                val third = w / 3
                image.area(x + third, y, x + 2 * third, y + h) to
                    (image.area(x, y, x + third, y + h) + image.area(x + 2 * third, y, x + w, y + h))
            }
            else -> 0.0 to 0.0
        }
        return ln((1.0 + compared.first) / (1.0 + compared.second))
    }
}

private class RollingIntegralImage(maxRows: Int, private val columns: Int) {
    private val capacity = maxRows + 1
    private val data = Array(capacity) { DoubleArray(columns) }
    var rows: Int = 0
        private set

    fun addRow(values: DoubleArray) {
        val row = data[rows % capacity]
        var running = 0.0
        for (column in 0 until columns) {
            running += values[column]
            row[column] = running
        }
        if (rows > 0) {
            val previous = data[(rows - 1) % capacity]
            for (column in 0 until columns) row[column] += previous[column]
        }
        rows++
    }

    fun area(r1: Int, c1: Int, r2: Int, c2: Int): Double {
        if (r1 == r2 || c1 == c2) return 0.0
        val row2 = data[(r2 - 1) % capacity]
        if (r1 == 0) {
            return if (c1 == 0) row2[c2 - 1] else row2[c2 - 1] - row2[c1 - 1]
        }
        val row1 = data[(r1 - 1) % capacity]
        return if (c1 == 0) {
            row2[c2 - 1] - row1[c2 - 1]
        } else {
            row2[c2 - 1] - row1[c2 - 1] - row2[c1 - 1] + row1[c1 - 1]
        }
    }
}
