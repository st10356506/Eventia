package com.example.eventplanner.viewmodel

// viewmodel/UserViewModel.kt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.example.eventplanner.models.UserRequest
import com.example.eventplanner.models.UserResponse
import com.example.eventplanner.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UserViewModel(private val repo: UserRepository = UserRepository()): ViewModel() {

    private val _userState = MutableStateFlow<Result<UserResponse>?>(null)
    val userState: StateFlow<Result<UserResponse>?> = _userState

    fun createUser(request: UserRequest) {
        viewModelScope.launch {
            val result = repo.createUser(request)
            _userState.value = result
        }
    }

    fun getUser(id: String) {
        viewModelScope.launch {
            _userState.value = repo.getUser(id)
        }
    }
}
