package com.example.skbt_up_gibdd_eyewitness.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [MessageEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class EyewitnessDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
}
