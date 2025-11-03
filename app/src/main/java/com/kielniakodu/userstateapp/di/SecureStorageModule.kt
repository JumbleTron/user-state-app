package com.kielniakodu.userstateapp.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.kielniakodu.userstateapp.service.AuthTokenSerializer
import com.kielniakodu.userstateapp.service.AuthTokens
import com.kielniakodu.userstateapp.service.CryptoManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecureStorageModule {

    private const val TOKEN_DATA_STORE_NAME = "tasks_track_tokens.pb"

    @Provides
    @Singleton
    fun provideAuthTokenSerializer(cryptoManager: CryptoManager): AuthTokenSerializer {
        return AuthTokenSerializer(cryptoManager)
    }

    @Provides
    @Singleton
    fun provideSecureTokenDataStore(
        @ApplicationContext context: Context,
        serializer: AuthTokenSerializer
    ): DataStore<AuthTokens> {
        return DataStoreFactory.create(
            serializer = serializer,
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = {
                context.dataStoreFile(TOKEN_DATA_STORE_NAME)
            }
        )
    }
}