import android.os.Parcel
import android.os.Parcelable

class ParcelableDemo {
    private fun testParcelable(`in`: Parcel) {
        val error1 = `in`.readParcelable<Parcelable>(null)
        val error2 = `in`.readParcelableArray(null)
        val error3 = `in`.readBundle(null)
        val error4 = `in`.readArray(null)
        val error5 = `in`.readSparseArray(null)
        val error6 = `in`.readValue(null)
        val error7 = `in`.readPersistableBundle(null)
        val error8 = `in`.readBundle()
        val error9 = `in`.readPersistableBundle()

        val ok = `in`.readParcelable<Parcelable>(javaClass.classLoader)
    }
}