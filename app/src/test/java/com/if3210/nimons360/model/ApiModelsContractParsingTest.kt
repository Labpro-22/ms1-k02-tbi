package com.if3210.nimons360.model

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiModelsContractParsingTest {

    private val moshi = Moshi.Builder().build()

    @Test
    fun loginResponse_parsesAsApiWrapperTokenData() {
        val json =
            """
            {
              "data": {
                "token": "jwt-token-value",
                "expiresAt": "2026-03-15T10:00:00Z",
                "user": {
                  "id": 12,
                  "nim": "13522084",
                  "email": "13522084@std.stei.itb.ac.id",
                  "fullName": "Dhafin Fawwaz Ikramullah"
                }
              }
            }
            """.trimIndent()

        val type = Types.newParameterizedType(ApiWrapper::class.java, TokenData::class.java)
        val adapter = moshi.adapter<ApiWrapper<TokenData>>(type)

        val parsed = adapter.fromJson(json)

        assertNotNull(parsed)
        assertEquals("jwt-token-value", parsed!!.data.token)
        assertEquals("2026-03-15T10:00:00Z", parsed.data.expiresAt)
        assertEquals(12, parsed.data.user.id)
        assertEquals("13522084", parsed.data.user.nim)
    }

    @Test
    fun userProfile_parsesCreatedAndUpdatedAt() {
        val json =
            """
            {
              "data": {
                "id": 12,
                "nim": "13522084",
                "email": "13522084@std.stei.itb.ac.id",
                "fullName": "Dhafin Fawwaz Ikramullah",
                "createdAt": "2026-03-15T10:00:00Z",
                "updatedAt": "2026-03-15T10:00:00Z"
              }
            }
            """.trimIndent()

        val type = Types.newParameterizedType(ApiWrapper::class.java, UserInfo::class.java)
        val adapter = moshi.adapter<ApiWrapper<UserInfo>>(type)

        val parsed = adapter.fromJson(json)

        assertNotNull(parsed)
        assertEquals("2026-03-15T10:00:00Z", parsed!!.data.createdAt)
        assertEquals("2026-03-15T10:00:00Z", parsed.data.updatedAt)
    }

    @Test
    fun familyDetail_parsesBothMemberAndNonMemberVariants() {
        val nonMemberJson =
            """
            {
              "data": {
                "id": 41,
                "name": "IF3210 Squad",
                "iconUrl": "https://mad.labpro.hmif.dev/assets/family_icon_1.png",
                "isMember": false,
                "createdAt": "2026-03-15T10:00:00Z",
                "updatedAt": "2026-03-15T10:00:00Z",
                "members": [
                  {
                    "fullName": "D********* T*********",
                    "email": "********@std.stei.itb.ac.id"
                  }
                ]
              }
            }
            """.trimIndent()

        val memberJson =
            """
            {
              "data": {
                "id": 41,
                "name": "IF3210 Squad",
                "iconUrl": "https://mad.labpro.hmif.dev/assets/family_icon_1.png",
                "isMember": true,
                "familyCode": "ABC123",
                "createdAt": "2026-03-15T10:00:00Z",
                "updatedAt": "2026-03-15T10:00:00Z",
                "members": [
                  {
                    "fullName": "Dewantoro Triatmojo",
                    "email": "13522011@std.stei.itb.ac.id"
                  }
                ]
              }
            }
            """.trimIndent()

        val type = Types.newParameterizedType(ApiWrapper::class.java, FamilyDetail::class.java)
        val adapter = moshi.adapter<ApiWrapper<FamilyDetail>>(type)

        val nonMember = adapter.fromJson(nonMemberJson)
        val member = adapter.fromJson(memberJson)

        assertNotNull(nonMember)
        assertNotNull(member)

        assertFalse(nonMember!!.data.isMember)
        assertNull(nonMember.data.familyCode)
        assertEquals(1, nonMember.data.members.size)
        assertNull(nonMember.data.members.first().id)

        assertTrue(member!!.data.isMember)
        assertEquals("ABC123", member.data.familyCode)
    }

    @Test
    fun discoverFamilies_parsesMaskedMemberInfo() {
        val json =
            """
            {
              "data": [
                {
                  "id": 41,
                  "name": "IF3210 Squad",
                  "iconUrl": "https://mad.labpro.hmif.dev/assets/family_icon_1.png",
                  "createdAt": "2026-03-15T10:00:00Z",
                  "members": [
                    {
                      "fullName": "D********* T*********",
                      "email": "********@std.stei.itb.ac.id"
                    }
                  ]
                }
              ]
            }
            """.trimIndent()

        val listType = Types.newParameterizedType(List::class.java, DiscoverFamily::class.java)
        val wrapperType = Types.newParameterizedType(ApiWrapper::class.java, listType)
        val adapter = moshi.adapter<ApiWrapper<List<DiscoverFamily>>>(wrapperType)

        val parsed = adapter.fromJson(json)

        assertNotNull(parsed)
        assertEquals(1, parsed!!.data.size)
        assertEquals("IF3210 Squad", parsed.data.first().name)
        assertNull(parsed.data.first().members.first().id)
        assertNull(parsed.data.first().members.first().joinedAt)
    }

    @Test
    fun joinAndLeaveResponses_parseResultFlags() {
        val joinJson =
            """
            {
              "data": {
                "joined": true
              }
            }
            """.trimIndent()

        val leaveJson =
            """
            {
              "data": {
                "left": true
              }
            }
            """.trimIndent()

        val joinType = Types.newParameterizedType(ApiWrapper::class.java, JoinResult::class.java)
        val joinAdapter = moshi.adapter<ApiWrapper<JoinResult>>(joinType)

        val leaveType = Types.newParameterizedType(ApiWrapper::class.java, LeaveResult::class.java)
        val leaveAdapter = moshi.adapter<ApiWrapper<LeaveResult>>(leaveType)

        val joinParsed = joinAdapter.fromJson(joinJson)
        val leaveParsed = leaveAdapter.fromJson(leaveJson)

        assertNotNull(joinParsed)
        assertNotNull(leaveParsed)
        assertTrue(joinParsed!!.data.joined)
        assertTrue(leaveParsed!!.data.left)
    }
}
