# Spring Boot Azure SSO REST API

A production-ready REST API demonstrating Azure AD (Microsoft Entra ID) authentication using Spring Boot, OAuth2, and OpenID Connect. This project showcases a stateless REST API where authentication is handled via Azure AD, and a custom JWT is issued for client-side session management.

## Features

- ✅ **Java 21 & Spring Boot 3.4**
- ✅ **Azure AD (Entra ID) SSO:** Integrated with Spring Security's OAuth2 client.
- ✅ **Stateless JWT Authentication:** After a successful OAuth2 login, the server generates a custom JWT. This token is then used for authenticating subsequent API requests.
- ✅ **OAuth2 Resource Server:** The API is configured as a resource server to validate the custom JWTs.
- ✅ **Protected REST API Endpoints:** Endpoints under `/api/**` are secured.
- ✅ **Custom Success Handler:** An `OAuth2LoginSuccessHandler` creates a user profile and issues a JWT upon successful authentication.
- ✅ **CORS Support:** Pre-configured for easy frontend integration.

## How It Works: The Authentication Flow

This application uses a hybrid flow that combines the standard OAuth2 Authorization Code Grant with custom JWT generation.

1.  **Initiate Login:** The user navigates to `/oauth2/authorization/azure`. Spring Security's OAuth2 client redirects the user to the Azure AD login page.
2.  **Azure AD Authentication:** The user authenticates with their Azure credentials.
3.  **Redirection with Authorization Code:** Azure AD redirects the user back to the application at the configured redirect URI (`/login/oauth2/code/azure`) with an authorization code.
4.  **Token Exchange:** Spring Security exchanges the authorization code for an ID token and an access token from Azure AD.
5.  **Custom JWT Generation:**
    *   The `OAuth2LoginSuccessHandler` is triggered.
    *   It uses the information from the Azure AD tokens to create a custom, long-lived JWT for the client.
    *   This JWT is returned to the client in the response body.
6.  **API Communication:** The client application (e.g., a frontend SPA) stores this JWT and sends it in the `Authorization: Bearer <token>` header for all subsequent requests to protected API endpoints.
7.  **Token Validation:** The `JwtAuthFilter` intercepts each request, validates the custom JWT, and sets up the security context, allowing the user to access the protected resources.

This approach allows the frontend to remain stateless while leveraging Azure AD for the initial, secure authentication.

📖 **For a detailed explanation of the OIDC authentication flow in this application, see [OIDC_FLOW.md](OIDC_FLOW.md).**

## Prerequisites

- Java 21 or higher
- Maven 3.6+
- An Azure AD (Microsoft Entra ID) tenant (you can use your organization's tenant or create a free one)
- An active Azure subscription (free tier works fine)

## Azure AD App Registration Setup

Follow these detailed steps to configure Azure AD and obtain the required credentials:

### Step 1: Access Azure Portal and Navigate to App Registrations

1.  Go to the [Azure Portal](https://portal.azure.com) and sign in with your Microsoft account.
2.  In the left sidebar, click on **Microsoft Entra ID** (formerly Azure Active Directory).
    -   If you don't see it, use the search bar at the top and type "Microsoft Entra ID" or "Azure Active Directory".
3.  In the Microsoft Entra ID overview page, select **App registrations** from the left menu.
4.  Click the **+ New registration** button at the top.

### Step 2: Register a New Application

1.  **Name**: Enter a meaningful name for your application, e.g., `spring-boot-azure-rest-sso` or `My Spring Boot API`.
2.  **Supported account types**: Choose one of the following based on your needs:
    -   **Accounts in this organizational directory only (Single tenant)** - Recommended for testing and internal apps
    -   **Accounts in any organizational directory (Multi-tenant)** - For apps that need to support users from multiple organizations
    -   **Accounts in any organizational directory and personal Microsoft accounts** - For public-facing apps
3.  **Redirect URI**:
    -   Select **Web** from the dropdown.
    -   Enter: `http://localhost:8080/login/oauth2/code/azure`
    -   (Note: This is the callback URL where Azure AD will redirect after authentication. Spring Boot handles this endpoint automatically.)
4.  Click **Register**.

### Step 3: Copy the Application (Client) ID and Tenant ID

After registration, you'll be taken to the app's overview page.

1.  **Copy the Application (client) ID**:
    -   Find the **Application (client) ID** field on the overview page (it's a GUID like `12345678-1234-1234-1234-123456789abc`).
    -   Copy this value - you'll need it for the `AZURE_CLIENT_ID` configuration.
2.  **Copy the Directory (tenant) ID**:
    -   Find the **Directory (tenant) ID** field on the same page.
    -   Copy this value - you'll need it for the `AZURE_TENANT_ID` configuration.

### Step 4: Create a Client Secret

1.  In the left menu of your app registration, click on **Certificates & secrets**.
2.  Click on the **+ New client secret** button.
3.  **Description**: Enter a description, e.g., `spring-boot-api-secret`.
4.  **Expires**: Choose an expiration period (e.g., 6 months, 12 months, or 24 months).
    -   ⚠️ **Important**: Make a note of the expiration date. You'll need to create a new secret and update your application before it expires.
5.  Click **Add**.
6.  **Copy the client secret value immediately**:
    -   After creation, you'll see the secret value in the **Value** column.
    -   ⚠️ **Critical**: Copy this value NOW. Azure will never show it again. If you lose it, you'll need to create a new secret.
    -   This is your `AZURE_CLIENT_SECRET`.

### Step 5: Configure API Permissions

1.  In the left menu, click on **API permissions**.
2.  You should see **Microsoft Graph** → **User.Read** already added by default. We need to add more permissions.
3.  Click **+ Add a permission**.
4.  Select **Microsoft Graph** → **Delegated permissions**.
5.  Search for and select the following permissions:
    -   ✅ `openid` (under OpenId permissions)
    -   ✅ `profile` (under OpenId permissions)
    -   ✅ `email` (under OpenId permissions)
    -   ✅ `User.Read` (should already be there)
6.  Click **Add permissions**.
7.  **(Optional but Recommended)** Click the **Grant admin consent for [Your Organization]** button. This will pre-approve these permissions for all users in your tenant, so they won't see a consent screen during login.
    -   Note: You need admin privileges to grant consent.

### Step 6: Configure Authentication Settings

1.  In the left menu, click on **Authentication**.
2.  Under **Implicit grant and hybrid flows**, check the following:
    -   ✅ **ID tokens** (used for OpenID Connect sign-in)
    -   ⬜ **Access tokens** (leave unchecked - not needed for this flow)
3.  Under **Allow public client flows**, select **No**.
4.  Click **Save** at the top.

### Step 7: Verify Your Configuration

At this point, you should have collected:

-   ✅ **Tenant ID**: `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`
-   ✅ **Client ID**: `yyyyyyyy-yyyy-yyyy-yyyy-yyyyyyyyyyyy`
-   ✅ **Client Secret**: `your-secret-value~zzz`

Keep these values secure - you'll use them in the next step to configure your Spring Boot application.

## Configuration

Update `src/main/resources/application.yml` or set the following environment variables with your Azure AD App Registration details:

```yaml
azure:
  tenant-id: YOUR_TENANT_ID
  client-id: YOUR_CLIENT_ID
  client-secret: YOUR_CLIENT_SECRET

app:
  security:
    jwt:
      secret-key: YOUR_OWN_SECURE_JWT_SECRET # Should be a long, random string
```

## API Endpoints

-   **`GET /oauth2/authorization/azure`**:
    -   Initiates the Azure AD login flow. Access this in your browser to start.
-   **`GET /api/v1/home`**:
    -   A protected endpoint. Requires a valid JWT.
    -   Returns a welcome message.
-   **`GET /api/v1/home/details`**:
    -   A protected endpoint. Requires a valid JWT.
    -   Returns a welcome message with user details from the token.
-   **`GET /api/v1/user/profile`**:
    -   A protected endpoint. Requires a valid JWT.
    -   Returns the authenticated user's profile information.

## How to Run

1.  **Configure:** Update `application.yml` with your Azure AD details.
2.  **Run the application:**
    ```bash
    mvn spring-boot:run
    ```
3.  **Login:** Open your browser and go to `http://localhost:8080/oauth2/authorization/azure`.
4.  **Get Token:** After logging in with your Azure account, you will be redirected, and the API will return a JSON response containing your JWT.
5.  **Access Protected API:** Use a tool like Postman or `curl` to make requests to the protected endpoints, including the JWT in the `Authorization` header.

    ```bash
    curl -H "Authorization: Bearer <YOUR_JWT_TOKEN>" http://localhost:8080/api/v1/user/profile
    ```

export AZURE_TENANT_ID=your-tenant-id
export AZURE_CLIENT_ID=your-client-id
export AZURE_CLIENT_SECRET=your-client-secret
export AZURE_APP_ID_URI=api://your-client-id
```

### Application Properties

Update `src/main/resources/application.yml` with your Azure AD details:

```yaml
spring:
  cloud:
    azure:
      active-directory:
        profile:
          tenant-id: ${AZURE_TENANT_ID}
        credential:
          client-id: ${AZURE_CLIENT_ID}
          client-secret: ${AZURE_CLIENT_SECRET}
        app-id-uri: ${AZURE_APP_ID_URI}
```

## Build & Run

### Build the project

```bash
mvn clean package
```

### Run tests

```bash
mvn test
```

### Run the application

```bash
# Default profile
mvn spring-boot:run

# Development profile (verbose logging)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Or run the JAR
java -jar target/azure-sso-api-1.0.0-SNAPSHOT.jar
```

The application will start on `http://localhost:8080`

## API Endpoints

### Public Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/actuator/health` | Health check endpoint |

### Protected Endpoints (Require JWT Token)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/home` | Welcome message with auth status |
| GET | `/api/v1/home/jwt` | Welcome message with JWT claims |
| GET | `/api/v1/status` | Authentication status details |
| GET | `/api/v1/user/profile` | User profile from JWT token |
| GET | `/api/v1/user/claims` | All JWT token claims (debug) |
| GET | `/api/v1/user/me` | User info via OIDC (browser flow) |
| GET | `/api/v1/user/roles` | User roles and groups |

## Testing the API

### Method 1: Using Postman or cURL (Recommended)

#### Step 1: Get Access Token from Azure AD

**Using Postman:**

1. Create a new request in Postman
2. Go to **Authorization** tab
3. Select **OAuth 2.0**
4. Configure:
   - **Grant Type**: `Authorization Code`
   - **Callback URL**: `http://localhost:8080/login/oauth2/code/azure`
   - **Auth URL**: `https://login.microsoftonline.com/{tenant-id}/oauth2/v2.0/authorize`
   - **Access Token URL**: `https://login.microsoftonline.com/{tenant-id}/oauth2/v2.0/token`
   - **Client ID**: `{your-client-id}`
   - **Client Secret**: `{your-client-secret}`
   - **Scope**: `api://{your-client-id}/access_as_user openid profile email`
5. Click **Get New Access Token**
6. Sign in with Azure AD
7. Copy the access token

**Using cURL:**

```bash
# 1. Get authorization code (open this URL in browser)
https://login.microsoftonline.com/{tenant-id}/oauth2/v2.0/authorize?client_id={client-id}&response_type=code&redirect_uri=http://localhost:8080/login/oauth2/code/azure&scope=api://{client-id}/access_as_user%20openid%20profile%20email

# 2. Exchange authorization code for access token
curl -X POST https://login.microsoftonline.com/{tenant-id}/oauth2/v2.0/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id={client-id}" \
  -d "client_secret={client-secret}" \
  -d "code={authorization-code}" \
  -d "redirect_uri=http://localhost:8080/login/oauth2/code/azure" \
  -d "grant_type=authorization_code"
```

#### Step 2: Call Protected Endpoints

```bash
# Set your access token
export ACCESS_TOKEN="your-jwt-access-token"

# Test home endpoint
curl -H "Authorization: Bearer $ACCESS_TOKEN" http://localhost:8080/api/v1/home

# Test user profile
curl -H "Authorization: Bearer $ACCESS_TOKEN" http://localhost:8080/api/v1/user/profile

# Test JWT claims
curl -H "Authorization: Bearer $ACCESS_TOKEN" http://localhost:8080/api/v1/user/claims

# Test user roles
curl -H "Authorization: Bearer $ACCESS_TOKEN" http://localhost:8080/api/v1/user/roles
```

### Expected Responses

**Home endpoint:**
```json
{
  "message": "Welcome to Azure SSO REST API!",
  "authenticated": true,
  "user": "user@example.com",
  "timestamp": 1712345678901
}
```

**User profile:**
```json
{
  "oid": "12345-67890-abcdef",
  "name": "John Doe",
  "email": "john.doe@example.com",
  "preferredUsername": "john.doe@example.com",
  "givenName": "John",
  "familyName": "Doe",
  "tenantId": "tenant-id-123",
  "subject": "12345-67890-abcdef",
  "issuer": "https://login.microsoftonline.com/tenant-id/v2.0",
  "audience": ["api://your-client-id"]
}
```

## Troubleshooting

### Common Issues

#### 1. 401 Unauthorized Response

**Cause**: Invalid or missing JWT token

**Solution**: 
- Verify your access token is valid (check expiration)
- Ensure you're sending the token in the `Authorization: Bearer <token>` header
- Check that the token audience matches your `app-id-uri`

#### 2. AADSTS50011: Redirect URI mismatch

**Cause**: The redirect URI in the request doesn't match what's configured in Azure AD

**Solution**: 
- Verify redirect URI in Azure Portal matches exactly: `http://localhost:8080/login/oauth2/code/azure`
- Check for trailing slashes and protocol (http vs https)

#### 3. AADSTS65001: User consent required

**Cause**: Admin consent not granted for API permissions

**Solution**: 
- In Azure Portal → App Registration → API permissions
- Click "Grant admin consent"

#### 4. JWT validation fails

**Cause**: Issuer or audience mismatch

**Solution**: 
- Check `application.yml` has correct `tenant-id`
- Verify `app-id-uri` matches what's configured in Azure Portal → Expose an API
- Check JWT claims using https://jwt.ms

#### 5. Application fails to start

**Cause**: Missing or invalid Azure AD configuration

**Solution**: 
- Verify all environment variables are set correctly
- For local testing, you can disable Azure AD: `spring.cloud.azure.active-directory.enabled=false`

### Enable Debug Logging

Add to `application.yml`:

```yaml
logging:
  level:
    org.springframework.security: DEBUG
    com.azure.spring: DEBUG
    org.springframework.security.oauth2: TRACE
```

## Security Best Practices

### Production Deployment

1. **Store Secrets Securely**
   - Use Azure Key Vault for client secrets
   - Never commit secrets to version control
   - Use environment variables or Azure App Configuration

2. **Enable HTTPS**
   - Configure SSL/TLS certificates
   - Redirect HTTP to HTTPS
   - Update Azure AD redirect URIs to use `https://`

3. **Configure CORS Properly**
   - Restrict `allowed-origins` to specific domains
   - Don't use `*` in production
   - Set `allowCredentials: true` only if needed

4. **Token Validation**
   - Verify token signatures (automatic with Spring Security)
   - Check token expiration (automatic)
   - Validate audience and issuer claims

5. **Monitoring and Logging**
   - Monitor failed authentication attempts
   - Log security events
   - Set up alerts for anomalies

## Project Structure

```
spring-boot-azure-rest-sso/
├── src/
│   ├── main/
│   │   ├── java/com/example/azuressoapi/
│   │   │   ├── AzureSsoApiApplication.java      # Main application class
│   │   │   ├── config/
│   │   │   │   └── SecurityConfig.java          # Security configuration
│   │   │   └── controller/
│   │   │       ├── HomeController.java          # Home endpoint
│   │   │       └── UserController.java          # User profile endpoints
│   │   └── resources/
│   │       ├── application.yml                  # Main configuration
│   │       ├── application-dev.yml              # Development profile
│   │       └── application-prod.yml             # Production profile
│   └── test/
│       └── java/com/example/azuressoapi/
│           └── SecurityTests.java               # Integration tests
├── pom.xml                                      # Maven dependencies
└── README.md                                    # This file
```

## Technology Stack

- **Java 21** - Latest LTS version
- **Spring Boot 3.4.0** - Application framework
- **Spring Security 6.x** - Security framework
- **Spring Cloud Azure 6.2.0** - Azure integration
- **OAuth2 / OpenID Connect** - Authentication protocol
- **Maven** - Build tool
- **JUnit 5** - Testing framework

## License

This project is licensed under the MIT License.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Support

For issues and questions:
- Check the [Troubleshooting](#troubleshooting) section
- Review [Azure AD documentation](https://learn.microsoft.com/en-us/azure/active-directory/)
- Review [Spring Cloud Azure documentation](https://spring.io/projects/spring-cloud-azure)

## References

- [Spring Security OAuth2 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/index.html)
- [Spring Cloud Azure Active Directory](https://learn.microsoft.com/en-us/azure/developer/java/spring-framework/spring-boot-starter-for-azure-active-directory-developer-guide)
- [Microsoft Identity Platform](https://learn.microsoft.com/en-us/azure/active-directory/develop/)
- [OAuth 2.0 and OpenID Connect](https://learn.microsoft.com/en-us/azure/active-directory/develop/v2-protocols)