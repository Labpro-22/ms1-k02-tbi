package com.if3210.nimons360.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.if3210.nimons360.data.local.entity.FavoriteLocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteLocationDao {

    @Insert
    suspend fun insertFavoriteLocation(entity: FavoriteLocationEntity): Long

    @Delete
    suspend fun deleteFavoriteLocation(entity: FavoriteLocationEntity)

    @Query("DELETE FROM favorite_locations WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT * FROM favorite_locations ORDER BY createdAt DESC")
    fun getFavoriteLocationsFlow(): Flow<List<FavoriteLocationEntity>>
}
