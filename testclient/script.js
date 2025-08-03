// Game Client JavaScript
class GameClient {
    constructor() {
        this.apiUrl = 'http://localhost:8080/api';
        this.accessToken = localStorage.getItem('accessToken');
        this.currentUser = JSON.parse(localStorage.getItem('currentUser') || '{}');
        this.inventory = [];
        
        this.init();
    }

    init() {
        this.bindEvents();
        this.loadSettings();
        
        // Check if user is already logged in
        if (this.accessToken && this.currentUser.username) {
            this.showGameScreen();
            this.loadInventory();
        } else {
            this.showLoginScreen();
        }
    }

    bindEvents() {
        // Login form
        const loginForm = document.getElementById('loginForm');
        loginForm.addEventListener('submit', (e) => this.handleLogin(e));

        // Demo account buttons
        const demoButtons = document.querySelectorAll('.btn-demo');
        demoButtons.forEach(btn => {
            btn.addEventListener('click', (e) => this.handleDemoLogin(e));
        });

        // Logout button
        const logoutBtn = document.getElementById('logoutBtn');
        logoutBtn.addEventListener('click', () => this.handleLogout());

        // Refresh button
        const refreshBtn = document.getElementById('refreshBtn');
        refreshBtn.addEventListener('click', () => this.loadInventory());

        // Tab buttons
        const tabButtons = document.querySelectorAll('.tab-btn');
        tabButtons.forEach(btn => {
            btn.addEventListener('click', (e) => this.switchTab(e));
        });

        // Modal close
        const modal = document.getElementById('itemModal');
        const closeBtn = modal.querySelector('.close');
        closeBtn.addEventListener('click', () => this.closeModal());
        
        window.addEventListener('click', (e) => {
            if (e.target === modal) {
                this.closeModal();
            }
        });
    }

    async handleLogin(e) {
        e.preventDefault();
        
        const username = document.getElementById('username').value;
        const password = document.getElementById('password').value;
        const errorDiv = document.getElementById('loginError');
        
        try {
            errorDiv.style.display = 'none';
            
            const response = await fetch(`${this.apiUrl}/login`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ username, password })
            });

            const data = await response.json();

            if (response.ok && data.success) {
                this.accessToken = data.accessToken;
                this.currentUser = {
                    id: data.userId,
                    username: data.username
                };
                
                // Save to localStorage
                localStorage.setItem('accessToken', this.accessToken);
                localStorage.setItem('currentUser', JSON.stringify(this.currentUser));
                
                this.showGameScreen();
                this.updatePlayerInfo();
                this.loadInventory();
            } else {
                this.showError(data.error || 'Login failed');
            }
        } catch (error) {
            console.error('Login error:', error);
            this.showError('Failed to connect to server. Please check if the API server is running.');
        }
    }

    handleDemoLogin(e) {
        const username = e.target.dataset.user;
        const password = e.target.dataset.pass;
        
        document.getElementById('username').value = username;
        document.getElementById('password').value = password;
        
        // Trigger login
        document.getElementById('loginForm').dispatchEvent(new Event('submit'));
    }

    handleLogout() {
        this.accessToken = null;
        this.currentUser = {};
        this.inventory = [];
        
        localStorage.removeItem('accessToken');
        localStorage.removeItem('currentUser');
        
        this.showLoginScreen();
        this.clearForms();
    }

    async loadInventory() {
        if (!this.accessToken) {
            this.showLoginScreen();
            return;
        }

        const inventoryGrid = document.getElementById('inventoryGrid');
        inventoryGrid.innerHTML = '<div class="loading">Loading inventory...</div>';

        try {
            const response = await fetch(`${this.apiUrl}/inventory`, {
                headers: {
                    'Authorization': `Bearer ${this.accessToken}`
                }
            });

            if (response.ok) {
                const data = await response.json();
                this.inventory = data.inventory || [];
                this.renderInventory();
                this.updateStats();
            } else if (response.status === 401) {
                this.showError('Session expired. Please login again.');
                this.handleLogout();
            } else {
                throw new Error('Failed to load inventory');
            }
        } catch (error) {
            console.error('Inventory error:', error);
            inventoryGrid.innerHTML = '<div class="loading">Failed to load inventory. Please try again.</div>';
        }
    }

    renderInventory() {
        const inventoryGrid = document.getElementById('inventoryGrid');
        const itemCount = document.getElementById('itemCount');
        
        if (this.inventory.length === 0) {
            inventoryGrid.innerHTML = '<div class="loading">Your inventory is empty</div>';
            itemCount.textContent = '0 items';
            return;
        }

        itemCount.textContent = `${this.inventory.length} items`;

        const itemsHtml = this.inventory.map(item => {
            const icon = this.getItemIcon(item.item_type);
            const properties = item.properties ? Object.entries(item.properties) : [];
            
            return `
                <div class="inventory-item" onclick="gameClient.showItemDetails(${JSON.stringify(item).replace(/"/g, '&quot;')})">
                    <div class="item-header">
                        <div style="display: flex; align-items: center;">
                            <span class="item-icon">${icon}</span>
                            <div class="item-info">
                                <h3>${item.item_name}</h3>
                                <span class="item-type ${item.item_type}">${item.item_type}</span>
                            </div>
                        </div>
                        <div class="item-quantity">×${item.quantity}</div>
                    </div>
                    ${properties.length > 0 ? `
                        <div class="item-properties">
                            ${properties.map(([key, value]) => `
                                <div class="property">
                                    <span>${this.formatPropertyName(key)}:</span>
                                    <span class="property-value">${value}</span>
                                </div>
                            `).join('')}
                        </div>
                    ` : ''}
                </div>
            `;
        }).join('');

        inventoryGrid.innerHTML = itemsHtml;
    }

    getItemIcon(itemType) {
        const icons = {
            weapon: '⚔️',
            armor: '🛡️',
            consumable: '🧪',
            special: '✨',
            default: '📦'
        };
        return icons[itemType] || icons.default;
    }

    formatPropertyName(name) {
        return name.replace(/_/g, ' ')
                  .replace(/\b\w/g, l => l.toUpperCase());
    }

    showItemDetails(item) {
        const modal = document.getElementById('itemModal');
        const itemDetails = document.getElementById('itemDetails');
        
        const icon = this.getItemIcon(item.item_type);
        const properties = item.properties ? Object.entries(item.properties) : [];
        
        itemDetails.innerHTML = `
            <div style="text-align: center; margin-bottom: 20px;">
                <span style="font-size: 48px;">${icon}</span>
                <h2 style="margin: 10px 0; color: #4a5568;">${item.item_name}</h2>
                <span class="item-type ${item.item_type}">${item.item_type}</span>
                <div style="margin: 15px 0;">
                    <span class="item-quantity">Quantity: ×${item.quantity}</span>
                </div>
            </div>
            ${properties.length > 0 ? `
                <div style="margin-top: 20px;">
                    <h3 style="color: #4a5568; margin-bottom: 15px;">Properties</h3>
                    ${properties.map(([key, value]) => `
                        <div class="property" style="padding: 8px 0; border-bottom: 1px solid #e2e8f0;">
                            <span>${this.formatPropertyName(key)}:</span>
                            <span class="property-value">${value}</span>
                        </div>
                    `).join('')}
                </div>
            ` : ''}
        `;
        
        modal.style.display = 'block';
    }

    closeModal() {
        document.getElementById('itemModal').style.display = 'none';
    }

    updatePlayerInfo() {
        document.getElementById('playerName').textContent = this.currentUser.username;
        document.getElementById('playerId').textContent = `ID: #${this.currentUser.id}`;
        document.getElementById('profileName').textContent = this.currentUser.username;
        document.getElementById('profileId').textContent = `Player ID: #${this.currentUser.id}`;
    }

    updateStats() {
        const stats = {
            total: this.inventory.length,
            weapons: this.inventory.filter(item => item.item_type === 'weapon').length,
            armor: this.inventory.filter(item => item.item_type === 'armor').length,
            consumables: this.inventory.filter(item => item.item_type === 'consumable').length
        };

        document.getElementById('statTotalItems').textContent = stats.total;
        document.getElementById('statWeapons').textContent = stats.weapons;
        document.getElementById('statArmor').textContent = stats.armor;
        document.getElementById('statConsumables').textContent = stats.consumables;
    }

    switchTab(e) {
        const targetTab = e.target.dataset.tab;
        
        // Update tab buttons
        document.querySelectorAll('.tab-btn').forEach(btn => {
            btn.classList.remove('active');
        });
        e.target.classList.add('active');
        
        // Update tab panels
        document.querySelectorAll('.tab-panel').forEach(panel => {
            panel.classList.remove('active');
        });
        document.getElementById(`${targetTab}Tab`).classList.add('active');

        // Load data based on tab
        if (targetTab === 'inventory') {
            this.loadInventory();
        }
    }

    showLoginScreen() {
        document.getElementById('loginScreen').classList.add('active');
        document.getElementById('gameScreen').classList.remove('active');
    }

    showGameScreen() {
        document.getElementById('loginScreen').classList.remove('active');
        document.getElementById('gameScreen').classList.add('active');
        this.updatePlayerInfo();
    }

    showError(message) {
        const errorDiv = document.getElementById('loginError');
        errorDiv.textContent = message;
        errorDiv.style.display = 'block';
    }

    clearForms() {
        document.getElementById('username').value = '';
        document.getElementById('password').value = '';
        document.getElementById('loginError').style.display = 'none';
    }

    loadSettings() {
        const savedApiUrl = localStorage.getItem('apiUrl');
        if (savedApiUrl) {
            this.apiUrl = savedApiUrl;
            document.getElementById('apiUrl').value = savedApiUrl;
        }

        const autoRefresh = localStorage.getItem('autoRefresh');
        if (autoRefresh !== null) {
            document.getElementById('autoRefresh').checked = autoRefresh === 'true';
        }
    }

    saveSettings() {
        const apiUrl = document.getElementById('apiUrl').value;
        const autoRefresh = document.getElementById('autoRefresh').checked;
        
        this.apiUrl = apiUrl;
        localStorage.setItem('apiUrl', apiUrl);
        localStorage.setItem('autoRefresh', autoRefresh.toString());
        
        alert('Settings saved!');
    }
}

// Global function for settings
function saveSettings() {
    gameClient.saveSettings();
}

// Initialize the game client when DOM is loaded
let gameClient;
document.addEventListener('DOMContentLoaded', () => {
    gameClient = new GameClient();
});

// Auto-refresh inventory every 30 seconds if enabled
setInterval(() => {
    const autoRefresh = document.getElementById('autoRefresh');
    if (autoRefresh && autoRefresh.checked && gameClient.accessToken) {
        const currentTab = document.querySelector('.tab-btn.active').dataset.tab;
        if (currentTab === 'inventory') {
            gameClient.loadInventory();
        }
    }
}, 30000);