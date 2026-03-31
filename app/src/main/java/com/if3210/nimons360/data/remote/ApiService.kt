package com.if3210.nimons360.data.remote

import com.if3210.nimons360.model.ApiWrapper
import com.if3210.nimons360.model.CreateFamilyRequest
import com.if3210.nimons360.model.DiscoverFamily
import com.if3210.nimons360.model.FamilyBasic
import com.if3210.nimons360.model.FamilyDetail
import com.if3210.nimons360.model.JoinRequest
import com.if3210.nimons360.model.JoinResult
import com.if3210.nimons360.model.LeaveRequest
import com.if3210.nimons360.model.LeaveResult
import com.if3210.nimons360.model.LoginRequest
import com.if3210.nimons360.model.TokenData
import com.if3210.nimons360.model.UpdateProfileRequest
import com.if3210.nimons360.model.UserInfo
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): ApiWrapper<TokenData>

    @GET("api/me")
    suspend fun getProfile(): ApiWrapper<UserInfo>

    @PATCH("api/me")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): ApiWrapper<UserInfo>

    @GET("api/families")
    suspend fun getAllFamilies(): ApiWrapper<List<FamilyBasic>>

    @GET("api/me/families")
    suspend fun getMyFamilies(): ApiWrapper<List<FamilyDetail>>

    @GET("api/families/discover")
    suspend fun discoverFamilies(): ApiWrapper<List<DiscoverFamily>>

    @GET("api/families/{familyId}")
    suspend fun getFamilyDetail(@Path("familyId") familyId: Int): ApiWrapper<FamilyDetail>

    @POST("api/families")
    suspend fun createFamily(@Body request: CreateFamilyRequest): ApiWrapper<FamilyDetail>

    @POST("api/families/join")
    suspend fun joinFamily(@Body request: JoinRequest): ApiWrapper<JoinResult>

    @POST("api/families/leave")
    suspend fun leaveFamily(@Body request: LeaveRequest): ApiWrapper<LeaveResult>
}
