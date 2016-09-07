import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
class OverrideConcreteTest {
    // OK: This one specifies both methods
    open class MyNotificationListenerService1 : NotificationListenerService() {
        override fun onNotificationPosted(statusBarNotification: StatusBarNotification) {
        }

        override fun onNotificationRemoved(statusBarNotification: StatusBarNotification) {
        }
    }

    // Error: Misses onNotificationPosted
    class MyNotificationListenerService2 : NotificationListenerService() {
        override fun onNotificationRemoved(statusBarNotification: StatusBarNotification) {
        }
    }

    // Error: Misses onNotificationRemoved
    open class MyNotificationListenerService3 : NotificationListenerService() {
        override fun onNotificationPosted(statusBarNotification: StatusBarNotification) {
        }
    }

    // Error: Missing both; wrong signatures (first has wrong arg count, second has wrong type)
    class MyNotificationListenerService4 : NotificationListenerService() {
        fun onNotificationPosted(statusBarNotification: StatusBarNotification, flags: Int) {
        }

        fun onNotificationRemoved(statusBarNotification: Int) {
        }
    }

    // OK: Inherits from a class which define both
    class MyNotificationListenerService5 : MyNotificationListenerService1()

    // OK: Inherits from a class which defines only one, but the other one is defined here
    class MyNotificationListenerService6 : MyNotificationListenerService3() {
        override fun onNotificationRemoved(statusBarNotification: StatusBarNotification) {
        }
    }

    // Error: Inheriting from a class which only defines one
    class MyNotificationListenerService7 : MyNotificationListenerService3()

    // OK: Has target api setting a local version that is high enough
    @TargetApi(21)
    class MyNotificationListenerService8 : NotificationListenerService()

    // OK: Suppressed
    @SuppressLint("OverrideAbstract")
    class MyNotificationListenerService9 : MyNotificationListenerService1()
}