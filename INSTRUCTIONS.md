# 🎮 Game Client Setup Instructions

## Quick Start Guide

### 1. Start the API Server

**Option A: Using the batch file (Windows)**
```bash
cd ..\apiserver
start-server.bat
```

**Option B: Using Maven directly**
```bash
cd ..\apiserver
mvn compile exec:java -Dexec.mainClass="com.gameserver.api.ApiServerApplication"
```

You should see:
```
Database initialized successfully
HTTP server started on port 8080
API available at: http://localhost:8080/api
```

### 2. Test the Setup

Open `test-client.html` in your browser to verify everything works:
- Test server connection
- Test login with demo accounts
- Test inventory retrieval

### 3. Launch the Game Client

Open `index.html` in your browser or serve it with a local web server:

**Option A: Direct file access**
- Double-click `index.html`
- Or open it in your browser

**Option B: Local web server (recommended)**
```bash
# Using Python
python -m http.server 3000

# Using Node.js
npx serve .

# Using PHP
php -S localhost:3000
```

Then open: http://localhost:3000

## Demo Accounts

| Username | Password | Description |
|----------|----------|-------------|
| player1  | password123 | Regular player with starter items |
| player2  | password456 | Player with magic items |
| admin    | admin123 | Admin account with special items |

## Features Overview

### 🔐 Login System
- Secure JWT-based authentication
- Demo account quick-login buttons
- Session persistence across browser restarts

### 🎒 Inventory Management
- Visual grid layout with item cards
- Item type categorization (weapon, armor, consumable, special)
- Detailed item properties and stats
- Click items for detailed modal view

### 👤 Profile System
- Player information display
- Inventory statistics (total items, by type)
- User avatar and details

### ⚙️ Settings
- Configurable API server URL
- Auto-refresh toggle for real-time updates
- Local settings persistence

### 📱 Responsive Design
- Works on desktop and mobile devices
- Touch-friendly interface
- Adaptive layout for different screen sizes

## Troubleshooting

### Common Issues

**"Failed to connect to server"**
1. Make sure the API server is running on port 8080
2. Check if Windows Firewall is blocking the connection
3. Try accessing http://localhost:8080/api directly in browser

**"CORS Error"**
1. Use a local web server instead of opening HTML directly
2. The API server has CORS enabled for all origins

**"Session expired"**
1. Login again - tokens expire after 24 hours
2. Clear browser localStorage and retry

**Empty Inventory**
1. Make sure you're logged in with a valid demo account
2. Check browser console for error messages
3. Try refreshing the inventory

### Development Mode

For development, you can:
1. Modify `styles.css` for visual changes
2. Edit `script.js` for functionality changes
3. Update `index.html` for structure changes
4. Change API URL in Settings tab for different server locations

### Browser Compatibility

Tested on:
- Chrome 80+
- Firefox 75+
- Safari 13+
- Edge 80+

## API Endpoints

The client uses these API endpoints:

- `POST /api/login` - User authentication
- `GET /api/inventory` - Fetch user inventory

## Next Steps

1. **Add new features**: Extend the client with additional game features
2. **Customize styling**: Modify the CSS to match your game's theme
3. **Add more endpoints**: Extend both server and client for more functionality
4. **Deploy**: Set up the server on a cloud platform for remote access

Enjoy your game client! 🎮