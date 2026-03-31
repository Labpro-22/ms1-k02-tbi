package com.if3210.nimons360.data.repository

import com.if3210.nimons360.data.local.dao.FavoriteLocationDao
import com.if3210.nimons360.data.local.entity.FavoriteLocationEntity
import com.if3210.nimons360.util.Result
import com.if3210.nimons360.util.repositoryCall
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class FavoriteLocationRepository @Inject constructor(
    private val favoriteLocationDao: FavoriteLocationDao,
) {

    suspend fun saveFavoriteLocation(
        latitude: Double,
        longitude: Double,
        label: String,
    ): Result<Long> {
        return repositoryCall {
            favoriteLocationDao.insertFavoriteLocation(
                FavoriteLocationEntity(
                    latitude = latitude,
                    longitude = longitude,
                    label = label,
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    suspend fun deleteFavoriteLocation(id: Int): Result<Unit> {
        return repositoryCall {
            favoriteLocationDao.deleteById(id)
        }
    }

    fun getAllFavoriteLocationsFlow(): Flow<List<FavoriteLocationEntity>> {
        return favoriteLocationDao.getFavoriteLocationsFlow()
    }
}
