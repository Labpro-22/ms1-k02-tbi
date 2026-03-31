package com.if3210.nimons360.data.repository

import com.if3210.nimons360.data.local.dao.PinnedFamilyDao
import com.if3210.nimons360.data.local.entity.PinnedFamilyEntity
import com.if3210.nimons360.util.Result
import com.if3210.nimons360.util.repositoryCall
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class PinnedFamilyRepository @Inject constructor(
    private val pinnedFamilyDao: PinnedFamilyDao,
) {

    suspend fun pinFamily(familyId: Int): Result<Unit> {
        return repositoryCall {
            pinnedFamilyDao.upsertPinnedFamily(
                PinnedFamilyEntity(
                    familyId = familyId,
                    pinnedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    suspend fun unpinFamily(familyId: Int): Result<Unit> {
        return repositoryCall {
            pinnedFamilyDao.deleteByFamilyId(familyId)
        }
    }

    fun getPinnedIdsFlow(): Flow<Set<Int>> {
        return pinnedFamilyDao.getPinnedFamilyIdsFlow().map { ids -> ids.toSet() }
    }

    fun getPinnedFamiliesFlow(): Flow<List<PinnedFamilyEntity>> {
        return pinnedFamilyDao.getPinnedFamiliesFlow()
    }
}
