package io.github.pprakapienka.vinylscrobbler.recognition.chromaprint

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Power-of-two real FFT whose magnitudes match KissFFT's floating-point path.
 * Chromaprint's default build feeds those magnitudes into the chroma extractor.
 * Algorithm by Mark Borgerding (BSD-3-Clause); this is a translation of the
 * radix-2/4 forward transform only.
 */
internal class KissRealFft(private val realSize: Int) {
    private val ncfft = realSize / 2
    private val complex = KissComplexFft(ncfft)
    private val superTwiddles = Array(ncfft / 2) { index ->
        val phase = -PI * ((index + 1).toDouble() / ncfft + 0.5)
        Cx(cos(phase).toFloat(), sin(phase).toFloat())
    }

    init {
        require(realSize > 0 && realSize % 2 == 0) { "Real FFT size must be even" }
    }

    fun magnitudeSquared(time: FloatArray): DoubleArray {
        require(time.size == realSize)
        val packed = Array(ncfft) { index -> Cx(time[index * 2], time[index * 2 + 1]) }
        val spectrum = Array(ncfft) { Cx() }
        complex.transform(packed, spectrum)

        val real = FloatArray(ncfft + 1)
        val imag = FloatArray(ncfft + 1)
        val dcReal = spectrum[0].r
        val dcImag = spectrum[0].i
        real[0] = dcReal + dcImag
        real[ncfft] = dcReal - dcImag

        for (k in 1..ncfft / 2) {
            val evenReal = spectrum[k].r
            val evenImag = spectrum[k].i
            val oddReal = spectrum[ncfft - k].r
            val oddImag = -spectrum[ncfft - k].i
            val sumReal = evenReal + oddReal
            val sumImag = evenImag + oddImag
            val diffReal = evenReal - oddReal
            val diffImag = evenImag - oddImag
            val twiddle = superTwiddles[k - 1]
            val mixedReal = diffReal * twiddle.r - diffImag * twiddle.i
            val mixedImag = diffReal * twiddle.i + diffImag * twiddle.r
            real[k] = 0.5f * (sumReal + mixedReal)
            imag[k] = 0.5f * (sumImag + mixedImag)
            real[ncfft - k] = 0.5f * (sumReal - mixedReal)
            imag[ncfft - k] = 0.5f * (mixedImag - sumImag)
        }

        return DoubleArray(ncfft + 1) { index ->
            val r = real[index].toDouble()
            val i = imag[index].toDouble()
            r * r + i * i
        }
    }
}

private class KissComplexFft(private val nfft: Int) {
    private val twiddles = Array(nfft) { index ->
        val phase = -2.0 * PI * index / nfft
        Cx(cos(phase).toFloat(), sin(phase).toFloat())
    }
    private val factors = factorize(nfft)

    fun transform(input: Array<Cx>, output: Array<Cx>) {
        work(output, 0, input, 0, 1, 1, 0)
    }

    private fun work(
        fout: Array<Cx>,
        foutOffset: Int,
        fin: Array<Cx>,
        finOffset: Int,
        fstride: Int,
        inStride: Int,
        factorPos: Int,
    ) {
        val radix = factors[factorPos]
        val stage = factors[factorPos + 1]
        val foutEnd = foutOffset + radix * stage
        if (stage == 1) {
            var outIndex = foutOffset
            var inIndex = finOffset
            while (outIndex != foutEnd) {
                fout[outIndex].setFrom(fin[inIndex])
                inIndex += fstride * inStride
                outIndex++
            }
        } else {
            var outIndex = foutOffset
            var inIndex = finOffset
            while (true) {
                work(fout, outIndex, fin, inIndex, fstride * radix, inStride, factorPos + 2)
                inIndex += fstride * inStride
                outIndex += stage
                if (outIndex == foutEnd) break
            }
        }
        when (radix) {
            2 -> butterfly2(fout, foutOffset, fstride, stage)
            4 -> butterfly4(fout, foutOffset, fstride, stage)
            else -> error("KissFFT radix $radix is not used by a power-of-two Chromaprint frame")
        }
    }

    private fun butterfly2(fout: Array<Cx>, start: Int, fstride: Int, count: Int) {
        var left = start
        var right = start + count
        var twiddle = 0
        repeat(count) {
            val product = Cx()
            product.multiply(fout[right], twiddles[twiddle])
            twiddle += fstride
            val leftReal = fout[left].r
            val leftImag = fout[left].i
            fout[right].r = leftReal - product.r
            fout[right].i = leftImag - product.i
            fout[left].r = leftReal + product.r
            fout[left].i = leftImag + product.i
            left++
            right++
        }
    }

    private fun butterfly4(fout: Array<Cx>, start: Int, fstride: Int, stage: Int) {
        val scratch = Array(6) { Cx() }
        var index = start
        var twiddle1 = 0
        var twiddle2 = 0
        var twiddle3 = 0
        repeat(stage) {
            scratch[0].multiply(fout[index + stage], twiddles[twiddle1])
            scratch[1].multiply(fout[index + 2 * stage], twiddles[twiddle2])
            scratch[2].multiply(fout[index + 3 * stage], twiddles[twiddle3])
            scratch[5].r = fout[index].r - scratch[1].r
            scratch[5].i = fout[index].i - scratch[1].i
            fout[index].r += scratch[1].r
            fout[index].i += scratch[1].i
            scratch[3].r = scratch[0].r + scratch[2].r
            scratch[3].i = scratch[0].i + scratch[2].i
            scratch[4].r = scratch[0].r - scratch[2].r
            scratch[4].i = scratch[0].i - scratch[2].i
            fout[index + 2 * stage].r = fout[index].r - scratch[3].r
            fout[index + 2 * stage].i = fout[index].i - scratch[3].i
            twiddle1 += fstride
            twiddle2 += fstride * 2
            twiddle3 += fstride * 3
            fout[index].r += scratch[3].r
            fout[index].i += scratch[3].i
            fout[index + stage].r = scratch[5].r + scratch[4].i
            fout[index + stage].i = scratch[5].i - scratch[4].r
            fout[index + 3 * stage].r = scratch[5].r - scratch[4].i
            fout[index + 3 * stage].i = scratch[5].i + scratch[4].r
            index++
        }
    }

    private fun factorize(size: Int): IntArray {
        val factors = IntArray(64)
        var n = size
        var p = 4
        var cursor = 0
        val root = kotlin.math.sqrt(n.toDouble()).toInt()
        while (n > 1) {
            while (n % p != 0) {
                p = when (p) {
                    4 -> 2
                    2 -> 3
                    else -> p + 2
                }
                if (p > root) p = n
            }
            n /= p
            factors[cursor++] = p
            factors[cursor++] = n
        }
        return factors.copyOf(cursor)
    }
}

private class Cx(var r: Float = 0f, var i: Float = 0f) {
    fun setFrom(other: Cx) {
        r = other.r
        i = other.i
    }

    fun multiply(left: Cx, right: Cx) {
        r = left.r * right.r - left.i * right.i
        i = left.r * right.i + left.i * right.r
    }
}
