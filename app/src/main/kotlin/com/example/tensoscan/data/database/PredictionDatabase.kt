package com.example.tensoscan.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.tensoscan.data.database.dao.PredictionDao
import com.example.tensoscan.data.database.entities.PredictionEntity

@Database(entities = [PredictionEntity::class], version = 1)
abstract class PredictionDatabase: RoomDatabase() {

    abstract fun getPredictionDao(): PredictionDao
}