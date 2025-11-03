# Android User Session Management Example

A production-ready Android application demonstrating best practices for secure user session management, JWT token handling, automatic token refresh, and offline authentication state.

## Features

- **Secure Token Storage**: Hardware-backed encryption using Android KeyStore with AES-256-GCM
- **JWT Token Parsing**: Automatic extraction of user data from JWT access tokens
- **Automatic Token Refresh**: Transparent token refresh with race condition prevention
- **Offline Support**: Maintains authentication state when network is unavailable
- **Session Events**: Observable events for refresh token missing and session expiration
- **Clean Architecture**: MVVM pattern with Hilt dependency injection
- **Modern Android Stack**: Jetpack Compose, Kotlin Coroutines, and Flow

## Architecture

### Security

**Token Storage:**
- All tokens are encrypted using Android KeyStore
- AES-256-GCM encryption with hardware-backed keys
- JSON serialization with Gson for robust parsing
- Keys never leave the secure hardware environment

**Network Security:**
- Separate OkHttp clients for authenticated and public endpoints
- Automatic Authorization header injection
- Token refresh with double-check locking to prevent race conditions
- Network status monitoring for offline detection

### Authentication Flow

```
1. User logs in → Tokens stored encrypted
2. API request → Access token auto-injected
3. Token expired (401) → Automatic refresh
4. Refresh succeeds → Request retried with new token
5. Refresh fails → User logged out + notification
```

### Session States

- **AUTHENTICATED**: Valid tokens, network available
- **OFFLINE_AUTHENTICATED**: Valid tokens, no network
- **UNAUTHENTICATED**: No valid tokens

## Tech Stack

- **Language**: Kotlin 2.2.21
- **UI**: Jetpack Compose with Material3
- **DI**: Dagger Hilt
- **Networking**: Retrofit 3.0 + OkHttp 5.3
- **Storage**: DataStore with custom encrypted serializer
- **Security**: Android KeyStore, AndroidX Security Crypto
- **JWT**: Auth0 JWT Decode library
- **Architecture**: MVVM + Clean Architecture

## Setup

### Prerequisites

- Android Studio Jellyfish | 2023.3.1 or later
- Android SDK API 24+ (minimum)
- JDK 8 or higher

### Configuration

1. **Clone the repository**

```bash
git clone https://github.com/yourusername/UserStateApp.git
cd UserStateApp
```

2. **Configure API Base URL**

Edit `app/build.gradle.kts` and update the API_BASE_URL:

```kotlin
buildConfigField("String", "API_BASE_URL", "\"https://your-api-url.example.com\"")
```

3. **Configure JWT Claims (Optional)**

If your backend uses different JWT claim names, edit `JwtManager.kt`:

```kotlin
val userId = jwt.getClaim("sub").asString() ?: ""
val email = jwt.getClaim("email").asString() ?: ""
// Adjust claim names to match your backend
```

4. **Build and Run**

```bash
./gradlew assembleDebug
./gradlew installDebug
```

## Usage

### Basic Authentication

```kotlin
@HiltViewModel
class YourViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {

    fun observeAuthState() {
        viewModelScope.launch {
            sessionManager.authStatusFlow.collect { status ->
                when (status) {
                    AuthStatus.AUTHENTICATED -> {
                        // User is logged in
                        val userData = sessionManager.getCurrentUserData()
                        println("User email: ${userData?.email}")
                    }
                    AuthStatus.UNAUTHENTICATED -> {
                        // Navigate to login
                    }
                    AuthStatus.OFFLINE_AUTHENTICATED -> {
                        // Show offline indicator
                    }
                }
            }
        }
    }
}
```

### Accessing User Data

```kotlin
// Get current user data from JWT token
val userData: UserData? = sessionManager.getCurrentUserData()

// Observe user data changes
sessionManager.userData.collect { userData ->
    userData?.let {
        println("User ID: ${it.userId}")
        println("Email: ${it.email}")
        println("Roles: ${it.roles}")
        println("Token expires at: ${it.expiresAt}")
    }
}
```

### Handling Session Events

```kotlin
// Listen for session events in your Activity
lifecycleScope.launch {
    sessionManager.sessionEvents.collect { event ->
        when (event) {
            SessionEvent.RefreshTokenMissing -> {
                // Show user notification
                showDialog("Session expired", "Please log in again")
            }
            SessionEvent.SessionExpired -> {
                // Navigate to login
            }
        }
    }
}
```

### Making API Calls

The app automatically handles token injection and refresh:

```kotlin
@Inject
lateinit var apiService: ApiService // Authenticated service

viewModelScope.launch {
    try {
        val response = apiService.getMessages()
        if (response.isSuccessful) {
            // Handle success
        }
    } catch (e: Exception) {
        // Handle error
    }
}
```

## API Requirements

Your backend API should provide these endpoints:

```
POST /api/login
Request: { "username": "string", "password": "string" }
Response: { "token": "jwt_access_token", "refresh_token": "jwt_refresh_token" }

POST /api/token/refresh
Request: { "refresh_token": "string" }
Response: { "token": "new_access_token", "refresh_token": "new_refresh_token" }
```

### JWT Token Structure

Your access token should include these claims:

```json
{
  "sub": "user_id",
  "email": "user@example.com",
  "name": "User Name",
  "roles": ["user", "admin"],
  "exp": 1234567890
}
```

## Security Best Practices Implemented

- ✅ Hardware-backed encryption for token storage
- ✅ No hardcoded credentials or API keys
- ✅ Proper error handling and logging
- ✅ Token refresh race condition prevention
- ✅ Secure communication (HTTPS only)
- ✅ JWT token validation and expiration checking
- ✅ Proper cleanup on logout
- ✅ ProGuard/R8 rules for obfuscation (release builds)

## Project Structure

```
app/src/main/java/com/kielniakodu/userstateapp/
├── di/                          # Dependency Injection modules
│   ├── NetworkModule.kt         # Network and API configuration
│   └── SecureStorageModule.kt   # Encrypted DataStore setup
├── domain/                      # Domain models
│   ├── AuthStatus.kt           # Authentication states
│   └── UserData.kt             # User information model
├── interceptor/                 # OkHttp interceptors
│   ├── AuthInterceptor.kt      # Token injection
│   ├── NetworkStatusInterceptor.kt # Offline detection
│   └── TokenAuthenticator.kt   # Automatic token refresh
├── service/                     # Services and managers
│   ├── ApiService.kt           # Retrofit API interface
│   ├── AuthTokenSerializer.kt  # Encrypted token serialization
│   ├── CryptoManager.kt        # Android KeyStore wrapper
│   ├── JwtManager.kt           # JWT parsing utilities
│   └── SessionManager.kt       # Session state management
├── ui/                         # UI components
│   └── theme/                  # Compose theme
├── MainActivity.kt             # Main activity
└── MainViewModel.kt            # Main view model
```

## Testing

Run unit tests:

```bash
./gradlew test
```

Run instrumented tests:

```bash
./gradlew connectedAndroidTest
```

## Common Issues

### Build Issues

If you encounter build errors after cloning:

```bash
./gradlew clean
./gradlew build --refresh-dependencies
```

### Token Parsing Errors

Ensure your JWT tokens use standard claim names or update `JwtManager.kt` to match your backend's claim structure.

### Network Errors

Check that `API_BASE_URL` in `build.gradle.kts` is correctly configured and accessible from your device/emulator.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the project
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Inspired by modern Android security best practices
- Built with guidance from Android Security documentation
- JWT handling based on Auth0 recommendations

## Contact

For questions or feedback, please open an issue on GitHub.

---

**Note**: This is a demonstration project. Always review and adapt the security measures to your specific requirements before using in production.