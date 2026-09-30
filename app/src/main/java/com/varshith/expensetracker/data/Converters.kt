package com.varshith.expensetracker.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromFrequency(value: RecurrenceFrequency): String = value.name

    @TypeConverter
    fun toFrequency(value: String): RecurrenceFrequency = RecurrenceFrequency.valueOf(value)
}