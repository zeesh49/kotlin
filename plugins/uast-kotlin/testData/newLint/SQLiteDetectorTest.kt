import android.database.sqlite.SQLiteDatabase
import android.provider.BaseColumns

interface TracksColumns : BaseColumns {
    companion object {

        val TABLE_NAME = "tracks"

        val NAME = "name"
        val CATEGORY = "category"
        val STARTTIME = "starttime"
        val MAXGRADE = "maxgrade"
        val MAPID = "mapid"
        val TABLEID = "tableid"
        val ICON = "icon"

        val CREATE_TABLE = "CREATE TABLE $TABLE_NAME (" +
                           BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                           NAME + " STRING, " +
                           CATEGORY + " STRING, " +
                           STARTTIME + " INTEGER, " +
                           MAXGRADE + " FLOAT, " +
                           MAPID + " STRING, " +
                           TABLEID + " STRING, " +
                           ICON + " STRING" +
                           ");"
    }
}

@SuppressWarnings("unused", "SpellCheckingInspection")
class SQLiteTest {
    interface Tables {
        interface AppKeys {

            interface Columns {
                companion object {
                    val _ID = "_id"
                    val PKG_NAME = "packageName"
                    val PKG_SIG = "signatureDigest"
                }
            }

            companion object {
                val NAME = "appkeys"

                val SCHEMA =
                        Columns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                        Columns.PKG_NAME + " STRING NOT NULL," +
                        Columns.PKG_SIG + " STRING NOT NULL"
            }
        }
    }

    fun test(db: SQLiteDatabase, name: String) {
        db.execSQL("CREATE TABLE " + name + "(" + Tables.AppKeys.SCHEMA + ");") // ERROR

    }

    fun onCreate(db: SQLiteDatabase) {
        db.execSQL(TracksColumns.CREATE_TABLE) // ERROR
    }

    private fun doCreate(db: SQLiteDatabase) {
        // Not yet handled; we need to flow string concatenation across procedure calls
        createTable(db, Tables.AppKeys.NAME, Tables.AppKeys.SCHEMA) // ERROR
    }

    private fun createTable(db: SQLiteDatabase, tableName: String, schema: String) {
        db.execSQL("CREATE TABLE $tableName($schema);")
    }
}