package io.github.narendrakumar2259.networkinspector.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.narendrakumar2259.networkinspector.data.local.AppDatabase
import io.github.narendrakumar2259.networkinspector.data.model.NetworkRequest
import io.github.narendrakumar2259.networkinspector.data.repository.NetworkRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NetworkRepository

    private val _requests = MutableStateFlow<List<NetworkRequest>>(emptyList())
    val requests: StateFlow<List<NetworkRequest>> = _requests.asStateFlow()

    private val _isVpnRunning = MutableStateFlow(false)
    val isVpnRunning: StateFlow<Boolean> = _isVpnRunning.asStateFlow()

    init {
        val dao = AppDatabase.getDatabase(application).networkRequestDao()
        repository = NetworkRepository(dao)

        viewModelScope.launch {
            repository.allRequests.collect { list ->
                _requests.value = list
            }
        }
    }

    fun setVpnRunning(running: Boolean) {
        _isVpnRunning.value = running
    }

    fun clearRequests() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }
}