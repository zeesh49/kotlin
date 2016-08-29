package test.pkg

import javax.crypto.Cipher

class CipherGetInstanceAES {
    @Throws(Exception::class)
    private fun foo() {
        Cipher.getInstance("AES")
    }
}

class CipherGetInstanceAESCBC {
    @Throws(Exception::class)
    private fun foo() {
        Cipher.getInstance("AES/CBC/NoPadding")
    }
}

class CipherGetInstanceAESECB {
    @Throws(Exception::class)
    private fun foo() {
        Cipher.getInstance("AES/ECB/NoPadding")
    }
}

class CipherGetInstanceDES {
    @Throws(Exception::class)
    private fun foo() {
        Cipher.getInstance("DES")
    }
}

class CipherGetInstanceTest {
    fun test() {
        val des = Cipher.getInstance(Constants.DES)
    }

    object Constants {
        val DES = "DES/ECB/NoPadding"
    }
}
