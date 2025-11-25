# 🔐 Cómo Obtener Auth Data (token, userId, boardId)

## 📖 Guía Completa

Ya he implementado **AuthRepository** y **LoginViewModel** para que guardes y obtengas estos valores automáticamente.

---

## 🎯 Flujo Completo

### **1. Usuario hace Login → Se guarda token/userId**
### **2. TaskViewModel carga automáticamente token/userId/boardId**
### **3. TaskListScreen conecta al WebSocket usando esos valores**

---

## 📝 Paso a Paso

### **Paso 1: Implementar llamada al backend en LoginViewModel** ⏱️ 10-15 min

Abre: `android/app/src/main/java/com/flowboard/presentation/viewmodel/LoginViewModel.kt`

**Busca línea 76 donde dice:**
```kotlin
// TODO: Replace with actual backend API call
```

**Reemplaza la función `simulateBackendLogin` con tu llamada real:**

```kotlin
// ANTES (simulación):
private suspend fun simulateBackendLogin(email: String, password: String): LoginResponse {
    kotlinx.coroutines.delay(1000)
    return if (email.isNotEmpty() && password.length >= 6) {
        LoginResponse(
            success = true,
            token = "eyJhbGc...",
            userId = "test-user-001",
            username = email.substringBefore("@")
        )
    } else {
        LoginResponse(success = false, errorMessage = "Invalid credentials")
    }
}

// DESPUÉS (llamada real al backend):
private suspend fun realBackendLogin(email: String, password: String): LoginResponse {
    // Crear API service si no lo tienes
    val response = authApiService.login(
        LoginRequest(
            email = email,
            password = password
        )
    )

    return LoginResponse(
        success = response.isSuccessful,
        token = response.body()?.token ?: "",
        userId = response.body()?.userId ?: "",
        username = response.body()?.username ?: ""
    )
}
```

---

### **Paso 2: Crear AuthApiService para llamar al backend** ⏱️ 15-20 min

Crea: `android/app/src/main/java/com/flowboard/data/remote/api/AuthApiService.kt`

```kotlin
package com.flowboard.data.remote.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {

    /**
     * Login endpoint
     * POST http://tu-backend.com/api/v1/auth/login
     */
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    /**
     * Register endpoint (opcional)
     */
    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<LoginResponse>
}

// Request body para login
data class LoginRequest(
    val email: String,
    val password: String
)

// Response del backend
data class LoginResponse(
    val token: String,
    val userId: String,
    val username: String,
    val boardId: String? = null  // Board ID por defecto (opcional)
)

// Request body para register (opcional)
data class RegisterRequest(
    val email: String,
    val password: String,
    val username: String
)
```

---

### **Paso 3: Registrar AuthApiService en Hilt** ⏱️ 5 min

Abre: `android/app/src/main/java/com/flowboard/di/NetworkModule.kt`

**Agrega:**

```kotlin
@Provides
@Singleton
fun provideAuthApiService(retrofit: Retrofit): AuthApiService {
    return retrofit.create(AuthApiService::class.java)
}
```

---

### **Paso 4: Inyectar AuthApiService en LoginViewModel**

Abre: `android/app/src/main/java/com/flowboard/presentation/viewmodel/LoginViewModel.kt`

**Línea 22, cambia:**

```kotlin
// ANTES:
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
    // TODO: Inject your API service to call backend login endpoint
) : ViewModel() {

// DESPUÉS:
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val authApiService: AuthApiService
) : ViewModel() {
```

**Y actualiza el método `login` (línea 54):**

```kotlin
fun login(email: String, password: String) {
    viewModelScope.launch {
        _loginState.value = LoginState.Loading

        try {
            // Llamada REAL al backend
            val response = authApiService.login(LoginRequest(email, password))

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                // Guardar datos de auth
                authRepository.saveAuth(
                    token = body.token,
                    userId = body.userId,
                    username = body.username
                )

                // Guardar board ID si el backend lo devuelve
                body.boardId?.let { boardId ->
                    authRepository.saveBoardId(boardId)
                }

                _loginState.value = LoginState.Success
                _isLoggedIn.value = true
            } else {
                _loginState.value = LoginState.Error("Invalid credentials")
            }
        } catch (e: Exception) {
            _loginState.value = LoginState.Error(e.message ?: "Network error")
        }
    }
}
```

---

### **Paso 5: Usar LoginViewModel en LoginScreen**

Abre donde uses `LoginScreen` (probablemente en tu Navigation):

```kotlin
@Composable
fun AuthNavGraph() {
    val loginViewModel: LoginViewModel = hiltViewModel()
    val loginState by loginViewModel.loginState.collectAsStateWithLifecycle()
    val isLoggedIn by loginViewModel.isLoggedIn.collectAsStateWithLifecycle()

    // Si ya está logueado, navegar a TaskList
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            // navController.navigate("taskList")
        }
    }

    LoginScreen(
        onLoginClick = { email, password ->
            loginViewModel.login(email, password)
        },
        onRegisterClick = {
            // navController.navigate("register")
        },
        isLoading = loginState is LoginState.Loading,
        error = (loginState as? LoginState.Error)?.message
    )
}
```

---

## 🔄 Cómo Funciona el Flujo Completo

### **1. Login**
```
Usuario → LoginScreen
       ↓
LoginViewModel.login(email, password)
       ↓
AuthApiService.login() → Backend
       ↓
Backend devuelve: { token, userId, username }
       ↓
AuthRepository.saveAuth(token, userId, username)
       ↓
Datos guardados en DataStore
```

### **2. TaskListScreen se abre**
```
TaskViewModel carga al iniciar (init block)
       ↓
authRepository.getToken() → "eyJhbGc..."
authRepository.getUserId() → "user-123"
authRepository.getBoardId() → "board-456"
       ↓
TaskViewModel expone estos valores como StateFlows
       ↓
TaskListScreen los obtiene con collectAsStateWithLifecycle()
       ↓
LaunchedEffect conecta al WebSocket con esos valores
```

---

## 🧪 Para Pruebas Rápidas (SIN backend aún)

Si todavía NO tienes el backend de auth listo, puedes usar valores hardcodeados:

### **Opción 1: Guardar manualmente en AuthRepository**

En cualquier lugar de tu app:

```kotlin
// En algún lugar al iniciar la app (para testing)
val authRepository: AuthRepository = // inject
viewModelScope.launch {
    authRepository.saveAuth(
        token = "test-jwt-token-123",
        userId = "test-user-001",
        username = "testuser"
    )
    authRepository.saveBoardId("test-board-001")
}
```

### **Opción 2: Modificar LoginViewModel para guardar valores de prueba**

En `LoginViewModel.kt`, línea 87, el método `simulateBackendLogin` ya guarda valores de prueba:

```kotlin
LoginResponse(
    success = true,
    token = "eyJhbGciOi...", // Token de prueba
    userId = "test-user-001",
    username = email.substringBefore("@")
)
```

**Solo haz login con cualquier email/password válido** y se guardarán automáticamente.

---

## 📍 BoardId - ¿De dónde viene?

El `boardId` puede venir de:

### **Opción 1: Backend lo devuelve en login**
```kotlin
// Backend response incluye boardId por defecto
{
  "token": "eyJ...",
  "userId": "user-123",
  "username": "john",
  "boardId": "default-board-456"  // ← Board por defecto del usuario
}
```

### **Opción 2: Usuario selecciona board en la app**
```kotlin
// Cuando usuario selecciona un board/proyecto
authRepository.saveBoardId("selected-board-789")
```

### **Opción 3: Desde argumentos de navegación**
```kotlin
// En Navigation
composable(
    route = "taskList/{boardId}",
    arguments = listOf(navArgument("boardId") { type = NavType.StringType })
) { backStackEntry ->
    val boardId = backStackEntry.arguments?.getString("boardId")
    // Guardar en AuthRepository
    LaunchedEffect(boardId) {
        if (boardId != null) {
            authRepository.saveBoardId(boardId)
        }
    }
    TaskListScreen(...)
}
```

---

## ✅ Verificación

Después de implementar esto, verifica:

1. **Login funciona:**
   - LoginScreen → Ingresar email/password → Loading... → Success

2. **Datos se guardan:**
   - AuthRepository tiene token, userId, boardId guardados

3. **TaskViewModel los carga:**
   - TaskViewModel.authToken tiene valor
   - TaskViewModel.userId tiene valor
   - TaskViewModel.boardId tiene valor

4. **TaskListScreen conecta:**
   - LaunchedEffect se ejecuta
   - WebSocket se conecta
   - Banner muestra "Conectado" (verde)

---

## 🐛 Troubleshooting

### ❌ "token es null en TaskListScreen"

**Causa:** No has hecho login o los datos no se guardaron

**Solución:**
1. Verifica que `loginViewModel.login()` se llame correctamente
2. Verifica que el backend devuelva token válido
3. Para testing, usa valores hardcodeados en `simulateBackendLogin`

---

### ❌ "WebSocket no conecta - token inválido"

**Causa:** Token expirado o formato incorrecto

**Solución:**
1. Genera token nuevo desde el backend
2. Verifica que JWT_SECRET sea el mismo en backend y validación
3. Verifica que el token tenga el formato: `Bearer eyJhbGc...`

---

## 🚀 Resumen

**Ya está TODO implementado en código.** Solo necesitas:

1. ✅ **LoginViewModel** - Ya creado
2. ✅ **AuthRepository** - Ya creado
3. ✅ **TaskViewModel carga auth data** - Ya implementado
4. ✅ **TaskListScreen usa auth data** - Ya implementado

**Lo único que TÚ necesitas hacer:**
- Crear `AuthApiService` para llamar a tu backend
- Implementar endpoint de login en tu backend (si no lo tienes)
- O usar valores de prueba del `simulateBackendLogin`

**Tiempo estimado:** 30-40 minutos

---

**Creado:** 2025-11-25
**Versión:** 1.0.0
