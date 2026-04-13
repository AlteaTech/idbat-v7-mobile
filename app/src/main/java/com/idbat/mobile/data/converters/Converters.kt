package com.idbat.mobile.data.converters

import androidx.room.TypeConverter
import com.idbat.mobile.data.entities.TypeSynchro
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromTypeSynchro(value: String?): TypeSynchro? {
        return value?.let { enumValueOf<TypeSynchro>(it) }
    }

    @TypeConverter
    fun typeSynchroToString(type: TypeSynchro?): String? {
        return type?.name
    }
}
