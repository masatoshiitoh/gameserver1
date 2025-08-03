# Game Client - Adventure Quest

A social game-like web client for the Game Server API.

## Features

- 🎮 **Social Game UI**: Modern, responsive interface with game-like styling
- 🔐 **Authentication**: Login with username/password or demo accounts
- 🎒 **Inventory Management**: View and interact with player inventory
- 👤 **Profile System**: Player profile with statistics
- ⚙️ **Settings**: Configurable API endpoint and auto-refresh
- 📱 **Responsive Design**: Works on desktop and mobile devices

## Quick Start

1. **Start the API Server**:
   ```bash
   cd ../apiserver
   mvn spring-boot:run
   ```
   The API server should be running on `http://localhost:8080`

2. **Open the Client**:
   Open `index.html` in your web browser or serve it with a local web server:
   ```bash
   # Using Python
   python -m http.server 3000
   
   # Using Node.js
   npx serve .
   
   # Or simply open index.html in your browser
   ```

3. **Login**:
   Use one of the demo accounts or create your own:
   - **Player 1**: `player1` / `password123`
   - **Player 2**: `player2` / `password456`
   - **Admin**: `admin` / `admin123`

## Usage

### Login Screen
- Enter username and password manually
- Use demo account buttons for quick access
- Login credentials are remembered in localStorage

### Game Interface

#### Inventory Tab 🎒
- View all items in your inventory
- Click on items to see detailed information
- Items are categorized by type (weapon, armor, consumable, special)
- Auto-refresh option available

#### Profile Tab 👤
- View player information
- See inventory statistics
- Player avatar and details

#### Settings Tab ⚙️
- Configure API server URL
- Enable/disable auto-refresh
- Settings are saved locally

### Features

- **Real-time Updates**: Inventory refreshes automatically (if enabled)
- **Session Management**: Login state persists across browser sessions
- **Error Handling**: User-friendly error messages
- **Responsive Design**: Works on all screen sizes
- **Accessibility**: Keyboard navigation and screen reader friendly

## API Endpoints Used

- `POST /api/login` - User authentication
- `GET /api/inventory` - Fetch user inventory

## Browser Compatibility

- Chrome 80+
- Firefox 75+
- Safari 13+
- Edge 80+

## Development

### File Structure
```
testclient/
├── index.html      # Main HTML file
├── styles.css      # CSS styling
├── script.js       # JavaScript logic
└── README.md       # This file
```

### Customization

1. **API URL**: Change in Settings tab or modify `apiUrl` in `script.js`
2. **Styling**: Edit `styles.css` to customize appearance
3. **Features**: Add new functionality in `script.js`

### Adding New Features

To add new API endpoints:

1. Add new tab in HTML
2. Create corresponding CSS styles
3. Implement API calls in JavaScript
4. Add tab switching logic

## Troubleshooting

### Common Issues

1. **"Failed to connect to server"**
   - Check if API server is running
   - Verify API URL in settings
   - Check browser console for CORS errors

2. **"Session expired"**
   - Login again
   - Check if API server is accessible

3. **Inventory not loading**
   - Check network tab in browser dev tools
   - Verify authentication token
   - Try refreshing the page

### CORS Issues

If running into CORS issues, make sure the API server allows requests from your client origin. You may need to configure CORS in the API server.

## License

This project is part of the gameserver1 repository.