package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [DeviceEntity::class], version = 1, exportSchema = false)
abstract class NetWardDatabase : RoomDatabase() {

    abstract fun deviceDao(): DeviceDao

    companion object {
        @Volatile
        private var INSTANCE: NetWardDatabase? = null

        fun getInstance(context: Context): NetWardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NetWardDatabase::class.java,
                    "netward_database.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
