package com.if3210.nimons360.data.repository

import com.if3210.nimons360.data.remote.ApiService
import com.if3210.nimons360.model.UpdateProfileRequest
import com.if3210.nimons360.model.UserInfo
import com.if3210.nimons360.util.normalizeSingleLineInput
import com.if3210.nimons360.util.Result
import com.if3210.nimons360.util.repositoryCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val apiService: ApiService,
) {

    suspend fun getProfile(): Result<UserInfo> {
        return repositoryCall {
            apiService.getProfile().data
        }
    }

    suspend fun updateProfile(fullName: String): Result<UserInfo> {
        return repositoryCall {
            val normalizedFullName = fullName.normalizeSingleLineInput()

            require(normalizedFullName.isNotBlank()) { "Full name must not be empty" }
            require(normalizedFullName.length <= MAX_FULL_NAME_LENGTH) { "Full name is too long" }

            apiService.updateProfile(
                UpdateProfileRequest(fullName = normalizedFullName),
            ).data
        }
    }

    companion object {
        private const val MAX_FULL_NAME_LENGTH = 100
    }
}
