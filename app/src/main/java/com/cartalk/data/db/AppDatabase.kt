package com.cartalk.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.cartalk.data.models.Document
import com.cartalk.data.models.DocumentType
import com.cartalk.data.models.Message

class DocumentTypeConverter {
    @TypeConverter
    fun fromDocumentType(type: DocumentType): String = type.name

    @TypeConverter
    fun toDocumentType(value: String): DocumentType = DocumentType.valueOf(value)
}

@Database(
    entities = [Document::class, Message::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(DocumentTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cartalk_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
