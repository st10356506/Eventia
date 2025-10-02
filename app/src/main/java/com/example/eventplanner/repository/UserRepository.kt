package com.example.eventplanner.repository

// repository/UserRepository.kt
import com.example.eventplanner.models.UserRequest
import com.example.eventplanner.models.UserResponse
import com.example.eventplanner.network.EventiaApi
import com.example.eventplanner.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class UserRepository(private val api: EventiaApi = RetrofitClient.eventiaApi) {

    suspend fun createUser(userRequest: UserRequest): Result<UserResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val resp = api.createUser(userRequest)
                if (resp.isSuccessful) {
                    val body = resp.body()
                    if (body != null) Result.success(body)
                    else Result.failure(Exception("Empty response body"))
                } else {
                    // try to read error body or return code
                    Result.failure(Exception("Server error: ${resp.code()} ${resp.message()}"))
                }
            } catch (e: HttpException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getUser(id: String): Result<UserResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val resp = api.getUser(id)
                if (resp.isSuccessful) resp.body()?.let { Result.success(it) } ?: Result.failure(Exception("Empty body"))
                else Result.failure(Exception("Server error: ${resp.code()}"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
