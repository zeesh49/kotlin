import android.content.Context
import android.widget.Button
import android.widget.TextView

internal class CustomScreen(context: Context) {

    init {
        val view = TextView(context)

        // Should fail - hardcoded string
        view.text = "Hardcoded"
        // Should pass - no letters
        view.text = "-"
        // Should fail - concatenation and toString for numbers.
        view.text = Integer.toString(50) + "%"
        view.text = java.lang.Double.toString(12.5) + " miles"

        val btn = Button(context)
        btn.text = "User " + userName
        btn.text = String.format("%s of %s users", Integer.toString(5), Integer.toString(10))
    }

    private val userName: String
        get() = "stub"
}
