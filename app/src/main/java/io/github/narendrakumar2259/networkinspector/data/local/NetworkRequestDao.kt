package io.github.narendrakumar2259.networkinspector.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.github.narendrakumar2259.networkinspector.data.model.NetworkRequest
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkRequestDao {

    @Insert
    suspend fun insert(request: NetworkRequest)

    @Query("SELECT * FROM network_requests ORDER BY timestamp DESC")
    fun getAllRequests(): Flow<List<NetworkRequest>>

    @Query("SELECT * FROM network_requests WHERE id = :id")
    suspend fun getRequestById(id: Long): NetworkRequest?

    @Query("SELECT * FROM network_requests WHERE destPort = :port ORDER BY timestamp DESC")
    fun getRequestsByPort(port: Int): Flow<List<NetworkRequest>>

    @Query("SELECT * FROM network_requests WHERE method IS NOT NULL ORDER BY timestamp DESC")
    fun getHttpRequests(): Flow<List<NetworkRequest>>

    @Query("DELETE FROM network_requests")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM network_requests")
    suspend fun getCount(): Int

    // Keep only the latest N requests
    @Query("DELETE FROM network_requests WHERE id NOT IN (SELECT id FROM network_requests ORDER BY timestamp DESC LIMIT :maxCount)")
    suspend fun trimToSize(maxCount: Int)
}