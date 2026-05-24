package com.flowboard.domain

import com.flowboard.data.database.DatabaseFactory.dbQuery
import com.flowboard.data.database.PasswordResetTokens
import com.flowboard.data.database.Users
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import com.flowboard.data.models.*
import com.flowboard.plugins.JwtConfig
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
// [PSP - RA5.b] Librería jBCrypt — implementa BCrypt, el algoritmo de hash de contraseñas.
// Equivalente Python: import bcrypt (el mismo algoritmo, distinta librería)
import org.mindrot.jbcrypt.BCrypt
import java.util.*

// [PSP] AuthService es un Singleton (Kotlin object).
// Un "object" garantiza que solo existe UNA instancia en todo el servidor.
// Toda la lógica de autenticación (registro, login, reset de contraseña) vive aquí.
object AuthService {

    // [PSP - RA5.e] suspend fun register — función asíncrona de registro.
    // "suspend" significa que puede pausarse mientras espera a la BD sin bloquear otros requests.
    // dbQuery { } ejecuta el bloque en el pool de threads de I/O (Dispatchers.IO).
    suspend fun register(request: RegisterRequest): LoginResponse = dbQuery {
        // Verificar si el email o username ya existen
        val existing = Users
            .select { Users.email eq request.email or (Users.username eq request.username) }
            .firstOrNull()

        if (existing != null) {
            throw IllegalArgumentException("User with this email or username already exists")
        }

        // [PSP - RA5.e] AQUÍ SE CREA EL HASH DE LA CONTRASEÑA con BCrypt.
        // BCrypt.gensalt()  → genera un salt aleatorio (cadena de 29 chars como "$2a$10$...")
        //                     el salt hace que dos contraseñas iguales tengan hashes distintos
        // BCrypt.hashpw()   → calcula hash(password + salt) aplicado 2^10 = 1024 veces
        //                     resultado: cadena de 60 chars que incluye el salt y el hash
        // Equivalente Python: bcrypt.hashpw(password.encode(), bcrypt.gensalt())
        // NUNCA se guarda la contraseña en texto plano — solo el hash de 60 caracteres.
        val hashedPassword = BCrypt.hashpw(request.password, BCrypt.gensalt())
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val userId = UUID.randomUUID()
        val resolvedFullName = request.fullName?.takeIf { it.isNotBlank() } ?: request.username

        // Insertar el usuario en la BD — nótese que guardamos 'hashedPassword', no la contraseña
        Users.insert {
            it[id]           = userId
            it[email]        = request.email
            it[username]     = request.username
            it[fullName]     = resolvedFullName
            it[passwordHash] = hashedPassword   // ← el hash de 60 chars, no la contraseña
            it[createdAt]    = now
        }

        val user = User(
            id        = userId.toString(),
            email     = request.email,
            username  = request.username,
            fullName  = request.fullName,
            role      = UserRole.USER,
            createdAt = now
        )

        // [PSP - RA5.b] Al registrarse, el servidor emite inmediatamente un JWT firmado con HMAC-SHA256.
        // makeToken() está en Security.kt → JwtConfig.makeToken()
        // El token viaja al cliente Android que lo guarda en DataStore (almacenamiento seguro local).
        LoginResponse(
            token = JwtConfig.makeToken(request.email, userId.toString(), request.username),
            user  = user
        )
    }

    // [PSP - RA5.e] suspend fun login — función asíncrona de login.
    // La verificación de la contraseña se hace con BCrypt.checkpw() en la línea siguiente.
    suspend fun login(request: LoginRequest): LoginResponse? = dbQuery {
        // Buscar usuario por email en la BD
        val row = Users.select { Users.email eq request.email }.singleOrNull()

        // [PSP - RA5.e] AQUÍ SE VERIFICA EL HASH con BCrypt.
        // BCrypt.checkpw() extrae el salt del hash guardado en la BD,
        // rehashea la contraseña recibida con ese mismo salt,
        // y compara el resultado con el hash almacenado.
        // Si coinciden → contraseña correcta.
        // Equivalente Python: bcrypt.checkpw(password.encode(), stored_hash)
        // La contraseña original NO está en ningún lado — solo el hash.
        if (row != null && BCrypt.checkpw(request.password, row[Users.passwordHash])) {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            // Actualizar timestamp de último login (auditoría — RA5.d)
            Users.update({ Users.email eq request.email }) { it[lastLoginAt] = now }

            val user = User(
                id             = row[Users.id].toString(),
                email          = row[Users.email],
                username       = row[Users.username],
                fullName       = row[Users.fullName],
                // [PSP - RA5.d] El rol del usuario se lee de la BD y se incluye en la respuesta
                role           = row[Users.role],
                profileImageUrl = row[Users.profileImageUrl],
                isActive       = row[Users.isActive],
                createdAt      = row[Users.createdAt],
                lastLoginAt    = now
            )

            // [PSP - RA5.b] Login correcto → emitir un nuevo JWT firmado con HMAC-SHA256
            // Este token es lo que el cliente Android guardará y enviará en cada petición
            LoginResponse(
                token = JwtConfig.makeToken(row[Users.email], row[Users.id].toString(), row[Users.username]),
                user  = user
            )
        } else {
            // Credenciales incorrectas → devolvemos null → AuthRoutes responde 401 Unauthorized
            null
        }
    }

    suspend fun getUserById(userId: String): User? = dbQuery {
        Users.select { Users.id eq java.util.UUID.fromString(userId) }
            .singleOrNull()
            ?.let { row ->
                User(
                    id              = row[Users.id].toString(),
                    email           = row[Users.email],
                    username        = row[Users.username],
                    fullName        = row[Users.fullName],
                    role            = row[Users.role],
                    profileImageUrl = row[Users.profileImageUrl],
                    isActive        = row[Users.isActive],
                    createdAt       = row[Users.createdAt],
                    lastLoginAt     = row[Users.lastLoginAt]
                )
            }
    }

    suspend fun getUserByEmail(email: String): User? = dbQuery {
        Users.select { Users.email eq email }
            .singleOrNull()
            ?.let { row ->
                User(
                    id              = row[Users.id].toString(),
                    email           = row[Users.email],
                    username        = row[Users.username],
                    fullName        = row[Users.fullName],
                    role            = row[Users.role],
                    profileImageUrl = row[Users.profileImageUrl],
                    isActive        = row[Users.isActive],
                    createdAt       = row[Users.createdAt],
                    lastLoginAt     = row[Users.lastLoginAt]
                )
            }
    }

    suspend fun updateProfile(userId: String, fullName: String?, profileImageUrl: String?): User? = dbQuery {
        val uuid = java.util.UUID.fromString(userId)
        Users.update({ Users.id eq uuid }) { row ->
            fullName?.let { row[Users.fullName] = it }
            profileImageUrl?.let { row[Users.profileImageUrl] = it }
        }
        Users.select { Users.id eq uuid }.singleOrNull()?.let { row ->
            User(
                id              = row[Users.id].toString(),
                email           = row[Users.email],
                username        = row[Users.username],
                fullName        = row[Users.fullName],
                role            = row[Users.role],
                profileImageUrl = row[Users.profileImageUrl],
                isActive        = row[Users.isActive],
                createdAt       = row[Users.createdAt],
                lastLoginAt     = row[Users.lastLoginAt]
            )
        }
    }

    // [PSP - RA5.e] updatePassword — también usa BCrypt para verificar la contraseña actual
    // y para hashear la nueva antes de guardarla.
    suspend fun updatePassword(userId: String, oldPassword: String, newPassword: String): Boolean = dbQuery {
        val uuid = java.util.UUID.fromString(userId)
        val row  = Users.select { Users.id eq uuid }.singleOrNull() ?: return@dbQuery false
        // [PSP] Verificar contraseña actual con BCrypt antes de permitir el cambio
        if (!BCrypt.checkpw(oldPassword, row[Users.passwordHash])) return@dbQuery false
        // [PSP] Hashear la nueva contraseña antes de guardarla — igual que en el registro
        Users.update({ Users.id eq uuid }) { it[passwordHash] = BCrypt.hashpw(newPassword, BCrypt.gensalt()) }
        true
    }

    // [PSP] Google Sign-In — si el usuario es nuevo, se genera un password aleatorio y se hashea
    // (el usuario nunca lo sabe, pero la BD siempre tiene un hash válido en passwordHash)
    suspend fun googleSignIn(request: GoogleSignInRequest): LoginResponse = dbQuery {
        val now      = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val existing = Users.select { Users.email eq request.email }.singleOrNull()

        if (existing != null) {
            Users.update({ Users.email eq request.email }) { it[lastLoginAt] = now }
            val finalUsername = existing[Users.username]
            val user = User(
                id              = existing[Users.id].toString(),
                email           = existing[Users.email],
                username        = finalUsername,
                fullName        = existing[Users.fullName],
                role            = existing[Users.role],
                profileImageUrl = request.profilePictureUrl ?: existing[Users.profileImageUrl],
                isActive        = existing[Users.isActive],
                createdAt       = existing[Users.createdAt],
                lastLoginAt     = now
            )
            LoginResponse(
                token = JwtConfig.makeToken(request.email, existing[Users.id].toString(), finalUsername),
                user  = user
            )
        } else {
            val userId       = UUID.randomUUID()
            val baseUsername = request.email.substringBefore("@").replace(Regex("[^a-zA-Z0-9_]"), "_")
            val finalUsername = if (Users.select { Users.username eq baseUsername }.count() == 0L)
                baseUsername
            else
                "${baseUsername}_${userId.toString().take(4)}"

            Users.insert {
                it[id]           = userId
                it[email]        = request.email
                it[username]     = finalUsername
                it[fullName]     = request.displayName ?: request.email.substringBefore("@")
                // [PSP] Para usuarios de Google no hay contraseña → se hashea un UUID aleatorio
                // Así passwordHash nunca está vacío pero nadie puede hacer login con contraseña
                it[passwordHash] = BCrypt.hashpw(UUID.randomUUID().toString(), BCrypt.gensalt())
                it[profileImageUrl] = request.profilePictureUrl
                it[createdAt]    = now
                it[lastLoginAt]  = now
            }
            val user = User(
                id              = userId.toString(),
                email           = request.email,
                username        = finalUsername,
                fullName        = request.displayName ?: request.email.substringBefore("@"),
                role            = UserRole.USER,
                profileImageUrl = request.profilePictureUrl,
                createdAt       = now,
                lastLoginAt     = now
            )
            LoginResponse(
                token = JwtConfig.makeToken(request.email, userId.toString(), finalUsername),
                user  = user
            )
        }
    }

    // [PSP - RA5.a] requestPasswordReset — principio de programación segura: anti-enumeración.
    // Siempre devuelve true (HTTP 200), tanto si el email existe como si no.
    // Así un atacante no puede saber qué emails están registrados probando este endpoint.
    // Documentado en Memoria §5.2 y verificado en TC-11.
    suspend fun requestPasswordReset(email: String): Boolean = dbQuery {
        val now     = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val expires = Clock.System.now().plus(15, DateTimeUnit.MINUTE).toLocalDateTime(TimeZone.UTC)

        val userExists = Users.select { Users.email eq email }.count() > 0

        if (userExists) {
            // Invalida tokens anteriores para este email
            PasswordResetTokens.update({ PasswordResetTokens.email eq email }) {
                it[PasswordResetTokens.used] = true
            }

            // [PSP - RA5.e] El OTP es un número aleatorio de 6 dígitos.
            // Se almacena en texto plano (no en hash) porque necesitamos compararlo directamente.
            // Caduca en 15 minutos y no se puede reutilizar (used = true tras usarse).
            val code = (100000..999999).random().toString()

            PasswordResetTokens.insert {
                it[PasswordResetTokens.id]        = UUID.randomUUID()
                it[PasswordResetTokens.email]     = email
                it[PasswordResetTokens.code]      = code
                it[PasswordResetTokens.expiresAt] = expires
                it[PasswordResetTokens.used]      = false
            }

            // Enviar email fuera de la transacción (fire-and-forget con coroutine)
            GlobalScope.launch {
                EmailService.sendPasswordResetEmail(email, code)
            }
        }

        true  // siempre 200, independientemente de si el email existe (anti-enumeración)
    }

    // [PSP - RA5.e] confirmPasswordReset — verifica el OTP y actualiza el hash de la contraseña.
    suspend fun confirmPasswordReset(email: String, code: String, newPassword: String): Boolean = dbQuery {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        val token = PasswordResetTokens
            .select {
                (PasswordResetTokens.email eq email) and
                (PasswordResetTokens.code  eq code)  and
                (PasswordResetTokens.used  eq false)  // token no usado
            }
            .singleOrNull()

        // Rechazar si el token no existe o ha expirado (documentado en TC-12 y TC-13)
        if (token == null || token[PasswordResetTokens.expiresAt] < now) {
            return@dbQuery false
        }

        // Marcar el token como usado — no se puede reutilizar
        PasswordResetTokens.update({
            (PasswordResetTokens.email eq email) and (PasswordResetTokens.code eq code)
        }) {
            it[PasswordResetTokens.used] = true
        }

        // [PSP - RA5.e] Actualizar la contraseña: volver a hashear con BCrypt antes de guardar
        Users.update({ Users.email eq email }) {
            it[Users.passwordHash] = BCrypt.hashpw(newPassword, BCrypt.gensalt())
        }

        true
    }
}
