package test.pkg

import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSession

abstract class InsecureHostnameVerifier {
    fun allowAll(): HostnameVerifier = object : HostnameVerifier {
        override fun verify(hostname: String, session: SSLSession): Boolean {
            return true
        }
    }

    val allowAllProperty: HostnameVerifier = object : HostnameVerifier {
        override fun verify(hostname: String, session: SSLSession): Boolean {
            return true
        }
    }

    val allowAllLambda: HostnameVerifier = HostnameVerifier { hostname, session -> true }

    var allowAll2: HostnameVerifier = object : HostnameVerifier {
        override fun verify(hostname: String, session: SSLSession): Boolean {
            val returnValue = true
            if (true) {
                val irrelevant = 5
                if (irrelevant > 6) {
                    return returnValue
                }
            }
            return returnValue
        }
    }

    var unknown: HostnameVerifier = object : HostnameVerifier {
        override fun verify(hostname: String, session: SSLSession): Boolean {
            var returnValue = true
            if (hostname.contains("something")) {
                returnValue = false
            }
            return returnValue
        }
    }
}

class ExampleHostnameVerifier : IntentService("ExampleHostnameVerifier") {
    internal var denyAll: HostnameVerifier = object : HostnameVerifier {
        override fun verify(hostname: String, session: SSLSession): Boolean {
            return false
        }
    }

    override fun onHandleIntent(p0: Intent?) {
        val url = URL("https://www.google.com")
        val connection = url.openConnection() as HttpsURLConnection
        connection.hostnameVerifier = denyAll
    }
}