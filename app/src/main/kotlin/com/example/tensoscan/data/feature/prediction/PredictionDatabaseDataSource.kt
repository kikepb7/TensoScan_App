package com.example.tensoscan.data.feature.prediction

import com.example.tensoscan.data.database.dao.PredictionDao
import com.example.tensoscan.data.database.entities.PredictionEntity
import com.example.tensoscan.data.utils.predictionEntityToPredictionModel
import com.example.tensoscan.ui.model.PredictionModel

class PredictionDatabaseDataSource(
    private val predictionDao: PredictionDao
) {

    suspend fun findPredictionsFromDatabase(): List<PredictionModel> {
        return predictionDao.findAllPredictions().map { predictionEntity ->
            predictionEntity.predictionEntityToPredictionModel()
        }
    }

    suspend fun insertPredictionToDatabase(prediction: PredictionEntity) {
        return predictionDao.insertPrediction(prediction = prediction)
    }

    suspend fun removePredictionFromDatabase(predictionId: Int) {
        return predictionDao.removePredictionById(predictionId = predictionId)
    }

    suspend fun clearPredictionList() {
        return predictionDao.removeAllPredictions()
    }
}