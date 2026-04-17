package com.example.wordsnotify

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class WordBookDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) {
        createWordBookTable(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            migrateToSnakeCaseColumns(db)
        }
    }

    private fun createWordBookTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_WORD_BOOK (
                $COLUMN_WORD_RANK INTEGER PRIMARY KEY,
                $COLUMN_HEAD_WORD TEXT NOT NULL,
                $COLUMN_DATA TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS $INDEX_HEAD_WORD
            ON $TABLE_WORD_BOOK($COLUMN_HEAD_WORD)
            """.trimIndent()
        )
    }

    private fun migrateToSnakeCaseColumns(db: SQLiteDatabase) {
        val legacyTableName = "${TABLE_WORD_BOOK}_legacy"

        db.beginTransaction()
        try {
            db.execSQL("DROP INDEX IF EXISTS $INDEX_HEAD_WORD")
            db.execSQL("ALTER TABLE $TABLE_WORD_BOOK RENAME TO $legacyTableName")

            createWordBookTable(db)

            db.execSQL(
                """
                INSERT OR REPLACE INTO $TABLE_WORD_BOOK ($COLUMN_WORD_RANK, $COLUMN_HEAD_WORD, $COLUMN_DATA)
                SELECT wordRank, headWord, data
                FROM $legacyTableName
                """.trimIndent()
            )

            db.execSQL("DROP TABLE IF EXISTS $legacyTableName")
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getWordCount(): Int {
        val db = readableDatabase
        db.rawQuery("SELECT COUNT(*) FROM $TABLE_WORD_BOOK", null).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    fun getTableSchemaSummary(): String {
        val columns = mutableListOf<String>()
        val db = readableDatabase
        db.rawQuery("PRAGMA table_info($TABLE_WORD_BOOK)", null).use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            val typeIndex = cursor.getColumnIndex("type")
            while (cursor.moveToNext()) {
                if (nameIndex < 0 || typeIndex < 0) continue
                val name = cursor.getString(nameIndex)
                val type = cursor.getString(typeIndex)
                columns.add("$name($type)")
            }
        }
        return columns.joinToString(", ")
    }

    companion object {
        const val DATABASE_NAME = "words_notify.db"
        const val DATABASE_VERSION = 2

        const val TABLE_WORD_BOOK = "word_book_toefl"
        const val COLUMN_WORD_RANK = "word_rank"
        const val COLUMN_HEAD_WORD = "head_word"
        const val COLUMN_DATA = "data"
        const val INDEX_HEAD_WORD = "idx_word_book_toefl_head_word"
    }
}
