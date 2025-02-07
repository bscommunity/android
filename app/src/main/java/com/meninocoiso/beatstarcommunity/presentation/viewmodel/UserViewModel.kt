package com.meninocoiso.beatstarcommunity.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.beatstarcommunity.data.repository.UserRepository
import com.meninocoiso.beatstarcommunity.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {
    private val _users = MutableStateFlow<Result<List<User>>?>(null)
    val users: StateFlow<Result<List<User>>?> = _users.asStateFlow()

    fun fetchUsers() {
        viewModelScope.launch {
            userRepository.getUsers().collect { result ->
                _users.value = result
            }
        }
    }
}