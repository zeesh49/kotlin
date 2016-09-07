import android.os.Parcel
import android.os.Parcelable

class ParcelableDemo {
    interface MoreThanParcelable : Parcelable {
        fun somethingMore()
    }

    abstract class AbstractParcelable : Parcelable

    //ERROR
    open class JustParcelable : Parcelable {
        override fun describeContents() = 0
        override fun writeToParcel(dest: Parcel, flags: Int) {}
    }

    //ERROR
    class JustParcelableSubclass : JustParcelable()

    //ERROR
    class ParcelableThroughAbstractSuper : AbstractParcelable() {
        override fun describeContents() = 0
        override fun writeToParcel(dest: Parcel, flags: Int) {}
    }

    //ERROR
    class ParcelableThroughInterface : MoreThanParcelable {
        override fun describeContents() = 0
        override fun writeToParcel(dest: Parcel, flags: Int) {}
        override fun somethingMore() {}
    }
}

//OK
class MyParcelable2 : Parcelable {
    override fun describeContents() = 0
    override fun writeToParcel(arg0: Parcel, arg1: Int) {}

    companion object {
        val CREATOR: Parcelable.Creator<MyParcelable2> = object : Parcelable.Creator<MyParcelable2> {
            override fun createFromParcel(`in`: Parcel) = MyParcelable2()
            override fun newArray(size: Int): Array<MyParcelable2?> = arrayOfNulls(size)
        }
    }
}

//ERROR, but is not checked right now in AS
class MyParcelable3 : Parcelable {
    override fun describeContents() = 0
    override fun writeToParcel(arg0: Parcel, arg1: Int) {}

    companion object {
        val CREATOR = 0
    }
}

//OK
abstract class MyParcelable4 : Parcelable {
    override fun describeContents() = 0
    override fun writeToParcel(arg0: Parcel, arg1: Int) {}
}

//OK
interface MyParcelable5 : Parcelable {
    override fun describeContents(): Int
}
