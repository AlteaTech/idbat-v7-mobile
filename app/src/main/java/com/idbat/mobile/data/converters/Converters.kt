package com.idbat.mobile.data.converters

import androidx.room.TypeConverter
import com.idbat.mobile.data.entities.SensSynchro
import com.idbat.mobile.data.entities.TypeSynchro
import java.util.*

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

    @TypeConverter
    fun fromSensSynchro(value: String?): SensSynchro? {
        return value?.let { enumValueOf<SensSynchro>(it) }
    }

    @TypeConverter
    fun sensSynchroToString(sens: SensSynchro?): String? {
        return sens?.name
    }
}
