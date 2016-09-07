package test.pkg

import java.security.SecureRandom
import java.util.Random

class SecureRandomTest {
    protected val dynamicSeed: Int
        get() = 1

    fun testLiterals() {
        val random1 = SecureRandom()
        random1.setSeed(System.currentTimeMillis()) // Wrong
        random1.setSeed(System.nanoTime()) // Wrong
        random1.setSeed(dynamicSeed.toLong()) // OK
        random1.setSeed(0) // Wrong
        random1.setSeed(1) // Wrong
        random1.setSeed(1023.toInt().toLong()) // Wrong
        random1.setSeed(1023L) // Wrong
        random1.setSeed(FIXED_SEED) // Wrong
    }

    fun testFixedSeedBytesField() {
        val random2 = SecureRandom()
        random2.setSeed(fixedSeed) // Wrong
    }

    companion object {
        private val FIXED_SEED = 1000L

        fun testRandomTypeOk() {
            val random2 = Random()
            random2.setSeed(0) // OK
        }

        fun testRandomTypeWrong() {
            val random3 = SecureRandom()
            random3.setSeed(0) // Wrong: owner is java/util/Random, but applied to SecureRandom object
        }

        fun testBytesOk() {
            val random1 = SecureRandom()
            val seed = random1.generateSeed(4)
            random1.setSeed(seed) // OK
        }

        fun testBytesWrong() {
            val random2 = SecureRandom()
            val seed = ByteArray(3)
            random2.setSeed(seed) // Wrong
        }

        fun testFixedSeedBytes(something: Byte) {
            val random2 = SecureRandom()
            val seedBytes = byteArrayOf(1, 2, 3)
            random2.setSeed(seedBytes) // Wrong
            val seedBytes2 = byteArrayOf(1, something, 3)
            random2.setSeed(seedBytes2) // OK
        }

        private val fixedSeed = byteArrayOf(1, 2, 3)
    }

}
