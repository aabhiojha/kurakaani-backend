# Authentication and Authorization Flow

## Overview

This backend uses Google OAuth2 for initial login, persists users and roles in PostgreSQL, issues a JWT after successful login, and uses that JWT for all future API authorization.

## End-to-End Flow

1. The client starts login by calling `/oauth2/authorization/google`.
2. Spring Security begins the Google OAuth2 flow.
3. The OAuth2 authorization request is stored in a short-lived cookie by `HttpCookieOAuth2AuthorizationRequestRepository`.
4. The user authenticates with Google.
5. Google redirects back to `/login/oauth2/code/google`.
6. Spring Security completes OAuth2 authentication and invokes `OAuth2AuthenticationSuccessHandler`.
7. The success handler extracts the Google user information:
   - `email`
   - `name`
8. The success handler calls `UserService.loadOrCreateOAuth2User(email, name)`.
9. `UserService` checks PostgreSQL through `UserRepository`.
10. If the user does not exist:
    - a new `AppUser` row is created
    - the default role `ROLE_USER` is assigned
11. If the user already exists:
    - the existing user is loaded
    - the stored roles are reused
12. `JwtService` generates a JWT for that user.
13. The JWT includes:
    - `sub` as the user email
    - `userId`
    - `name`
    - `roles`
    - issued time
    - expiration time
14. The success handler returns a JSON response containing:
    - `tokenType`
    - `accessToken`
    - `expiresAt`
    - `user`
15. No HTTP session is created because the application is configured as stateless.

## Subsequent Request Flow

1. The client stores the JWT.
2. For each protected API request, the client sends:

```http
Authorization: Bearer <jwt>
```

3. `JwtAuthenticationFilter` runs before the main Spring Security authentication filters.
4. The filter:
   - reads the `Authorization` header
   - extracts the bearer token
   - validates the token signature and expiration using `JwtService`
   - reads the `roles` claim
   - creates an authenticated `UsernamePasswordAuthenticationToken`
   - stores it in the `SecurityContext`
5. Spring Security then applies endpoint authorization rules.

## RBAC Rules

- `/api/auth/**` is public
- `/api/user/**` requires `ROLE_USER`
- `/api/admin/**` requires `ROLE_ADMIN`

## Important Components

- `SecurityConfig`
  Configures stateless security, OAuth2 login, the JWT filter, and RBAC rules.

- `OAuth2AuthenticationSuccessHandler`
  Converts a successful Google login into a persisted local user and a JWT response.

- `AppUser`
  JPA entity for local user storage.

- `UserRepository`
  Loads users by email from PostgreSQL.

- `UserService`
  Creates first-time users and loads existing roles.

- `JwtService`
  Generates and validates JWTs.

- `JwtAuthenticationFilter`
  Authenticates incoming bearer tokens.

- `HttpCookieOAuth2AuthorizationRequestRepository`
  Keeps the OAuth2 handshake stateless by storing the authorization request in a cookie.

## Result

The system uses Google only for identity verification during login. After that, the application relies entirely on JWTs for authentication and on stored roles for authorization.
