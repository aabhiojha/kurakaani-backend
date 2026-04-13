# Kurakaani Backend

A real-time chat application backend built with Spring Boot 3. Supports direct messaging, group chat, friend requests, media uploads, and live typing indicators — all over WebSocket with STOMP and Redis pub/sub.

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Environment Variables](#environment-variables)
  - [Running Locally](#running-locally)
- [Architecture](#architecture)
- [Authentication](#authentication)
- [REST API](#rest-api)
  - [Auth](#auth)
  - [Users](#users)
  - [Rooms](#rooms)
  - [Messages](#messages)
  - [Friends](#friends)
- [WebSocket API](#websocket-api)
  - [Connecting](#connecting)
  - [Sending Messages](#sending-messages)
  - [Typing Indicators](#typing-indicators)
  - [Subscriptions](#subscriptions)
- [Error Handling](#error-handling)
- [Project Structure](#project-structure)
- [API Docs (Swagger)](#api-docs-swagger)

---

## Overview

**Kurakaani** (Nepali for "conversation") is a monolithic Spring Boot chat backend. It provides:

- JWT-based stateless authentication
- 1-to-1 direct messaging (DM) and group rooms
- Real-time messaging and typing indicators over WebSocket (STOMP + SockJS)
- Redis pub/sub for scalable message fan-out
- A friend request system (send, accept, reject, cancel, unfriend)
- Media upload (images, videos) to S3-compatible storage (MinIO)
- Full-text message search within and across rooms
- Password reset via email

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.5, Java 21 |
| Database | PostgreSQL + Flyway |
| Cache / Pub-Sub | Redis |
| Real-time | WebSocket, STOMP, SockJS |
| Auth | JWT (jjwt 0.13) + Spring Security |
| Storage | AWS S3 SDK v2 (MinIO-compatible) |
| Email | Spring Mail (SMTP / Gmail) |
| Docs | SpringDoc OpenAPI (Swagger UI) |
| Build | Maven |
| Mapping | MapStruct |

---

## Getting Started

### Prerequisites

- Java 21+
- PostgreSQL 14+
- Redis 7+
- MinIO (or any S3-compatible store) — required only for media uploads
- An SMTP account — required only for password reset emails

### Environment Variables

| Variable | Description | Default |
|---|---|---|
| `JWT_SECRET` | Base64-encoded HMAC-SHA256 secret | (hardcoded dev default) |
| `JWT_EXPIRATION_SECONDS` | Token TTL in seconds | `864000` (10 days) |
| `MAIL_USERNAME` | SMTP sender address | `aojha6822@gmail.com` |
| `MAIL_PASSWORD` | SMTP password / app-password | — |
| `STORAGE_ENDPOINT` | S3/MinIO endpoint URL | `http://127.0.0.1:9000` |
| `STORAGE_BUCKET` | Bucket name | `kurakaani` |
| `STORAGE_REGION` | Bucket region | `rustfs` |
| `STORAGE_ACCESS_KEY` | S3 access key | — |
| `STORAGE_SECRET_KEY` | S3 secret key | — |

Database connection is configured in `application-dev.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/chat-app-db
    username: postgres
    password: postgres
```

Override these via environment variables or a local `application-local.yaml`.

### Running Locally

```bash
# Clone the repo
git clone https://github.com/<your-org>/kurakaani-backend.git
cd kurakaani-backend

# Start dependencies (requires Docker)
docker compose up -d   # or start PostgreSQL, Redis, MinIO manually

# Build and run with the dev profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

The server starts on `http://localhost:8080`.  
Swagger UI is available at `http://localhost:8080/docs`.

---

## Architecture

```
Client (browser / mobile)
        |
        |  HTTP (REST)         WebSocket (STOMP over SockJS)
        v                               v
  Spring MVC Controllers       WebSocket Message Broker
        |                               |
  Service Layer  <------- Redis Pub/Sub subscriber
        |                               |
  JPA Repositories           Redis Pub/Sub publisher
        |
  PostgreSQL
```

**Request flow for a chat message:**

1. Client sends STOMP frame to `/app/chat.send/{roomId}`.
2. `ChatController` publishes the message to a Redis channel (`chat.group.{roomId}` or `chat.dm.user.{username}`).
3. `RedisSubscriber` receives the event and broadcasts it to connected WebSocket subscribers.
4. The message is also persisted in PostgreSQL via `MessageService`.

---

## Authentication

All REST endpoints (except `/api/auth/**`) require a Bearer token in the `Authorization` header.

```
Authorization: Bearer <jwt-token>
```

Tokens are obtained via `POST /api/auth/login` or `POST /api/auth/register`.

**Token format:** JWT signed with HS256, containing `sub` (username), `jti` (unique ID), `roles`, and expiry.

**WebSocket authentication:** Pass the JWT in the STOMP `connect` headers:

```javascript
const headers = { Authorization: `Bearer ${token}` };
stompClient.connect(headers, onConnected);
```

---

## REST API

Base URL: `http://localhost:8080/api`

All responses use JSON. Error responses follow a [common format](#error-handling).

---

### Auth

#### `POST /auth/register`

Register a new user account.

**Request body:**
```json
{
  "username": "alice",
  "password": "secret123",
  "email": "alice@example.com"
}
```

**Response `200`:**
```json
{
  "token": "<jwt>",
  "username": "alice",
  "roles": ["ROLE_USER"]
}
```

---

#### `POST /auth/login`

Authenticate and receive a JWT.

**Request body:**
```json
{
  "username": "alice",
  "password": "secret123"
}
```

**Response `200`:**
```json
{
  "token": "<jwt>",
  "username": "alice",
  "roles": ["ROLE_USER"]
}
```

---

#### `POST /auth/password-reset`

Initiate a password reset. Sends a one-time code to the user's email.

**Request body:**
```json
{
  "email": "alice@example.com"
}
```

**Response `200`:** empty body.

---

#### `POST /auth/password-reset-confirm`

Complete a password reset with the emailed code.

**Request body:**
```json
{
  "token": 123456,
  "password": "newSecret456"
}
```

**Response `200`:** empty body.

---

### Users

All user endpoints require `ROLE_USER` or higher.

#### `GET /user/me`

Get the authenticated user's profile.

**Response `200`:**
```json
{
  "id": 1,
  "userName": "alice",
  "email": "alice@example.com",
  "profileImageUrl": "https://...",
  "enabled": true,
  "roles": ["ROLE_USER"],
  "createdAt": "2026-01-01T10:00:00",
  "updatedAt": "2026-01-10T12:00:00"
}
```

---

#### `PATCH /user/me`

Update the authenticated user's profile.

**Request body** (all fields optional):
```json
{
  "userName": "alice_new",
  "email": "alice_new@example.com"
}
```

**Response `200`:** updated `UserDto`.

---

#### `POST /user/profilePic/upload`

Upload or replace the profile picture.

**Request:** `multipart/form-data` with field `file` (max 10 MB, image types).

**Response `200`:** empty body.

---

#### `GET /user/`

Get all registered users.

**Response `200`:** array of `UserDto`.

---

#### `GET /user/{userId}` — Admin only

Get a specific user by ID.

**Response `200`:** `UserDto`.

---

#### `DELETE /user/{userId}` — Admin only

Delete a user account.

**Response `204`:** no content.

---

### Rooms

Rooms are either `DM` (two users) or `GROUP` (named, multi-user).

#### `GET /rooms/`

Get all rooms the authenticated user belongs to.

**Response `200`:**
```json
[
  {
    "id": 42,
    "name": "engineering",
    "description": "Engineering team chat",
    "type": "GROUP",
    "memberCount": 5,
    "recentMessage": "Hey everyone!",
    "unreadCount": 3
  }
]
```

---

#### `POST /rooms/group`

Create a new group room. The creator is automatically added as `ADMIN`.

**Request body:**
```json
{
  "name": "engineering",
  "description": "Engineering team chat",
  "type": "GROUP"
}
```

Field constraints: `name` max 100 chars, `description` max 255 chars.

**Response `200`:** `RoomDto` (see below).

---

#### `POST /rooms/dm?userId={userId}`

Create a DM room with another user, or return the existing one.

**Query param:** `userId` — ID of the other user.

**Response `200`:** `RoomDto`.

---

#### `GET /rooms/room/{roomId}`

Get members of a room.

**Response `200`:**
```json
[
  {
    "roomMemberId": 1,
    "roomId": 42,
    "userId": 7,
    "username": "bob",
    "profileImageUrl": "https://...",
    "roomRole": "MEMBER",
    "joinedAt": "2026-01-05T09:00:00"
  }
]
```

---

#### `GET /rooms/room/{roomId}/message`

Get message history for a room.

**Response `200`:**
```json
[
  {
    "id": 101,
    "roomId": 42,
    "userInfo": { "id": 7, "userName": "bob" },
    "content": "Hello!",
    "messageType": "TEXT",
    "mediaUrl": null,
    "mediaContentType": null,
    "mediaFileName": null,
    "isEdited": false,
    "isDeleted": false,
    "createdAt": "2026-01-10T10:05:00",
    "updatedAt": "2026-01-10T10:05:00"
  }
]
```

---

#### `GET /rooms/room/{roomId}/add/friends`

Get the authenticated user's friends who are not yet members of the room (for the "add members" UI).

**Response `200`:** array of `FriendsDto { userId, username, profilePicUrl }`.

---

#### `POST /rooms/room/{roomId}/add`

Add users to a room. Requester must be `ADMIN`.

**Request body:**
```json
{
  "userIds": [3, 5, 8]
}
```

**Response `204`:** no content.

---

#### `POST /rooms/room/{roomId}/remove`

Remove members from a room. Requester must be `ADMIN`.

**Request body:**
```json
{
  "membersId": [3, 5]
}
```

**Response `204`:** no content.

---

#### `PATCH /rooms/room/{roomId}`

Update room name, description, or membership. Requester must be `ADMIN`.

**Request body** (all fields optional):
```json
{
  "name": "engineering-team",
  "description": "Updated description",
  "userId": [3, 5]
}
```

**Response `200`:** `RoomDto`.

---

#### `POST /rooms/room/{roomId}/group/create`

Convert an existing DM into a group room by adding more users.

**Request body:**
```json
{
  "userIds": [10, 11]
}
```

**Response `200`:** `RoomDto`.

---

### Messages

#### `POST /rooms/room/{roomId}/message/media`

Send a media message (image or video).

**Request:** `multipart/form-data`

| Field | Type | Required | Description |
|---|---|---|---|
| `file` | Binary | Yes | Image or video, max 10 MB |
| `content` | String | No | Optional caption |

**Response `200`:**
```json
{
  "id": 202,
  "senderId": 7,
  "roomId": 42,
  "content": "Check this out",
  "messageType": "IMAGE",
  "mediaUrl": "https://...",
  "mediaContentType": "image/png",
  "mediaFileName": "screenshot.png",
  "isEdited": false,
  "isDeleted": false,
  "createdAt": "2026-01-10T11:00:00",
  "updatedAt": "2026-01-10T11:00:00"
}
```

---

#### `GET /rooms/{roomId}/messages/search?text={query}`

Search messages within a specific room.

**Query param:** `text` — search string.

**Response `200`:** array of `MessageDto`.

---

#### `GET /rooms/messages/search?text={query}`

Search messages across all rooms the user belongs to.

**Query param:** `text` — search string.

**Response `200`:** array of `MessageDto`.

---

### Friends

#### `POST /friend/request/{userId}`

Send a friend request to another user.

**Response `200`:** empty body.

---

#### `POST /friend/respond/{userId}/{response}`

Accept or reject an incoming friend request.

**Path param:** `response` — `ACCEPTED` or `REJECTED`.

**Response `200`:** empty body.

---

#### `POST /friend/{userId}/cancel`

Cancel a friend request you sent.

**Response `200`:** empty body.

---

#### `POST /friend/{userId}/unfriend`

Remove an accepted friend.

**Response `200`:** empty body.

---

#### `GET /friend/requests`

Get all incoming (pending) friend requests.

**Response `200`:**
```json
[
  {
    "id": 5,
    "requesterId": 3,
    "requesterName": "charlie",
    "recipientId": 1,
    "recipientName": "alice",
    "status": "PENDING",
    "createdAt": "2026-01-08T09:00:00",
    "updatedAt": "2026-01-08T09:00:00"
  }
]
```

---

#### `GET /friend/requests/sent`

Get all friend requests you have sent.

**Response `200`:** array of `FriendShipDto`.

---

#### `GET /friend/friends`

Get your accepted friends list.

**Response `200`:**
```json
[
  {
    "userId": 3,
    "username": "charlie",
    "profilePicUrl": "https://..."
  }
]
```

---

## WebSocket API

### Connecting

**Endpoint:** `ws://localhost:8080/ws` (SockJS fallback available)

```javascript
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

const client = new Client({
  webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
  connectHeaders: {
    Authorization: `Bearer ${token}`,
  },
  onConnect: () => {
    // subscribe to topics here
  },
});

client.activate();
```

---

### Sending Messages

**Destination:** `/app/chat.send/{roomId}`

**Payload:**
```json
{
  "roomId": 42,
  "content": "Hello, world!"
}
```

```javascript
client.publish({
  destination: `/app/chat.send/${roomId}`,
  body: JSON.stringify({ roomId, content: "Hello, world!" }),
});
```

Text messages only. For media, use the REST endpoint `POST /rooms/room/{roomId}/message/media` and then announce via WebSocket if needed.

---

### Typing Indicators

**Destination:** `/app/chat.typing/{roomId}`

**Payload:**
```json
{
  "userId": 7,
  "userName": "bob",
  "typing": true
}
```

Set `typing: false` when the user stops typing.

---

### Subscriptions

Subscribe after the STOMP connection is established.

| Topic | When to subscribe | Payload type |
|---|---|---|
| `/topic/chat.group.{roomId}` | After joining a group room | `MessageDto` |
| `/topic/rooms/{roomId}/typing` | After opening any room | `TypingEvent` |
| `/user/queue/messages` | On connect (for DMs) | `MessageDto` |
| `/user/queue/notifications` | On connect (for friend requests & room invites) | `NotificationDto` |

**Example — subscribe to a group room:**
```javascript
client.subscribe(`/topic/chat.group.${roomId}`, (frame) => {
  const message = JSON.parse(frame.body);
  // { id, senderId, roomId, content, messageType, mediaUrl, ... }
});
```

**Example — subscribe to DMs:**
```javascript
client.subscribe('/user/queue/messages', (frame) => {
  const message = JSON.parse(frame.body);
});
```

**Example — subscribe to notifications:**
```javascript
client.subscribe('/user/queue/notifications', (frame) => {
  const notification = JSON.parse(frame.body);
  // friend requests, room invites, etc.
});
```

---

## Error Handling

All errors return a consistent JSON body.

**Structure:**
```json
{
  "timestamp": "2026-04-13T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Username already taken",
  "fieldErrors": [
    {
      "field": "username",
      "message": "must not be blank"
    }
  ]
}
```

`fieldErrors` is only present for validation errors (`422`).

| Status | Meaning |
|---|---|
| `400` | Bad request / invalid input |
| `401` | Missing or invalid JWT |
| `403` | Insufficient permissions |
| `404` | Resource not found |
| `409` | Duplicate resource (e.g. username taken) |
| `500` | Internal server error |

---

## Project Structure

```
src/main/java/com/abhishekojha/kurakanimonolith/
├── common/
│   ├── config/           # Security, WebSocket, Redis, CORS, async, OpenAPI
│   ├── exception/        # Global exception handler and custom exceptions
│   ├── helpers/          # Shared utilities
│   ├── mail/             # Email service (password reset, registration)
│   ├── objectStorage/    # S3 / MinIO operations
│   ├── redis/            # Redis pub/sub subscriber
│   └── security/         # JWT filter, JWT service, UserDetailsService
└── modules/
    ├── auth/             # Register, login, password reset
    ├── user/             # Profile management
    ├── room/             # Room CRUD, DM/group logic
    ├── room_member/      # Membership management
    ├── message/          # Message persistence, media upload, search
    ├── friendRequest/    # Friend request workflow
    └── notification/     # Real-time notification dispatch

src/main/resources/
├── application.yaml          # Base config (active profile: dev)
├── application-dev.yaml      # Dev overrides
├── application-prod.yaml     # Prod overrides
├── logback-spring.xml        # Logging config
└── db/                       # Flyway SQL migration scripts
```

---

## API Docs (Swagger)

Interactive Swagger UI is served at:

```
http://localhost:8080/docs
```

Raw OpenAPI JSON is at:

```
http://localhost:8080/v3/api-docs
```

---

## Further Reading

The `docs/` directory contains deeper integration guides:

- [`docs/backend-integration-guide.md`](./docs/backend-integration-guide.md) — architecture, auth flow, REST contracts, Redis channels, storage, and security notes.
- [`docs/frontend-websocket-redis-integration.md`](./docs/frontend-websocket-redis-integration.md) — WebSocket connection setup, STOMP subscriptions, publish payloads, and a frontend implementation checklist.
