package com.if3210.nimons360.di

import android.content.Context
import androidx.room.Room
import com.if3210.nimons360.data.local.AppDatabase
import com.if3210.nimons360.data.local.DatabasePassphraseProvider
import com.if3210.nimons360.data.local.dao.FavoriteLocationDao
import com.if3210.nimons360.data.local.dao.PinnedFamilyDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import timber.log.Timber

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    private const val DATABASE_NAME = "nimons360.db"
    private const val TAG = "DatabaseModule"
    private val SQLITE_HEADER_PREFIX = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        databasePassphraseProvider: DatabasePassphraseProvider,
    ): AppDatabase {
        System.loadLibrary("sqlcipher")
        removeLegacyPlaintextDatabaseIfNeeded(context)

        val supportFactory = SupportOpenHelperFactory(databasePassphraseProvider.getOrCreatePassphrase())
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            DATABASE_NAME,
        )
            .openHelperFactory(supportFactory)
            .build()
    }

    @Provides
    fun providePinnedFamilyDao(appDatabase: AppDatabase): PinnedFamilyDao {
        return appDatabase.pinnedFamilyDao()
    }

    @Provides
    fun provideFavoriteLocationDao(appDatabase: AppDatabase): FavoriteLocationDao {
        return appDatabase.favoriteLocationDao()
    }

    private fun removeLegacyPlaintextDatabaseIfNeeded(context: Context) {
        val databaseFile = context.getDatabasePath(DATABASE_NAME)
        if (!databaseFile.exists()) {
            return
        }

        val header = ByteArray(SQLITE_HEADER_PREFIX.size)
        val bytesRead = runCatching {
            databaseFile.inputStream().use { stream ->
                stream.read(header)
            }
        }.getOrElse {
            Timber.tag(TAG).w(it, "Failed to inspect database header before SQLCipher initialization")
            return
        }

        if (bytesRead == SQLITE_HEADER_PREFIX.size && header.contentEquals(SQLITE_HEADER_PREFIX)) {
            val deleted = context.deleteDatabase(DATABASE_NAME)
            if (!deleted) {
                Timber.tag(TAG).w("Failed to remove legacy plaintext database before enabling SQLCipher")
            }
        }
    }
}
