# Contribuir a FlowBoard 📋

¡Gracias por tu interés en contribuir a FlowBoard! Este documento te guiará para hacer contribuciones efectivas al proyecto.

## 📋 Código de Conducta

Al participar en este proyecto, te comprometes a mantener un ambiente respetuoso y colaborativo. Se amable, profesional y constructivo en todas las interacciones.

## 🚀 Cómo Contribuir

### 1. Reportar Bugs 🐛

Si encuentras un bug:
- Busca en [Issues](https://github.com/tu-usuario/flowboard/issues) existentes
- Si no existe, crea un nuevo issue con:
  - Descripción clara del problema
  - Pasos para reproducir
  - Comportamiento esperado vs actual
  - Screenshots si es relevante
  - Información del dispositivo/sistema

### 2. Sugerir Funcionalidades 💡

Para nuevas características:
- Crea un issue con etiqueta "enhancement"
- Describe claramente la funcionalidad
- Explica por qué sería útil
- Considera la implementación y impacto

### 3. Contribuir Código 💻

#### Setup del Entorno de Desarrollo
```bash
# Clonar el repo
git clone https://github.com/tu-usuario/flowboard.git
cd flowboard

# Setup backend
cd backend
./gradlew build

# Setup Android
cd ../android
./gradlew build
```

#### Proceso de Desarrollo
1. **Fork** el repositorio
2. **Clona** tu fork localmente
3. **Crea** una rama para tu feature:
   ```bash
   git checkout -b feature/nueva-funcionalidad
   ```
4. **Desarrolla** siguiendo las convenciones
5. **Testea** tu código
6. **Commit** con mensajes descriptivos
7. **Push** a tu fork
8. **Crea** un Pull Request

### 4. Estándares de Código

#### Kotlin/Android
- Usar [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Documentar funciones públicas con KDoc
- Seguir Clean Architecture patterns
- Tests unitarios para nueva funcionalidad

#### Backend
- Seguir patrones REST
- Validar todos los inputs
- Manejar errores apropiadamente
- Documentar endpoints

#### Commits
```
tipo(alcance): descripción corta

Descripción más detallada si es necesario

Fixes #123
```

Tipos: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`

## 🧪 Testing

### Android
```bash
cd android
./gradlew test           # Unit tests
./gradlew connectedCheck # Instrumented tests
```

### Backend
```bash
cd backend
./gradlew test
```

## 📚 Estructura del Proyecto

```
FlowBoard/
├── android/          # App Android
├── backend/          # API Ktor
├── docs/            # Documentación
└── README.md        # Documentación principal
```

## 🏗️ Arquitectura

### Android
- **Clean Architecture** con capas Domain/Data/Presentation
- **MVVM** con ViewModels y StateFlow
- **Jetpack Compose** para UI
- **Hilt** para DI

### Backend
- **Ktor** framework
- **Exposed** ORM
- **PostgreSQL** database
- **JWT** authentication

## 📋 Roadmap

Ver [GitHub Projects](https://github.com/tu-usuario/flowboard/projects) para el roadmap actual.

## ❓ Preguntas

- 💬 **Discusiones**: [GitHub Discussions](https://github.com/tu-usuario/flowboard/discussions)
- 📧 **Email**: dev@flowboard.com
- 🐛 **Bugs**: [GitHub Issues](https://github.com/tu-usuario/flowboard/issues)

## 🏆 Reconocimientos

Los contribuidores serán reconocidos en:
- README principal
- Release notes
- Hall of Fame (futuro)

¡Gracias por ayudar a hacer FlowBoard mejor! 🚀