package com.zdmgold.autoclickernega.data.db

import android.content.Context
import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

@Database(entities = [ScriptEntity::class], version = 1, exportSchema = true)
abstract class NegaDatabase : RoomDatabase() {
    abstract fun scripts(): ScriptDao

    companion object {
        const val FILE_NAME = "nega.db"
        private const val TAG = "NegaDatabase"

        /**
         * Opens the database. A file that cannot be opened is renamed aside, never deleted, and a fresh
         * one is created. [Opened.recovered] tells the caller so the user can be told.
         */
        fun open(context: Context): Opened {
            val app = context.applicationContext
            return try {
                Opened(build(app).also { it.openHelper.writableDatabase }, recovered = false)
            } catch (e: SQLiteException) {
                Log.w(TAG, "Database could not be opened and was moved aside: ${e.message}")
                moveAside(app.getDatabasePath(FILE_NAME))
                Opened(build(app), recovered = true)
            }
        }

        private fun build(context: Context): NegaDatabase {
            val builder = Room.databaseBuilder(context, NegaDatabase::class.java, FILE_NAME)
            Migrations.ALL.forEach { builder.addMigrations(it) }
            return builder.build()
        }

        private fun moveAside(file: File) {
            val stamp = System.currentTimeMillis()
            if (file.exists()) file.renameTo(File(file.parentFile, "$FILE_NAME.corrupt-$stamp"))
            File(file.path + "-wal").delete()
            File(file.path + "-shm").delete()
            File(file.path + "-journal").delete()
        }
    }
}

class Opened(
    val database: NegaDatabase,
    val recovered: Boolean,
)
