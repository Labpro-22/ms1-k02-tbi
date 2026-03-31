package com.if3210.nimons360.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pinned_families")
data class PinnedFamilyEntity(
    @PrimaryKey
    val familyId: Int,
    val pinnedAt: Long,
)
