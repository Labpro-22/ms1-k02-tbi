package com.if3210.nimons360.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiWrapper<T>(
    @param:Json(name = "data")
    val data: T,
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @param:Json(name = "email")
    val email: String,
    @param:Json(name = "password")
    val password: String,
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    @param:Json(name = "data")
    val data: TokenData,
)

@JsonClass(generateAdapter = true)
data class TokenData(
    @param:Json(name = "token")
    val token: String,
    @param:Json(name = "expiresAt")
    val expiresAt: String,
    @param:Json(name = "user")
    val user: UserInfo,
)

@JsonClass(generateAdapter = true)
data class UserInfo(
    @param:Json(name = "id")
    val id: Int,
    @param:Json(name = "nim")
    val nim: String,
    @param:Json(name = "email")
    val email: String,
    @param:Json(name = "fullName")
    val fullName: String,
    @param:Json(name = "createdAt")
    val createdAt: String? = null,
    @param:Json(name = "updatedAt")
    val updatedAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class UpdateProfileRequest(
    @param:Json(name = "fullName")
    val fullName: String,
)

@JsonClass(generateAdapter = true)
data class FamilyBasic(
    @param:Json(name = "id")
    val id: Int,
    @param:Json(name = "name")
    val name: String,
    @param:Json(name = "iconUrl")
    val iconUrl: String,
)

@JsonClass(generateAdapter = true)
data class MemberInfo(
    @param:Json(name = "id")
    val id: Int? = null,
    @param:Json(name = "fullName")
    val fullName: String,
    @param:Json(name = "email")
    val email: String,
    @param:Json(name = "joinedAt")
    val joinedAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class FamilyDetail(
    @param:Json(name = "id")
    val id: Int,
    @param:Json(name = "name")
    val name: String,
    @param:Json(name = "iconUrl")
    val iconUrl: String,
    @param:Json(name = "isMember")
    val isMember: Boolean,
    @param:Json(name = "familyCode")
    val familyCode: String? = null,
    @param:Json(name = "createdAt")
    val createdAt: String? = null,
    @param:Json(name = "updatedAt")
    val updatedAt: String? = null,
    @param:Json(name = "members")
    val members: List<MemberInfo>,
)

@JsonClass(generateAdapter = true)
data class DiscoverFamily(
    @param:Json(name = "id")
    val id: Int,
    @param:Json(name = "name")
    val name: String,
    @param:Json(name = "iconUrl")
    val iconUrl: String,
    @param:Json(name = "createdAt")
    val createdAt: String,
    @param:Json(name = "members")
    val members: List<MemberInfo>,
)

@JsonClass(generateAdapter = true)
data class CreateFamilyRequest(
    @param:Json(name = "name")
    val name: String,
    @param:Json(name = "iconUrl")
    val iconUrl: String,
)

@JsonClass(generateAdapter = true)
data class JoinRequest(
    @param:Json(name = "familyId")
    val familyId: Int,
    @param:Json(name = "familyCode")
    val familyCode: String,
)

@JsonClass(generateAdapter = true)
data class JoinResult(
    @param:Json(name = "joined")
    val joined: Boolean,
)

@JsonClass(generateAdapter = true)
data class JoinResponse(
    @param:Json(name = "data")
    val data: JoinResult,
)

@JsonClass(generateAdapter = true)
data class LeaveRequest(
    @param:Json(name = "familyId")
    val familyId: Int,
)

@JsonClass(generateAdapter = true)
data class LeaveResult(
    @param:Json(name = "left")
    val left: Boolean,
)

@JsonClass(generateAdapter = true)
data class LeaveResponse(
    @param:Json(name = "data")
    val data: LeaveResult,
)
