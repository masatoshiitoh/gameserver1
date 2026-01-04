# Game Server API

A Vert.x-based REST API server for game backend services.

## Features

- **Login API**: User authentication with JWT tokens
- **Inventory API**: Retrieve user inventory data
- **Test Client**: Built-in web client for testing at `/testclient/`
- **H2 Database**: In-memory database for development
- **CORS Support**: Cross-origin requests enabled

## API Endpoints

### POST /api/login
Authenticate a user with username and password.

**Request Body:**
```json
{
  "username": "player1",
  "password": "password123"
}
```

**Success Response (200):**
```json
{
  "success": true,
  "userId": 1,
  "username": "player1",
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Error Response (401):**
```json
{
  "error": "Invalid credentials"
}
```

### GET /api/inventory
Get inventory items for the authenticated user.

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Success Response (200):**
```json
{
  "userId": 1,
  "inventory": [
    {
      "item_name": "Iron Sword",
      "item_type": "weapon",
      "quantity": 1,
      "properties": {
        "damage": 50,
        "durability": 100
      }
    }
  ]
}
```

## Sample Users

- Username: `player1`, Password: `password123`
- Username: `player2`, Password: `password456`
- Username: `admin`, Password: `admin123`

## Running the Server

### Build and Run

```bash
cd apiserver
mvn clean compile
mvn exec:java -Dexec.mainClass="com.gameserver.api.ApiServerApplication"
```

Or using the Vert.x Maven plugin:
```bash
mvn vertx:run
```

The server will start on port 8080 and display:
```
HTTP server started on port 8080
API available at: http://localhost:8080/api
Test client available at: http://localhost:8080/testclient/
```

### Access Points

- **API Server**: http://localhost:8080/api
- **Test Client**: http://localhost:8080/testclient/

## Testing

### Using the Web Client

The easiest way to test the API is using the built-in web client:
1. Start the server
2. Open http://localhost:8080/testclient/ in your browser
3. Login with demo accounts (player1/password123, player2/password456, or admin/admin123)

### Using cURL

Example login request:
```bash
curl -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{"username":"player1","password":"password123"}'
```

Example inventory request (requires authentication):
```bash
# Save the accessToken from login response, then:
curl http://localhost:8080/api/inventory \
  -H "Authorization: Bearer <accessToken>"
```

## Project Structure

```
apiserver/
├── src/
│   ├── main/
│   │   ├── java/com/gameserver/api/
│   │   │   ├── ApiServerApplication.java  # Main application
│   │   │   ├── DatabaseService.java       # Database operations
│   │   │   └── JwtService.java            # JWT token handling
│   │   └── resources/
│   │       └── webroot/
│   │           └── testclient/            # Web client files
│   │               ├── index.html
│   │               ├── styles.css
│   │               └── script.js
│   └── test/                              # Test files
├── pom.xml                                # Maven configuration
└── README.md                              # This file
```

## Technology Stack

- **Vert.x 4.4.4**: Reactive application framework
- **H2 Database**: In-memory SQL database
- **JWT**: JSON Web Tokens for authentication
- **Maven**: Build and dependency management