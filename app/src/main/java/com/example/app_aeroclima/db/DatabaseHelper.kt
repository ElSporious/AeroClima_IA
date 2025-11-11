import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.mindrot.jbcrypt.BCrypt

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "UserManager.db"
        private const val TABLE_USERS = "users"
        private const val KEY_ID = "id"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTableQuery = ("CREATE TABLE " + TABLE_USERS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_USERNAME + " TEXT UNIQUE,"
                + KEY_PASSWORD + " TEXT" + ")")
        db?.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    fun addUser(username: String, plainPassword: String): Boolean {
        val db = this.writableDatabase
        val hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt())
        val values = ContentValues()
        values.put(KEY_USERNAME, username)
        values.put(KEY_PASSWORD, hashedPassword)

        val success = db.insert(TABLE_USERS, null, values)
        db.close()
        return (Integer.parseInt("$success") != -1)
    }

    fun checkUser(username: String, plainPasswordToCheck: String): Boolean {
        val db = this.readableDatabase
        var userExists = false

        val cursor = db.query(
            TABLE_USERS,
            arrayOf(KEY_ID, KEY_PASSWORD),
            "$KEY_USERNAME = ?",
            arrayOf(username),
            null, null, null
        )

        if (cursor.moveToFirst()) {
            val passwordColumnIndex = cursor.getColumnIndex(KEY_PASSWORD)

            if (passwordColumnIndex >= 0) {
                val hashedPasswordFromDB = cursor.getString(passwordColumnIndex)
                if (BCrypt.checkpw(plainPasswordToCheck, hashedPasswordFromDB)) {
                    userExists = true
                }
            }
        }

        cursor.close()
        db.close()
        return userExists
    }
}