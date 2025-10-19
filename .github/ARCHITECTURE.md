# 🏗️ Decisiones de Arquitectura - FlowBoard

## 📝 Documento de Registro de Decisiones de Arquitectura (ADR)

Este documento explica las decisiones arquitectónicas clave del proyecto FlowBoard.

---

## ADR-001: Estructura Monorepo con Gradle Composite Build

### Contexto

FlowBoard es un proyecto full-stack que incluye:
- Frontend: Aplicación Android nativa (Kotlin + Jetpack Compose)
- Backend: API REST (Ktor + PostgreSQL)

Necesitábamos decidir cómo organizar estos dos proyectos relacionados pero independientes.

### Decisión

**Adoptamos una arquitectura monorepo con Gradle Composite Build**, donde:

```
FlowBoard/
├── android/          # Proyecto Android completo e independiente
├── backend/          # Proyecto Ktor completo e independiente
├── docs/             # Documentación compartida
└── [archivos raíz]   # Configuración de composite build
```

### Alternativas Consideradas

1. **Multi-repo (repositorios separados)**
   - ❌ Dificultad para mantener versiones sincronizadas
   - ❌ Más complejo para contribuidores
   - ✅ Deployments completamente independientes

2. **Monorepo con módulos Gradle tradicionales**
   - ❌ Mezcla configuraciones Android y backend
   - ❌ Android Studio intentaría compilar backend como módulo Android
   - ❌ Problemas de performance al cargar todo

3. **Monorepo con Composite Build** ✅
   - ✅ Proyectos verdaderamente independientes
   - ✅ Cada uno puede abrirse por separado
   - ✅ También pueden trabajar juntos desde la raíz
   - ✅ Flexibilidad total para desarrolladores

### Consecuencias

**Positivas:**
- ✅ Frontend y backend son proyectos autocontenidos
- ✅ Desarrolladores pueden abrir solo lo que necesiten
- ✅ Gradle wrapper en cada proyecto + raíz para conveniencia
- ✅ Configuraciones de build independientes
- ✅ Fácil de escalar (agregar `ios/`, `web/`, etc.)
- ✅ CI/CD puede compilar módulos por separado
- ✅ Documentación centralizada

**Negativas:**
- ⚠️ Tres sets de archivos Gradle (raíz, android, backend)
- ⚠️ Desarrolladores deben entender la estructura al inicio
- ⚠️ Scripts de utilidades necesarios para facilitar uso

**Mitigaciones:**
- 📝 Documentación clara ([QUICK_START.md](../QUICK_START.md), [DEVELOPMENT.md](../DEVELOPMENT.md))
- 🛠️ Scripts de utilidades (`flow.sh`, `flow.bat`)
- 📂 Estructura estándar (similar a React Native, Flutter)

### Estado

✅ **Aceptada e Implementada** (2025-01-19)

---

## ADR-002: Offline-First para la App Android

### Contexto

La app Android necesita funcionar en entornos con conectividad limitada (universidades, transporte público, etc.).

### Decisión

**Implementar arquitectura Offline-First** usando:
- Room Database como fuente de verdad (Single Source of Truth)
- Backend opcional solo para sincronización
- Sincronización bidireccional cuando hay conectividad

### Consecuencias

**Positivas:**
- ✅ App funciona sin backend (excelente para desarrollo)
- ✅ Mejor experiencia de usuario (sin esperas)
- ✅ Resistente a fallos de red
- ✅ Frontend y backend pueden desarrollarse independientemente

**Negativas:**
- ⚠️ Lógica de sincronización compleja (resolución de conflictos)
- ⚠️ Mayor almacenamiento local necesario

### Estado

✅ **Implementada**

---

## ADR-003: Clean Architecture + MVVM

### Contexto

Necesitamos una arquitectura escalable, testable y mantenible para una app que crecerá en funcionalidades.

### Decisión

**Adoptar Clean Architecture con patrón MVVM:**

```
presentation/  → UI (Compose) + ViewModels
domain/        → Casos de uso + Modelos de dominio + Interfaces
data/          → Implementaciones (Room, Retrofit, Repositories)
di/            → Inyección de dependencias (Hilt)
```

### Consecuencias

**Positivas:**
- ✅ Separación clara de responsabilidades
- ✅ Altamente testable (mockeamos interfaces)
- ✅ Independencia de frameworks
- ✅ Fácil onboarding para nuevos desarrolladores

**Negativas:**
- ⚠️ Más archivos y boilerplate inicial
- ⚠️ Curva de aprendizaje para juniors

### Estado

✅ **Implementada**

---

## ADR-004: Gradle Composite Build en Raíz

### Contexto

Queremos permitir que desarrolladores puedan:
1. Abrir solo `android/` en Android Studio (rápido, enfocado)
2. Abrir toda la raíz `FlowBoard/` para tener visibilidad completa
3. Ejecutar comandos desde la raíz con `-p android` o `-p backend`

### Decisión

**Configurar Gradle Composite Build en la raíz** con:
- `settings.gradle.kts` que incluye `includeBuild("android")`
- `build.gradle.kts` con tareas de conveniencia
- Gradle wrapper copiado desde `android/` a raíz
- Scripts de utilidades (`flow.sh`, `flow.bat`)

### Consecuencias

**Positivas:**
- ✅ Máxima flexibilidad de desarrollo
- ✅ Comandos unificados desde raíz
- ✅ No interferencia entre Android y backend
- ✅ Compatible con IntelliJ IDEA y Android Studio

**Negativas:**
- ⚠️ Duplicación de gradle wrapper (raíz + android/)
- ⚠️ Requiere documentación clara

### Estado

✅ **Implementada**

---

## ADR-005: Scripts de Utilidades Cross-Platform

### Contexto

Los desarrolladores necesitan ejecutar tareas comunes rápidamente sin memorizar comandos largos de Gradle.

### Decisión

**Crear scripts de utilidades:**
- `flow.sh` para Linux/Mac
- `flow.bat` para Windows

Con comandos simples:
```bash
flow build    # Compilar
flow run      # Instalar y ejecutar
flow test     # Tests
flow backend  # Iniciar backend
flow clean    # Limpiar
```

### Consecuencias

**Positivas:**
- ✅ Developer experience mejorada
- ✅ Onboarding más rápido
- ✅ Menos errores al ejecutar comandos

**Negativas:**
- ⚠️ Mantenimiento de dos versiones (sh + bat)
- ⚠️ Puede ocultar complejidad a nuevos devs

### Estado

✅ **Implementada**

---

## Futuras Decisiones Pendientes

### En Consideración

- **ADR-006**: ¿Adoptar Kotlin Multiplatform Mobile (KMM)?
  - Permitiría compartir lógica entre Android e iOS
  - Requiere evaluación de madurez del ecosistema

- **ADR-007**: ¿Implementar GraphQL en lugar de REST?
  - Beneficios: Menos over-fetching, tipado fuerte
  - Contras: Complejidad adicional

- **ADR-008**: ¿Migrar a Jetpack Compose Multiplatform?
  - Permitiría reutilizar UI en Desktop/Web
  - Aún en experimental

---

## Principios de Diseño

1. **Separation of Concerns**: Cada módulo/capa tiene responsabilidad única
2. **Offline-First**: La app debe funcionar sin backend
3. **Developer Experience**: Priorizar facilidad de desarrollo
4. **Escalabilidad**: Diseñar para crecimiento (más plataformas, features)
5. **Estándares de Industria**: Seguir patrones probados (Clean Arch, MVVM)
6. **Documentación Clara**: Código autodocumentado + docs completas

---

## Referencias

- [Project Structure](../PROJECT_STRUCTURE.md)
- [Development Guide](../DEVELOPMENT.md)
- [Quick Start](../QUICK_START.md)
- [Android Architecture Guide](https://developer.android.com/topic/architecture)
- [Ktor Documentation](https://ktor.io/docs)
- [Gradle Composite Builds](https://docs.gradle.org/current/userguide/composite_builds.html)

---

**Última actualización:** 2025-01-19
**Mantenedores:** [@PauLopNun](https://github.com/PauLopNun)
