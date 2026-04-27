package io.github.narendrakumar2259.networkinspector.data.repository

import io.github.narendrakumar2259.networkinspector.data.local.NetworkRequestDao
import io.github.narendrakumar2259.networkinspector.data.model.NetworkRequest
import kotlinx.coroutines.flow.Flow

class NetworkRepository(private val dao: NetworkRequestDao) {

    val allRequests: Flow<List<NetworkRequest>> = dao.getAllRequests()

    val httpRequests: Flow<List<NetworkRequest>> = dao.getHttpRequests()

    suspend fun insert(request: NetworkRequest) {
        dao.insert(request)
        // Auto-trim to keep max 1000 entries
        val count = dao.getCount()
        if (count > 1000) {
            dao.trimToSize(1000)
        }
    }

    suspend fun clearAll() {
        dao.clearAll()
    }

    fun getRequestsByPort(port: Int): Flow<List<NetworkRequest>> {
        return dao.getRequestsByPort(port)
    }

    suspend fun getRequestById(id: Long): NetworkRequest? {
        return dao.getRequestById(id)
    }
}