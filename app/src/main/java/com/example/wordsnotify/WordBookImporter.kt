package com.example.wordsnotify

import android.content.Context
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class WordBookImporter(private val context: Context) {
    data class ImportResult(
        val importedCount: Int
    )

    fun importFromAsset(assetFileName: String = "toefl.json"): ImportResult {
        val dbHelper = WordBookDatabaseHelper(context)
        val db = dbHelper.writableDatabase
        var importedCount = 0

        db.beginTransaction()
        try {
            val statement = db.compileStatement(
                """
                INSERT OR REPLACE INTO ${WordBookDatabaseHelper.TABLE_WORD_BOOK}
                (${WordBookDatabaseHelper.COLUMN_WORD_RANK}, ${WordBookDatabaseHelper.COLUMN_HEAD_WORD}, ${WordBookDatabaseHelper.COLUMN_DATA})
                VALUES (?, ?, ?)
                """.trimIndent()
            )

            context.assets.open(assetFileName).use { inputStream ->
                InputStreamReader(inputStream, StandardCharsets.UTF_8).use { streamReader ->
                    JsonReader(streamReader).use { reader ->
                        reader.beginArray()
                        while (reader.hasNext()) {
                            val element = JsonParser.parseReader(reader)
                            if (!element.isJsonObject) {
                                continue
                            }

                            val jsonObject = element.asJsonObject
                            val wordRank = jsonObject.getIntSafely("wordRank") ?: continue
                            val headWord = jsonObject.getStringSafely("headWord") ?: ""
                            val rawData = jsonObject.toString()

                            statement.clearBindings()
                            statement.bindLong(1, wordRank.toLong())
                            statement.bindString(2, headWord)
                            statement.bindString(3, rawData)
                            statement.executeInsert()
                            importedCount++
                        }
                        reader.endArray()
                    }
                }
            }

            db.setTransactionSuccessful()
            return ImportResult(importedCount = importedCount)
        } finally {
            db.endTransaction()
            db.close()
            dbHelper.close()
        }
    }

    private fun JsonObject.getIntSafely(key: String): Int? {
        val value = get(key) ?: return null
        return if (value.isJsonPrimitive && value.asJsonPrimitive.isNumber) value.asInt else null
    }

    private fun JsonObject.getStringSafely(key: String): String? {
        val value = get(key) ?: return null
        return if (value.isJsonPrimitive) value.asString else null
    }
}
