package test.pkg

import android.app.AlarmManager

class AlarmTest {
    fun test(alarmManager: AlarmManager) {
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, 5000, 60000, null) // OK
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, 6000, 70000, null) // OK
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, 50, 10, null) // ERROR
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, 5000, // ERROR
                                  OtherClass.MY_INTERVAL, null)                          // ERROR

        // Check value flow analysis
        val interval = 10
        val interval2 = (2 * interval).toLong()
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, 5000, interval2, null) // ERROR
    }

    private object OtherClass {
        val MY_INTERVAL = 1000L
    }
}
