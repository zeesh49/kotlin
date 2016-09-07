import android.content.Context
import android.telephony.SmsManager

class NonInternationalizedSmsDetectorTest {
    private fun sendLocalizedMessage(context: Context) {
        // Don't warn here
        val sms = SmsManager.getDefault()
        sms.sendTextMessage("+1234567890", null, null, null, null)
    }

    private fun sendAlternativeCountryPrefix(context: Context) {
        // Do warn here
        val sms = SmsManager.getDefault()
        sms.sendMultipartTextMessage("001234567890", null, null, null, null)
    }
}
