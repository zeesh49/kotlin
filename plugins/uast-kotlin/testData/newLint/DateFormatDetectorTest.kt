package test.pkg

import java.text.*
import java.util.*
import java.lang.String

class LocaleTest {
    fun testStrings() {
        String.format(Locale.getDefault(), "OK: %f", 1.0f)
        String.format("OK: %x %A %c %b %B %h %n %%", 1, 2, 'c', true, false, 5)
        String.format("WRONG: %f", 1.0f) // Implies locale
        String.format("WRONG: %1\$f", 1.0f)
        String.format("WRONG: %e", 1.0f)
        String.format("WRONG: %d", 1.0f)
        String.format("WRONG: %g", 1.0f)
        String.format("WRONG: %g", 1.0f)
        String.format("WRONG: %1\$tm %1\$te,%1\$tY",
                      GregorianCalendar(2012, GregorianCalendar.AUGUST, 27))
    }

    @android.annotation.SuppressLint("NewApi") // DateFormatSymbols requires API 9
    fun testSimpleDateFormat() {
        SimpleDateFormat() // WRONG
        SimpleDateFormat("yyyy-MM-dd") // WRONG
        SimpleDateFormat("yyyy-MM-dd", DateFormatSymbols.getInstance()) // WRONG
        SimpleDateFormat("yyyy-MM-dd", Locale.US) // OK
    }
}
