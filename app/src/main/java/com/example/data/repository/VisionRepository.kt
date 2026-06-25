package com.example.data.repository

import com.example.data.database.VisionDao
import com.example.data.database.VisionItem
import com.example.data.model.GenerateContentRequest
import com.example.data.model.GenerateContentResponse
import com.example.data.model.GenerateVideosRequest
import com.example.data.model.VeoResponse
import com.example.data.network.RetrofitClient
import kotlinx.coroutines.flow.Flow

class VisionRepository(private val visionDao: VisionDao) {
    val allHistory: Flow<List<VisionItem>> = visionDao.getAllItems()

    suspend fun insertItem(item: VisionItem) {
        visionDao.insertItem(item)
    }

    suspend fun deleteItem(id: Int) {
        visionDao.deleteItemById(id)
    }

    suspend fun clearHistory() {
        visionDao.clearAll()
    }

    suspend fun generateContent(
        model: String,
        apiKey: String,
        request: GenerateContentRequest
    ): GenerateContentResponse {
        return RetrofitClient.service.generateContent(model, apiKey, request)
    }

    suspend fun generateVideos(
        model: String,
        apiKey: String,
        request: GenerateVideosRequest
    ): VeoResponse {
        return RetrofitClient.service.generateVideos(model, apiKey, request)
    }

    suspend fun getOperation(
        operationName: String,
        apiKey: String
    ): VeoResponse {
        return RetrofitClient.service.getOperation(operationName, apiKey)
    }
}
