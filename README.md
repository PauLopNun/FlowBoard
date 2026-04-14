# FlowBoard

Collaborative document editor and task management platform for Android.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
[![Ktor](https://img.shields.io/badge/Backend-Ktor-orange.svg)](https://ktor.io)
[![WebSockets](https://img.shields.io/badge/Real--Time-WebSockets-brightgreen.svg)](https://ktor.io/docs/websocket.html)
[![Material3](https://img.shields.io/badge/UI-Material%203-purple.svg)](https://m3.material.io)
[![Build APK](https://github.com/PauLopNun/FlowBoard/actions/workflows/build-apk.yml/badge.svg)](https://github.com/PauLopNun/FlowBoard/actions/workflows/build-apk.yml)

## Overview

FlowBoard is an Android + Kotlin backend application inspired by Notion. Users can create and edit documents collaboratively in real time using a block-based CRDT editor, manage tasks and events, chat with teammates, and organize work into shared workspaces. The backend is deployed on Render and exposes a REST API plus WebSocket connections.

**Live backend:** `https://flowboard-api-phrk.onrender.com`

---

## Features

### Authentication
- Email / password register and login with JWT
- Google Sign-In via Android Credential Manager (login + register)
- Forgot password via 6-digit OTP delivered by email (Resend)

### Collaborative Document Editor
- Block-based Notion-style editor (CRDT over WebSockets, multi-user real time)
- **11 block types:** Heading 1/2/3, Paragraph, Bullet list, Numbered list, To-do (checkbox), Quote, Callout, Code block, Divider
- Slash command menu (`/`) to insert any block type
- Markdown shortcuts: `# `, `## `, `### `, `- `, `1. `, `` ``` ``, `> `, `[] `
- Formatting toolbar: Bold, Italic, Underline, block-type chips
- **Page emoji** — tappable icon above the title with a 30-emoji picker
- **Word counter** — live word count shown when a block is focused
- **Typing indicator** — animated "Alice is editing…" banner when collaborators are present
- Breadcrumb trail (parent hierarchy)
- Export to **Markdown** or **PDF**
- Share document with any registered user by email (viewer / editor role)
- Sub-pages: create nested pages directly from the editor
- Auto-save (5 s debounce after last change)
- Offline-tolerant: HTTP save fallback when WebSocket is disconnected

### Dashboard
- **Jump back in** — horizontal row of 5 most-recently-updated pages
- Search across all documents
- Inbox (shared-with-me)
- Sidebar page tree (root pages with expandable children)

### Task Management
- Full CRUD with priorities (Low / Medium / High / Urgent)
- Due dates, assignees, tags
- Toggle completion with a single tap
- Calendar view — monthly grid with task dots per day

### Chat
- Direct messages and group chats
- Real-time delivery, unread badge counts
- Edit and delete own messages
- Add / remove participants (owner/admin only)
- Archive and mute rooms

### Workspaces
- Create shared team workspaces
- Join by 12-character invite code
- Member list with roles (OWNER / ADMIN / MEMBER)
- Remove members

### Projects
- Group tasks by project (name, description, color, deadline)
- Project CRUD, member list

### Notifications
- In-app notification feed with mark-as-read and delete
- Email delivery via Resend for document shares, task assignments, etc.

---

## Tech Stack

### Android
| Component | Library |
|---|---|
| Language | Kotlin 1.9 |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt |
| Networking | Ktor Client |
| Local DB | Room (SQLite) |
| Auth | Google Credential Manager |
| State | StateFlow / collectAsStateWithLifecycle |
| Real-time | WebSocket (Ktor) + CRDT engine |

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
| Connection pool | HikariCP |

---

## Architecture

```
Android App
├── Presentation  (Compose screens + ViewModels)
├── Domain        (repositories, CRDT engine)
└── Data
    ├── Local     (Room — documents, tasks, workspaces, etc.)
    ├── Remote    (Ktor Client → REST API)
    └── WebSocket (real-time document sync + cursor presence)

Backend (Ktor on Render)
├── REST API   (/api/v1/*)
├── WebSocket  (/ws/*, /document-ws/*)
├── PostgreSQL (Render managed DB)
└── Resend     (transactional email)
```

---

## REST API

All endpoints except `/auth/*` require `Authorization: Bearer <token>`.

### Auth
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/auth/register` | Register (email + password) |
| POST | `/api/v1/auth/login` | Login, returns JWT |
| POST | `/api/v1/auth/google` | Google Sign-In |
| POST | `/api/v1/auth/forgot-password` | Send OTP to email |
| POST | `/api/v1/auth/reset-password` | Verify OTP and set new password |

### Users
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/users/me` | Current user profile |
| PUT | `/api/v1/users/me` | Update profile |
| GET | `/api/v1/users/search?email=` | Find user by email |

### Documents
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/documents` | List owned + shared documents |
| POST | `/api/v1/documents` | Create document |
| GET | `/api/v1/documents/{id}` | Get document |
| PUT | `/api/v1/documents/{id}` | Update document |
| DELETE | `/api/v1/documents/{id}` | Delete document |
| GET | `/api/v1/documents/{id}/children` | List sub-pages |
| POST | `/api/v1/documents/{id}/share` | Share with user by email (role: viewer/editor) |

### Tasks
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/tasks` | List tasks |
| POST | `/api/v1/tasks` | Create task |
| GET | `/api/v1/tasks/{id}` | Get task |
| PUT | `/api/v1/tasks/{id}` | Update task |
| DELETE | `/api/v1/tasks/{id}` | Delete task |
| PATCH | `/api/v1/tasks/{id}/toggle` | Toggle completion |

### Projects
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/projects` | List projects |
| POST | `/api/v1/projects` | Create project |
| PUT | `/api/v1/projects/{id}` | Update project |
| DELETE | `/api/v1/projects/{id}` | Delete project |

### Chat
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/chat/rooms` | List chat rooms |
| POST | `/api/v1/chat/rooms` | Create room (DIRECT / GROUP) |
| GET | `/api/v1/chat/rooms/{id}` | Get room detail + unread count |
| GET | `/api/v1/chat/rooms/{id}/messages` | Get messages (limit, offset) |
| POST | `/api/v1/chat/rooms/{id}/messages` | Send message |
| PUT | `/api/v1/chat/messages/{id}` | Edit own message |
| DELETE | `/api/v1/chat/messages/{id}` | Delete own message |
| POST | `/api/v1/chat/rooms/{id}/participants` | Add participant |
| DELETE | `/api/v1/chat/rooms/{id}/participants/{userId}` | Remove participant |

### Workspaces
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/workspaces` | List my workspaces |
| POST | `/api/v1/workspaces` | Create workspace |
| GET | `/api/v1/workspaces/{id}` | Get workspace |
| DELETE | `/api/v1/workspaces/{id}` | Delete workspace (owner only) |
| POST | `/api/v1/workspaces/join` | Join by invite code |
| DELETE | `/api/v1/workspaces/{id}/members/{userId}` | Remove member |

### Notifications
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/notifications` | List notifications |
| GET | `/api/v1/notifications/stats` | Unread count |
| PATCH | `/api/v1/notifications/{id}/read` | Mark as read |
| PATCH | `/api/v1/notifications/read-all` | Mark all as read |
| DELETE | `/api/v1/notifications/{id}` | Delete notification |

### WebSocket
| Endpoint | Description |
|---|---|
| `/ws/{documentId}?token=` | General document WebSocket |
| `/document-ws/{documentId}?token=` | CRDT document sync (operations + cursor presence) |

---

## Project Structure

```
FlowBoard/
├── .github/workflows/
│   └── build-apk.yml          # CI: builds debug APK + publishes GitHub Release on every push to master
├── android/
│   └── app/src/main/java/com/flowboard/
│       ├── data/
│       │   ├── auth/           # Google Sign-In (Credential Manager)
│       │   ├── crdt/           # CRDT engine (OT transforms)
│       │   ├── local/          # Room entities + DAOs
│       │   ├── models/         # Domain models, WebSocket messages
│       │   ├── remote/         # Ktor API services + DTOs
│       │   └── repository/     # Repository implementations
│       ├── di/                 # Hilt modules (network, database, auth)
│       └── presentation/
│           ├── ui/
│           │   ├── components/ # Shared composables (UserAvatar, ConnectionBanner, ShareDialog…)
│           │   ├── screens/
│           │   │   ├── auth/       # Login, Register, ForgotPassword
│           │   │   ├── chat/       # ChatList, ChatScreen, CreateChatDialog
│           │   │   ├── dashboard/  # DashboardScreen (home, search, inbox, sidebar)
│           │   │   ├── documents/  # CollaborativeDocumentScreenV2, MyDocuments, DocumentExporter
│           │   │   ├── notifications/
│           │   │   ├── profile/
│           │   │   ├── settings/
│           │   │   ├── tasks/      # TaskList, TaskDetail, CreateTask, CalendarScreen
│           │   │   └── workspace/  # WorkspaceScreen
│           │   └── theme/
│           └── viewmodel/      # One ViewModel per feature
└── backend/
    └── src/main/kotlin/com/flowboard/
        ├── data/
        │   ├── database/       # Exposed table definitions (13 tables)
        │   └── models/         # Request/response models, CRDT models
        ├── domain/             # Business logic services
        ├── plugins/            # Ktor plugins (auth, routing, serialization, CORS)
        └── routes/             # Route handlers (one file per domain)
```

---

## Getting Started

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 17+

### Build the Android app

```bash
git clone https://github.com/PauLopNun/FlowBoard.git
```

Open `FlowBoard/android` in Android Studio and run on a device or emulator (Shift+F10).

The app connects to the production backend by default — no local setup needed.

### Download the latest APK

Every push to `master` triggers a GitHub Actions build. The resulting APK is attached as a release artifact:

1. Go to [Releases](https://github.com/PauLopNun/FlowBoard/releases)
2. Download `app-debug.apk`
3. Enable **Install from unknown sources** on your Android device and open the file

### Run the backend locally (optional)

```bash
cd backend

export DATABASE_URL="jdbc:postgresql://localhost:5432/flowboard"
export DATABASE_USER="postgres"
export DATABASE_PASSWORD="your_password"
export JWT_SECRET="your_secret"
export JWT_ISSUER="flowboard-api"
export JWT_AUDIENCE="flowboard-app"
export RESEND_API_KEY="re_your_key"   # optional — needed for email features

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
| `RESEND_API_KEY` | Resend API key for email notifications and password reset |

### Google Sign-In setup

1. Create an Android OAuth credential in Google Cloud Console:
   - Package name: `com.flowboard`
   - SHA-1: your debug/release keystore fingerprint
2. Create a Web OAuth credential and copy the client ID
3. Update `webClientId` in `GoogleAuthManager.kt`
4. Enable the **Identity Toolkit API** in the Google Cloud project

---

## CI / CD

The GitHub Actions workflow (`.github/workflows/build-apk.yml`) runs on every push to `master`:

1. Sets up JDK 17 + Android SDK
2. Caches Gradle dependencies
3. Builds the debug APK (`./gradlew assembleDebug`)
4. Uploads it as an artifact (30-day retention)
5. Creates a GitHub Release tagged `build-<run_number>` with the APK attached

---

## Contact

**Pau López Núñez**
- Email: paulopeznunez@gmail.com
- LinkedIn: [paulopnun](https://www.linkedin.com/in/paulopnun)
- GitHub: [@PauLopNun](https://github.com/PauLopNun)
