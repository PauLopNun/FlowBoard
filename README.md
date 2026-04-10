# FlowBoard

Collaborative document editor and task management platform for Android.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
[![Ktor](https://img.shields.io/badge/Backend-Ktor-orange.svg)](https://ktor.io)
[![WebSockets](https://img.shields.io/badge/Real--Time-WebSockets-brightgreen.svg)](https://ktor.io/docs/websocket.html)
[![Material3](https://img.shields.io/badge/UI-Material%203-purple.svg)](https://m3.material.io)
[![Status](https://img.shields.io/badge/Status-Production%20Ready-success.svg)]()

## Overview

FlowBoard is an Android application that lets users create and edit documents collaboratively in real time, manage tasks, and share workspaces with teammates. The backend is deployed on Render and serves a REST API and WebSocket connections.

**Live backend:** `https://flowboard-api-phrk.onrender.com`

## Features

- **Authentication** — Email/password registration and login with JWT. Google Sign-In via Credential Manager available on both login and registration screens.
- **Collaborative documents** — Block-based editor (Notion-style) with CRDT synchronization over WebSockets. Multiple users see each other's changes instantly. Markdown shortcuts supported. Export to PDF or Markdown.
- **Document sharing** — Share any document with a registered user by email. The recipient receives an in-app notification and an email (via Resend).
- **Task management** — Full CRUD with priorities (Low/Medium/High/Urgent), due dates, and completion tracking.
- **Chat** — Direct messages and group chats with real-time delivery, unread counts, archive, and mute.
- **Permissions** — Owner, editor, and viewer roles per document.
- **Notifications** — In-app notification feed with mark-read and delete. Email delivery via Resend.

## Tech Stack

### Android
| Component | Library |
|---|---|
| Language | Kotlin 1.9 |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt |
| Networking | Ktor Client |
| Local DB | Room / SQLite |
| Auth | Google Credential Manager |
| State | StateFlow |

### Backend
| Component | Library |
|---|---|
| Framework | Ktor 2.3.7 |
| Language | Kotlin |
| Database | PostgreSQL (Render) |
| ORM | Exposed |
| Auth | JWT (HMAC256) |
| Serialization | kotlinx.serialization |
| Email | Resend API |

## Architecture

```
Android App
├── Presentation (Compose screens + ViewModels)
├── Domain (repositories, use cases)
└── Data
    ├── Local  (Room)
    ├── Remote (Ktor Client → REST API)
    └── WebSocket (real-time document sync)

Backend (Ktor on Render)
├── REST API  (/api/v1/*)
├── WebSocket (/ws/*, /document-ws/*)
├── PostgreSQL (Render managed DB)
└── Resend    (transactional email)
```

## REST API

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/auth/register` | Register new user |
| POST | `/api/v1/auth/login` | Login, returns JWT |
| POST | `/api/v1/auth/google` | Google Sign-In |
| GET | `/api/v1/users/me` | Current user profile |
| PUT | `/api/v1/users/me` | Update profile |
| GET | `/api/v1/users/search?email=` | Find user by email |
| GET | `/api/v1/documents` | List owned + shared documents |
| POST | `/api/v1/documents` | Create document |
| GET | `/api/v1/documents/{id}` | Get document |
| PUT | `/api/v1/documents/{id}` | Update document |
| DELETE | `/api/v1/documents/{id}` | Delete document |
| POST | `/api/v1/documents/{id}/share` | Share with user by email |
| GET | `/api/v1/tasks` | List tasks |
| POST | `/api/v1/tasks` | Create task |
| PUT | `/api/v1/tasks/{id}` | Update task |
| DELETE | `/api/v1/tasks/{id}` | Delete task |
| PATCH | `/api/v1/tasks/{id}/toggle` | Toggle completion |
| GET | `/api/v1/chat/rooms` | List chat rooms |
| POST | `/api/v1/chat/rooms` | Create chat room (direct or group) |
| GET | `/api/v1/chat/rooms/{id}/messages` | Get messages |
| POST | `/api/v1/chat/rooms/{id}/messages` | Send message |
| GET | `/api/v1/notifications` | List notifications |
| PATCH | `/api/v1/notifications/{id}/read` | Mark as read |
| PATCH | `/api/v1/notifications/read-all` | Mark all as read |

All endpoints except `/auth/*` require `Authorization: Bearer <token>`.

## Project Structure

```
FlowBoard/
├── android/
│   └── app/src/main/java/com/flowboard/
│       ├── data/
│       │   ├── auth/          # Google Sign-In
│       │   ├── local/         # Room entities, DAOs
│       │   ├── remote/        # API services, DTOs
│       │   └── repository/    # AuthRepository, etc.
│       ├── di/                # Hilt modules
│       └── presentation/
│           ├── ui/screens/    # Compose screens
│           ├── ui/components/ # Shared composables
│           └── viewmodel/     # ViewModels
└── backend/
    └── src/main/kotlin/com/flowboard/
        ├── data/
        │   ├── database/      # Exposed table definitions
        │   └── models/        # Request/response models
        ├── domain/            # Business logic services
        ├── plugins/           # Ktor plugins (auth, routing, serialization)
        └── routes/            # API route handlers
```

## Getting Started

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 17+

### Build the Android app

1. Clone the repository:
   ```bash
   git clone https://github.com/PauLopNun/FlowBoard.git
   ```

2. Open `FlowBoard/android` in Android Studio.

3. Run on a device or emulator (Shift+F10).

The app connects to the production backend by default. No local backend setup is required.

### Run the backend locally (optional)

```bash
cd backend

export DATABASE_URL="jdbc:postgresql://localhost:5432/flowboard"
export DATABASE_USER="postgres"
export DATABASE_PASSWORD="your_password"
export JWT_SECRET="your_secret"
export JWT_ISSUER="flowboard-api"
export JWT_AUDIENCE="flowboard-app"
export RESEND_API_KEY="re_your_key"   # optional, for emails

./gradlew run
```

### Backend environment variables (Render)

| Variable | Description |
|---|---|
| `DATABASE_URL` | PostgreSQL connection string |
| `DATABASE_USER` | DB username |
| `DATABASE_PASSWORD` | DB password |
| `JWT_SECRET` | HMAC256 signing secret |
| `JWT_ISSUER` | Token issuer (e.g. `flowboard-api`) |
| `JWT_AUDIENCE` | Token audience (e.g. `flowboard-app`) |
| `RESEND_API_KEY` | Resend API key for email notifications |

### Google Sign-In setup

To enable Google Sign-In:

1. Create an Android OAuth credential in Google Cloud Console:
   - Package name: `com.flowboard`
   - SHA-1: your debug/release keystore fingerprint
2. Create a Web OAuth credential and copy the client ID.
3. Update `webClientId` in `GoogleAuthManager.kt`.
4. Enable the **Identity Toolkit API** in the Google Cloud project.

## Contact

**Pau López Núñez**
- Email: paulopeznunez@gmail.com
- LinkedIn: [paulopnun](https://www.linkedin.com/in/paulopnun)
- GitHub: [@PauLopNun](https://github.com/PauLopNun)
