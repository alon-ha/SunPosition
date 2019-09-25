package com.alon.sunposition

import android.content.Context
import java.util.*
import android.content.Context.MODE_PRIVATE


interface DataPersistencing {
    fun saveLastDateLoadedSunPosition(date: Date)
    fun getLastDateLoadedSunPosition(): Date?
}

class DataPersistence(context: Context): DataPersistencing {

    private val sunPositionSharedPreferencesKey = "sunPosition"
    private val lastDateKey = "lastDate"
    private val sharedPreferences = context.getSharedPreferences(sunPositionSharedPreferencesKey, MODE_PRIVATE)


    override fun saveLastDateLoadedSunPosition(date: Date) {
        val edit = sharedPreferences.edit()
        edit.putLong(lastDateKey, date.time)
        edit.apply()
    }

    override fun getLastDateLoadedSunPosition(): Date? {
        val time = sharedPreferences.getLong(lastDateKey, 0)
        if (time == 0.toLong()) {
            return null
        }
        return Date(time)
    }
}