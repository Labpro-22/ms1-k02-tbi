package com.if3210.nimons360.data.repository

import com.if3210.nimons360.data.remote.ApiService
import com.if3210.nimons360.model.CreateFamilyRequest
import com.if3210.nimons360.model.DiscoverFamily
import com.if3210.nimons360.model.FamilyBasic
import com.if3210.nimons360.model.FamilyDetail
import com.if3210.nimons360.model.JoinRequest
import com.if3210.nimons360.model.JoinResult
import com.if3210.nimons360.model.LeaveRequest
import com.if3210.nimons360.model.LeaveResult
import com.if3210.nimons360.util.Constants
import com.if3210.nimons360.util.normalizeSingleLineInput
import com.if3210.nimons360.util.Result
import com.if3210.nimons360.util.repositoryCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FamilyRepository @Inject constructor(
    private val apiService: ApiService,
) {

    suspend fun getAllFamilies(): Result<List<FamilyBasic>> {
        return repositoryCall {
            apiService.getAllFamilies().data
        }
    }

    suspend fun getMyFamilies(): Result<List<FamilyDetail>> {
        return repositoryCall {
            apiService.getMyFamilies().data
        }
    }

    suspend fun discoverFamilies(): Result<List<DiscoverFamily>> {
        return repositoryCall {
            apiService.discoverFamilies().data
        }
    }

    suspend fun getFamilyDetail(familyId: Int): Result<FamilyDetail> {
        return repositoryCall {
            require(familyId > 0) { "Family id must be greater than 0" }
            apiService.getFamilyDetail(familyId).data
        }
    }

    suspend fun createFamily(name: String, iconUrl: String): Result<FamilyDetail> {
        return repositoryCall {
            val normalizedName = name.normalizeSingleLineInput()
            val normalizedIconUrl = iconUrl.trim()

            require(normalizedName.isNotBlank()) { "Family name must not be empty" }
            require(normalizedName.length <= MAX_FAMILY_NAME_LENGTH) { "Family name is too long" }
            require(normalizedIconUrl in Constants.FAMILY_ICON_URLS) {
                "Family icon URL must be one of the approved icon assets"
            }

            apiService.createFamily(
                CreateFamilyRequest(
                    name = normalizedName,
                    iconUrl = normalizedIconUrl,
                ),
            ).data
        }
    }

    suspend fun joinFamily(familyId: Int, familyCode: String): Result<JoinResult> {
        return repositoryCall {
            val normalizedFamilyCode = familyCode.trim()

            require(familyId > 0) { "Family id must be greater than 0" }
            require(normalizedFamilyCode.isNotBlank()) { "Family code must not be empty" }
            require(normalizedFamilyCode.length in MIN_FAMILY_CODE_LENGTH..MAX_FAMILY_CODE_LENGTH) {
                "Family code length is invalid"
            }
            require(FAMILY_CODE_PATTERN.matches(normalizedFamilyCode)) {
                "Family code format is invalid"
            }

            apiService.joinFamily(
                JoinRequest(
                    familyId = familyId,
                    familyCode = normalizedFamilyCode,
                ),
            ).data
        }
    }

    suspend fun leaveFamily(familyId: Int): Result<LeaveResult> {
        return repositoryCall {
            require(familyId > 0) { "Family id must be greater than 0" }
            apiService.leaveFamily(LeaveRequest(familyId = familyId)).data
        }
    }

    companion object {
        private const val MAX_FAMILY_NAME_LENGTH = 100
        private const val MIN_FAMILY_CODE_LENGTH = 4
        private const val MAX_FAMILY_CODE_LENGTH = 32
        private val FAMILY_CODE_PATTERN = Regex("^[A-Za-z0-9]+$")
    }
}
