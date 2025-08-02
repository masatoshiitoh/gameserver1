# Game Server API

A Vert.x-based REST API server for game backend services.

## Features

- **Login API**: User authentication endpoint
- **Inventory API**: Retrieve user inventory data
- **H2 Database**: In-memory database for development

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
  "username": "player1"
}
```

**Error Response (401):**
```json
{
  "error": "Invalid credentials"
}
```

### GET /api/inventory/{userId}
Get inventory items for a specific user.

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

```bash
cd apiserver
mvn compile exec:java -Dexec.mainClass="com.gameserver.api.ApiServerApplication"
```

Or using the Vert.x Maven plugin:
```bash
mvn vertx:run
```

The server will start on port 8080.

## Testing

Example login request:
```bash
curl -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{"username":"player1","password":"password123"}'
```

Example inventory request:
```bash
curl http://localhost:8080/api/inventory/1
```