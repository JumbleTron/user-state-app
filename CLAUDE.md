# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

UserStateApp is an Android application demonstrating secure authentication and session management with automatic token refresh capabilities. Built with Kotlin, Jetpack Compose, and Dagger Hilt.

**Key Technologies:**
- Kotlin 2.2.21
- Jetpack Compose (Material3)
- Dagger Hilt for dependency injection
- Retrofit 3.0.0 + OkHttp 5.3.0 for networking
- DataStore with encrypted storage
- Android KeyStore for cryptographic operations

## Build Commands

**Build the project:**
```bash
./gradlew build
```

**Build debug APK:**
```bash
./gradlew assembleDebug
```

**Build release APK:**
```bash
./gradlew assembleRelease
```

**Run unit tests:**
```bash
./gradlew test
```

**Run instrumented tests:**
```bash
./gradlew connectedAndroidTest
```

**Run specific test class:**
```bash
./gradlew test --tests "com.kielniakodu.userstateapp.ExampleUnitTest"
```

**Clean build:**
```bash
./gradlew clean
```

**Install debug build on device:**
```bash
./gradlew installDebug
```

## Architecture Overview

The app follows a clean architecture pattern with clear separation of concerns:

### Authentication Flow

The authentication system is built around three core states managed by `SessionManager`:
- `AUTHENTICATED` - User has valid tokens and network connectivity
- `UNAUTHENTICATED` - No valid tokens, requires login
- `OFFLINE_AUTHENTICATED` - User was authenticated but network is unavailable

**Token Management:**
- Access tokens are automatically attached to requests via `AuthInterceptor`
- Expired tokens trigger automatic refresh through `TokenAuthenticator`
- Token refresh uses a synchronized block to prevent race conditions from concurrent requests
- If refresh token is invalid/expired, session is cleared and user is logged out

### Secure Storage System

The app implements a multi-layered security approach for storing authentication tokens:

1. **CryptoManager** (`service/CryptoManager.kt`):
   - Uses Android KeyStore for hardware-backed cryptographic keys
   - Implements AES/GCM/NoPadding encryption with 256-bit keys
   - Each encrypted payload includes randomized IV for security
   - Keys never leave the secure hardware

2. **AuthTokenSerializer** (`service/AuthTokenSerializer.kt`):
   - Custom DataStore serializer that encrypts/decrypts tokens on read/write
   - Tokens are concatenated with pipe delimiter then encrypted
   - Transparent encryption - rest of app works with plain AuthTokens objects

3. **DataStore Integration** (`di/SecureStorageModule.kt`):
   - Proto-less DataStore implementation using custom serializer
   - Tokens stored in `tasks_track_tokens.pb` file (encrypted)
   - Coroutine-safe with dedicated IO scope

### Network Architecture

Two distinct OkHttp clients managed through Hilt:

**AuthClient** (`@Named("AuthClient")`):
- For authenticated endpoints requiring access token
- Chain: NetworkStatusInterceptor → AuthInterceptor → TokenAuthenticator → Logging
- Automatically adds `Authorization: Bearer <token>` header
- Handles 401 responses by refreshing tokens

**PublicClient** (`@Named("PublicClient")`):
- For public endpoints (login, token refresh)
- No auth interceptors to avoid circular dependency
- Only includes logging interceptor

**Interceptor Flow:**
1. `NetworkStatusInterceptor` - Monitors connectivity, updates SessionManager state
2. `AuthInterceptor` - Injects current access token into request headers
3. `TokenAuthenticator` - On 401 response, attempts token refresh before retrying request

### Dependency Injection Structure

All DI modules use Hilt's `@InstallIn(SingletonComponent::class)`:

- **NetworkModule** - Provides both OkHttp clients, Retrofit instances, and ApiService implementations
- **SecureStorageModule** - Provides encrypted DataStore and related components
- Key pattern: Use `@Named` qualifiers to differentiate between public and authenticated API services

### ViewModel and State Management

`MainViewModel` receives both API service instances via constructor injection:
- `@Named("PublicApiService")` - For login operations
- Unnamed `ApiService` - Authenticated service for protected endpoints
- All API calls wrapped in viewModelScope coroutines

### Navigation Architecture

The app uses Jetpack Compose Navigation with three main screens:

**Screen Routes** (`navigation/Screen.kt`):
- `Login` - Authentication screen
- `Home` - Main authenticated screen with user data and actions
- `Profile` - Secondary screen demonstrating navigation and back button handling

**Navigation Flow:**
- NavGraph observes `authStatusFlow` and automatically navigates based on authentication state
- After login: Login → Home (clears back stack)
- After logout: Current Screen → Login (clears entire back stack)
- BackHandler prevents navigating back to login after authentication
- Profile screen demonstrates proper navigation with TopAppBar back button

## User Data Access

The app provides easy access to user data extracted from JWT tokens:

**Accessing User Data:**
```kotlin
// Get current user data
val userData = sessionManager.getCurrentUserData()
userData?.let {
    println("User ID: ${it.userId}")
    println("Email: ${it.email}")
    println("Roles: ${it.roles}")
}

// Observe user data changes with Flow
sessionManager.userData.collect { data ->
    // React to user data changes
}
```

**JWT Token Parsing:**
- `JwtManager` automatically parses JWT access tokens
- Extracts user ID, email, username, roles, and expiration
- Validates token expiration
- Returns null for invalid or expired tokens

**Session Events:**
The app emits events for important session changes:
- `SessionEvent.RefreshTokenMissing` - When refresh token is not available
- `SessionEvent.SessionExpired` - When session is cleared

Listen for events in your Activity:
```kotlin
sessionManager.sessionEvents.collect { event ->
    when (event) {
        SessionEvent.RefreshTokenMissing -> {
            // Show user notification
        }
        SessionEvent.SessionExpired -> {
            // Navigate to login
        }
    }
}
```

## Key Implementation Details

### Token Authenticator Race Condition Prevention

The `TokenAuthenticator` implements a double-check locking pattern:
1. First check: Compare request token with current token - another request may have already refreshed
2. Synchronized block: Prevent multiple threads from refreshing simultaneously
3. Second check: Re-verify after entering synchronized block
4. Only refresh if still necessary

This prevents duplicate refresh API calls when multiple concurrent requests receive 401 responses.

### Offline State Management

`NetworkStatusInterceptor` catches `IOException` during network calls:
- On success: Calls `sessionManager.setOnlineState()`
- On IOException: Calls `sessionManager.setOfflineState()`
- Transitions only occur if user is authenticated, preventing invalid state changes

### SessionManager Initialization

`loadInitialAuthStatus()` must be called from MainActivity onCreate:
- Checks DataStore for existing tokens
- Sets initial auth state before UI renders
- Prevents flash of wrong authentication state

## Important Patterns

**Using Synchronized API Access:**
```kotlin
// TokenAuthenticator uses synchronized(this) to prevent race conditions
// when multiple requests fail simultaneously
```

**Hilt Constructor Injection with Named Dependencies:**
```kotlin
@HiltViewModel
class MainViewModel @Inject constructor(
    @param:Named("PublicApiService") private val publicApiService: ApiService,
    private val secureApiService: ApiService
)
```

**Encrypted DataStore Access:**
```kotlin
// Always use SessionManager methods, never access DataStore directly
// Encryption/decryption handled transparently by serializer
// Uses JSON serialization (not pipe delimiter) for robust parsing
suspend fun getAuthTokens(): AuthTokens = secureTokenDataStore.data.first()
```

## Security Review Summary

**Token Storage Security:** ✅ SECURE
- Android KeyStore with hardware-backed encryption
- AES-256-GCM encryption
- JSON serialization for robust parsing (improved from pipe delimiter)
- Keys never leave secure environment

**Authentication Best Practices:** ✅ IMPLEMENTED
- No hardcoded credentials in source code
- Configurable API base URL via BuildConfig
- JWT token validation and parsing
- Automatic token refresh with race condition prevention
- User notification when refresh token is missing

**Configuration:**
- Set API_BASE_URL in app/build.gradle.kts
- Default demo credentials are clearly marked for demonstration only
- All sensitive data excluded from version control via .gitignore

## API Endpoints

Base URL: Configurable via `BuildConfig.API_BASE_URL` in app/build.gradle.kts

- `POST /api/login` - Username/password authentication
- `POST /api/token/refresh` - Refresh access token
- `GET /api/chat/messages` - Protected endpoint (requires auth)