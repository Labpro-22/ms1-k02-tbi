package com.if3210.nimons360.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.if3210.nimons360.data.local.entity.PinnedFamilyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PinnedFamilyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPinnedFamily(entity: PinnedFamilyEntity)

    @Delete
    suspend fun deletePinnedFamily(entity: PinnedFamilyEntity)

    @Query("DELETE FROM pinned_families WHERE familyId = :familyId")
    suspend fun deleteByFamilyId(familyId: Int)

    @Query("SELECT * FROM pinned_families ORDER BY pinnedAt DESC")
    fun getPinnedFamiliesFlow(): Flow<List<PinnedFamilyEntity>>

    @Query("SELECT familyId FROM pinned_families")
    fun getPinnedFamilyIdsFlow(): Flow<List<Int>>
}
