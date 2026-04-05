# OIDC Flow with Spring Boot and Azure AD

This document explains the OpenID Connect (OIDC) authentication flow as implemented in this Spring Boot application for Azure Single Sign-On (SSO).

## Table of Contents

- [The Core Concept: Hybrid Flow](#the-core-concept-hybrid-flow)
- [Step-by-Step OIDC Flow](#step-by-step-oidc-flow)
- [Default Spring Boot OAuth2 Endpoints](#default-spring-boot-oauth2-endpoints)
- [Custom Components](#custom-components)
- [Complete Flow Diagram](#complete-flow-diagram)

## The Core Concept: Hybrid Flow

This application uses a combination of the standard **OAuth 2.0 Authorization Code Grant** flow and custom token handling. Here's a high-level overview:

1.  **Delegate to Azure AD:** The application relies on Azure AD to handle the user login and consent process.
2.  **Receive Authorization:** After a successful login, Azure AD provides a temporary authorization code to the application.
3.  **Create a Custom Session:** The Spring Boot backend exchanges this code for user information and then creates its own JWT (JSON Web Token). This custom JWT, not the one from Azure, is sent to the client (e.g., a browser application).
4.  **Stateless API Calls:** The client uses this custom JWT to authenticate with the REST API for all subsequent requests.

This approach is ideal for Single Page Applications (SPAs) because it avoids exposing Azure's access or ID tokens directly to the browser and allows the backend to control session duration and claims within the JWT.

## Step-by-Step OIDC Flow

### 1. Initiating the Login

The flow begins when the user is directed to a special URL constructed by Spring Security.

-   **Default Spring Boot Endpoint:** `GET /oauth2/authorization/{registrationId}`
-   **In this application:** The `registrationId` is `azure`, so the URL is `GET /oauth2/authorization/azure`.

**What happens internally:**

When a user accesses this endpoint, Spring Security's `OAuth2AuthorizationRequestRedirectFilter` intercepts the request. It then constructs an authorization request and redirects the user's browser to Azure AD's authorization endpoint.

**Redirect URL example:**
```
https://login.microsoftonline.com/\{tenant-id\}/oauth2/v2.0/authorize\?
  client_id={client-id}&
  response_type=code&
  redirect_uri=http://localhost:8080/login/oauth2/code/azure&
  scope=openid profile email&
  state={random-state}&
  nonce={random-nonce}
```

**Key parameters:**
- `client_id`: Your Azure AD Application (client) ID
- `response_type=code`: Indicates we want an authorization code
- `redirect_uri`: Where Azure should redirect after authentication
- `scope`: The permissions we're requesting (openid, profile, email)
- `state`: A random value to prevent CSRF attacks
- `nonce`: A random value to prevent replay attacks

### 2. User Authentication at Azure AD

-   The user is now on the Microsoft login page.
-   They enter their credentials (email/password, MFA, etc.).
-   If it's the first time, they may be asked to consent to the permissions requested by the application (e.g., `openid`, `profile`, `email`).
-   Azure AD validates the credentials and consent.

### 3. Redirection to Spring Boot with Authorization Code

After successful authentication, Azure AD redirects the user back to the application using the `redirect_uri` specified in the initial request.

-   **Default Spring Boot Callback URL:** `GET /login/oauth2/code/{registrationId}`
-   **In this application:** `GET /login/oauth2/code/azure`

**Example redirect URL:**
```
http://localhost:8080/login/oauth2/code/azure\?
  code={authorization-code}&
  state={same-state-as-before}
```

The redirect URL will include:
- `code`: The authorization code that can be exchanged for tokens
- `state`: The same state value sent in step 1 (Spring validates this matches)

### 4. Code-for-Token Exchange

Spring Security's `OAuth2LoginAuthenticationFilter` handles this callback request. In the background, it performs a "code-for-token" exchange:

**What happens:**

1.  Spring Security sends a **server-to-server POST request** to Azure AD's token endpoint:
    ```
    POST https://login.microsoftonline.com/\{tenant-id\}/oauth2/v2.0/token
    Content-Type: application/x-www-form-urlencoded
    
    grant_type=authorization_code&
    code={authorization-code}&
    redirect_uri=http://localhost:8080/login/oauth2/code/azure&
    client_id={client-id}&
    client_secret={client-secret}
    ```

2.  Azure AD validates the request and returns:
    - **ID Token**: A JWT containing user identity information (name, email, OID, etc.)
    - **Access Token**: A token that can be used to call Microsoft Graph API
    - **Refresh Token** (optional): For obtaining new tokens

3.  Spring Security validates the ID token's signature using Azure's public keys (downloaded from the `jwk-set-uri`).

4.  Spring Security extracts user information from the ID token and creates an `OidcUser` object.

### 5. Custom OAuth2 User Service (`AzureOAuth2UserService`)

Before the success handler is called, Spring Security invokes our custom `AzureOAuth2UserService` to load additional user details if needed.

**Purpose:**
- Enriches the user object with additional information
- Maps Azure AD claims to our application's `User` model
- Can call Microsoft Graph API for additional user data (e.g., profile picture, manager info)

**Key method:**
```java
@Override
public OAuth2User loadUser(OAuth2UserRequest userRequest) {
    OAuth2User oauth2User = super.loadUser(userRequest);
    
    // Extract user information from Azure AD
    Map<String, Object> attributes = oauth2User.getAttributes();
    
    // Create our custom User object
    User user = User.builder()
        .oid((String) attributes.get("oid"))
        .email((String) attributes.get("email"))
        .name((String) attributes.get("name"))
        // ... more fields
        .build();
    
    return new OAuth2UserImpl(user, attributes, "email");
}
```

### 6. Custom Success Handler (`OAuth2LoginSuccessHandler`)

This is where the application's custom logic comes into play. Once Spring Security successfully obtains the tokens from Azure AD and loads the user, it invokes our custom `OAuth2LoginSuccessHandler`.

**What it does:**

1.  **Receives Authenticated User:** The handler is passed an `Authentication` object, which contains the user details fetched from Azure AD (processed by our `AzureOAuth2UserService`).

2.  **Extracts User Information:**
    ```java
    OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
    Map<String, Object> attributes = oidcUser.getAttributes();
    
    User user = new User();
    user.setEmail((String) attributes.get("email"));
    user.setName((String) attributes.get("name"));
    user.setOid((String) attributes.get("oid"));
    ```

3.  **Generates Custom JWT:** It calls the `JwtService` to create a new, custom JWT. This token is signed with the application's own secret key (not Azure's). The user's details (like email, name, and OID) are included as claims in this new token.
    ```java
    String token = jwtService.generateToken(user);
    ```

4.  **Returns JWT to Client:** The handler writes the custom JWT directly into the HTTP response body as a JSON object:
    ```json
    {
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "user": {
        "id": "12345678-1234-1234-1234-123456789abc",
        "email": "user@example.com",
        "name": "John Doe",
        "oid": "12345678-1234-1234-1234-123456789abc"
      },
      "message": "Authentication successful"
    }
    ```

5.  **No Session Created:** Because the API is configured with `SessionCreationPolicy.STATELESS`, no server-side session is created. The JWT is the sole proof of authentication from this point forward.

### 7. Client Stores and Uses JWT

The client application (e.g., a React/Angular SPA or mobile app) is now responsible for:

1.  **Storing the JWT**: In local storage, session storage, or memory (depending on security requirements).
2.  **Sending JWT with every request**: Include the token in the `Authorization` header:
    ```
    Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
    ```

### 8. JWT Validation on Subsequent Requests (`JwtAuthFilter`)

For every incoming request to a protected endpoint (e.g., `/api/v1/user/profile`), the `JwtAuthFilter` intercepts the request and validates the JWT.

**Filter execution flow:**

1.  **Extract Token from Header:**
    ```java
    String header = request.getHeader("Authorization");
    String token = header.substring(7).trim(); // Remove "Bearer " prefix
    ```

2.  **Validate Token:**
    ```java
    if (!jwtService.isTokenValid(token)) {
        // Token is invalid or expired, continue without authentication
        filterChain.doFilter(request, response);
        return;
    }
    ```

3.  **Parse User from Token:**
    ```java
    User user = jwtService.parseToken(token);
    ```

4.  **Set Security Context:**
    ```java
    OAuth2UserImpl userDetails = new OAuth2UserImpl(user, new HashMap<>(), "email");
    OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
        userDetails,
        userDetails.getAuthorities(),
        "azure"
    );
    SecurityContextHolder.getContext().setAuthentication(authentication);
    ```

5.  **Continue Filter Chain:** The request proceeds to the controller with the authenticated user context.

## Default Spring Boot OAuth2 Endpoints

Spring Security provides these endpoints automatically when OAuth2 client is configured:

| Endpoint | Purpose | HTTP Method | When Used |
|----------|---------|-------------|-----------|
| `/oauth2/authorization/{registrationId}` | Initiates OAuth2 login flow | GET | User clicks "Login with Azure" |
| `/login/oauth2/code/{registrationId}` | OAuth2 callback endpoint | GET | Azure redirects here after authentication |
| `/logout` | Logs out the user | GET/POST | User clicks "Logout" |

**In this application:**
- `{registrationId}` = `azure` (defined in `application.yml` under `spring.security.oauth2.client.registration.azure`)
- Login endpoint: `/oauth2/authorization/azure`
- Callback endpoint: `/login/oauth2/code/azure`

These endpoints are created automatically by Spring Security - you don't need to write controllers for them!

## Custom Components

### 1. `OAuth2LoginSuccessHandler`

**Location:** `com.example.azuressoapi.config.OAuth2LoginSuccessHandler`

**Purpose:** Handles successful OAuth2 authentication by generating a custom JWT token.

**Key Responsibilities:**
- Extract user information from Azure AD's OIDC token
- Generate a custom JWT using `JwtService`
- Return the JWT to the client in a JSON response
- Set appropriate HTTP headers

### 2. `JwtAuthFilter`

**Location:** `com.example.azuressoapi.config.JwtAuthFilter`

**Purpose:** Validates JWT tokens on incoming requests and sets up Spring Security context.

**Key Responsibilities:**
- Extract JWT from `Authorization` header
- Validate token signature and expiration
- Parse user information from token
- Set authentication in Spring Security context

**Filter Order:** Executes before `BasicAuthenticationFilter` (configured in `SecurityConfig`)

### 3. `JwtService`

**Location:** `com.example.azuressoapi.service.JwtService`

**Purpose:** Handles all JWT operations (generation, validation, parsing).

**Key Responsibilities:**
- Generate JWT with custom claims
- Validate token signature and expiration
- Parse token and extract user information
- Use HMAC SHA-256 algorithm for signing

**Configuration:**
- Secret key: `app.security.jwt.secret-key` (from `application.yml`)
- Expiration: `app.security.jwt.expiration` (default: 24 hours)

### 4. `AzureOAuth2UserService`

**Location:** `com.example.azuressoapi.service.AzureOAuth2UserService`

**Purpose:** Loads and enriches user information from Azure AD.

**Key Responsibilities:**
- Fetch user details from Azure AD
- Map Azure AD claims to application's `User` model
- Create `OAuth2UserImpl` wrapper for Spring Security

### 5. `SecurityConfig`

**Location:** `com.example.azuressoapi.config.SecurityConfig`

**Purpose:** Central Spring Security configuration.

**Key Configuration:**
- Enables OAuth2 login with Azure
- Configures JWT filter in the filter chain
- Sets stateless session management
- Defines public and protected endpoints
- Configures CORS for cross-origin requests

## Complete Flow Diagram

```
┌─────────┐                                  ┌──────────────┐
│         │ 1. GET /oauth2/authorization/azure │            │
│  User   ├──────────────────────────────────>│   Spring   │
│ Browser │                                   │    Boot    │
│         │<──────────────────────────────────┤   App      │
└────┬────┘ 2. Redirect to Azure AD           └──────────────┘
     │
     │ 3. User logs in with Microsoft credentials
     │
     v
┌──────────┐
│  Azure   │
│    AD    │
└────┬─────┘
     │
     │ 4. Redirect to /login/oauth2/code/azure?code={auth-code}
     │
     v
┌──────────────────────────────────────────────────────────────┐
│                       Spring Boot App                        │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ 5. OAuth2LoginAuthenticationFilter                  │   │
│  │    - Exchanges code for Azure tokens                │   │
│  │    - Validates ID token                             │   │
│  └────────────────────┬────────────────────────────────┘   │
│                       │                                     │
│  ┌────────────────────v────────────────────────────────┐   │
│  │ 6. AzureOAuth2UserService                           │   │
│  │    - Loads user from Azure AD                       │   │
│  │    - Creates User object                            │   │
│  └────────────────────┬────────────────────────────────┘   │
│                       │                                     │
│  ┌────────────────────v────────────────────────────────┐   │
│  │ 7. OAuth2LoginSuccessHandler                        │   │
│  │    - Extracts user info                             │   │
│  │    - Calls JwtService.generateToken()               │   │
│  │    - Returns JWT in JSON response                   │   │
│  └────────────────────┬────────────────────────────────┘   │
│                       │                                     │
└───────────────────────┼─────────────────────────────────────┘
                        │
                        v
                  ┌─────────┐
                  │ Response│
                  │  JSON   │
                  │ {"token"│
                  │  "..."}│
                  └────┬────┘
                       │
                       v
          ┌────────────────────────┐
          │   Client stores JWT    │
          │   in local storage     │
          └────────┬───────────────┘
                   │
                   │ 8. Subsequent API requests
                   │    Authorization: Bearer <JWT>
                   │
                   v
          ┌────────────────────────────────────────────┐
          │         Spring Boot App (API Request)      │
          │                                            │
          │  ┌──────────────────────────────────────┐ │
          │  │ JwtAuthFilter                        │ │
          │  │  - Extracts JWT from header          │ │
          │  │  - Validates token                   │ │
          │  │  - Sets SecurityContext              │ │
          │  └───────────────┬──────────────────────┘ │
          │                  │                        │
          │  ┌───────────────v──────────────────────┐ │
          │  │ Controller (e.g., UserController)    │ │
          │  │  - Accesses @AuthenticationPrincipal │ │
          │  │  - Returns user data                 │ │
          │  └──────────────────────────────────────┘ │
          │                                            │
          └────────────────────────────────────────────┘
```

## Key Takeaways

1.  **Spring Boot handles most of the OAuth2 flow automatically** - you just need to configure it in `application.yml`.

2.  **Two sets of tokens are involved:**
    - **Azure AD tokens** (ID token, access token) - used during the initial authentication flow
    - **Custom JWT** - created by your application for subsequent API requests

3.  **The custom JWT approach provides:**
    - Full control over token expiration
    - Custom claims tailored to your application
    - Stateless authentication without relying on Azure AD for every request
    - Protection of Azure AD tokens from client-side exposure

4.  **Default endpoints are automatically available** - no need to create controllers for OAuth2 login flow.

5.  **Security is layered:**
    - Azure AD handles user authentication
    - Your application controls authorization and session management
    - JWT validation happens on every API request

## Troubleshooting

### Common Issues

**Issue:** Redirect URI mismatch
- **Solution:** Ensure the redirect URI in Azure AD matches exactly: `http://localhost:8080/login/oauth2/code/azure`

**Issue:** Invalid client secret
- **Solution:** Regenerate the client secret in Azure AD and update `application.yml`

**Issue:** JWT signature verification failed
- **Solution:** Check that `app.security.jwt.secret-key` is set correctly and hasn't changed

**Issue:** Token expired
- **Solution:** The custom JWT has a 24-hour expiration by default. User needs to log in again via `/oauth2/authorization/azure`
