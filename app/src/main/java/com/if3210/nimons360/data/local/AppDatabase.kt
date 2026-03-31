package com.if3210.nimons360.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.if3210.nimons360.data.local.dao.FavoriteLocationDao
import com.if3210.nimons360.data.local.dao.PinnedFamilyDao
import com.if3210.nimons360.data.local.entity.FavoriteLocationEntity
import com.if3210.nimons360.data.local.entity.PinnedFamilyEntity

@Database(
    entities = [PinnedFamilyEntity::class, FavoriteLocationEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pinnedFamilyDao(): PinnedFamilyDao
    abstract fun favoriteLocationDao(): FavoriteLocationDao
}
