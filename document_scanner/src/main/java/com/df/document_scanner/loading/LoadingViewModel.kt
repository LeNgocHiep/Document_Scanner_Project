package com.df.document_scanner.loading// LoadingViewModel.kt
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LoadingViewModel : ViewModel() {
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    fun showLoading() {
        _isLoading.value = true
    }

    fun hideLoading() {
        _isLoading.value = false
    }
}
