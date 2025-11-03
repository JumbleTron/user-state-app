package com.kielniakodu.userstateapp.di

import com.kielniakodu.userstateapp.interceptor.AuthInterceptor
import com.kielniakodu.userstateapp.interceptor.NetworkStatusInterceptor
import com.kielniakodu.userstateapp.service.ApiService
import com.kielniakodu.userstateapp.service.SessionManager
import com.kielniakodu.userstateapp.service.TokenAuthenticator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideAuthInterceptor(sessionManager: SessionManager): AuthInterceptor {
        return AuthInterceptor(sessionManager)
    }

    @Provides
    @Singleton
    fun provideNetworkStatusInterceptor(sessionManager: SessionManager): NetworkStatusInterceptor {
        return NetworkStatusInterceptor(sessionManager)
    }

    @Provides
    @Singleton
    fun provideTokenAuthenticator(
        sessionManager: SessionManager,
        @Named("PublicApiService") apiService: ApiService
    ): TokenAuthenticator {
        return TokenAuthenticator(sessionManager, apiService)
    }

    @Provides
    @Singleton
    @Named("AuthClient")
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
        networkStatusInterceptor: NetworkStatusInterceptor,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(networkStatusInterceptor)
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    @Provides
    @Singleton
    @Named("PublicClient")
    fun providePublicOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(@Named("AuthClient") okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(com.kielniakodu.userstateapp.BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("RetrofitPublic")
    fun provideRetrofitPublic(@Named("PublicClient") okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(com.kielniakodu.userstateapp.BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    @Provides
    @Singleton
    @Named("PublicApiService")
    fun providePublicApiService(@Named("RetrofitPublic") retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}
