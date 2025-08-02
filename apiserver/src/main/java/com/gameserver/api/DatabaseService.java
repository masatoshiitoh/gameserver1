package com.gameserver.api;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;

public class DatabaseService {
    
    private final Vertx vertx;
    private SQLClient client;
    
    public DatabaseService(Vertx vertx) {
        this.vertx = vertx;
    }
    
    public DatabaseService(Vertx vertx, String dbName) {
        this.vertx = vertx;
        this.dbName = dbName;
    }
    
    private String dbName = "gameserver";
    
    public Future<Void> init() {
        Promise<Void> promise = Promise.promise();
        
        JsonObject config = new JsonObject()
            .put("url", "jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1")
            .put("driver_class", "org.h2.Driver")
            .put("user", "sa")
            .put("password", "");
        
        client = JDBCClient.createShared(vertx, config);
        
        createTables()
            .compose(v -> insertSampleData())
            .onSuccess(v -> promise.complete())
            .onFailure(promise::fail);
        
        return promise.future();
    }
    
    private Future<Void> createTables() {
        Promise<Void> promise = Promise.promise();
        
        String createUsersTable = """
            CREATE TABLE IF NOT EXISTS users (
                id INT AUTO_INCREMENT PRIMARY KEY,
                username VARCHAR(50) UNIQUE NOT NULL,
                password VARCHAR(255) NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
        
        String createInventoryTable = """
            CREATE TABLE IF NOT EXISTS inventory (
                id INT AUTO_INCREMENT PRIMARY KEY,
                user_id INT NOT NULL,
                item_name VARCHAR(100) NOT NULL,
                item_type VARCHAR(50) NOT NULL,
                quantity INT DEFAULT 1,
                properties VARCHAR(1000),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(id)
            )
        """;
        
        client.getConnection(connResult -> {
            if (connResult.succeeded()) {
                SQLConnection connection = connResult.result();
                
                connection.execute(createUsersTable, userTableResult -> {
                    if (userTableResult.succeeded()) {
                        connection.execute(createInventoryTable, inventoryTableResult -> {
                            connection.close();
                            if (inventoryTableResult.succeeded()) {
                                promise.complete();
                            } else {
                                promise.fail(inventoryTableResult.cause());
                            }
                        });
                    } else {
                        connection.close();
                        promise.fail(userTableResult.cause());
                    }
                });
            } else {
                promise.fail(connResult.cause());
            }
        });
        
        return promise.future();
    }
    
    private Future<Void> insertSampleData() {
        Promise<Void> promise = Promise.promise();
        
        String insertUsers = """
            INSERT INTO users (username, password) VALUES 
            ('player1', 'password123'),
            ('player2', 'password456'),
            ('admin', 'admin123')
        """;
        
        String insertInventory = """
            INSERT INTO inventory (user_id, item_name, item_type, quantity, properties) VALUES 
            (1, 'Iron Sword', 'weapon', 1, '{"damage": 50, "durability": 100}'),
            (1, 'Health Potion', 'consumable', 5, '{"healing": 25}'),
            (1, 'Leather Armor', 'armor', 1, '{"defense": 20, "durability": 80}'),
            (2, 'Magic Staff', 'weapon', 1, '{"damage": 75, "mana_cost": 10}'),
            (2, 'Mana Potion', 'consumable', 3, '{"mana_restore": 50}'),
            (3, 'Admin Key', 'special', 1, '{"access_level": "admin"}')
        """;
        
        client.getConnection(connResult -> {
            if (connResult.succeeded()) {
                SQLConnection connection = connResult.result();
                
                connection.execute(insertUsers, userInsertResult -> {
                    if (userInsertResult.succeeded()) {
                        connection.execute(insertInventory, inventoryInsertResult -> {
                            connection.close();
                            if (inventoryInsertResult.succeeded()) {
                                promise.complete();
                            } else {
                                promise.fail(inventoryInsertResult.cause());
                            }
                        });
                    } else {
                        connection.close();
                        promise.fail(userInsertResult.cause());
                    }
                });
            } else {
                promise.fail(connResult.cause());
            }
        });
        
        return promise.future();
    }
    
    public Future<JsonObject> authenticateUser(String username, String password) {
        Promise<JsonObject> promise = Promise.promise();
        
        String query = "SELECT id, username FROM users WHERE username = ? AND password = ?";
        JsonArray params = new JsonArray().add(username).add(password);
        
        client.queryWithParams(query, params, result -> {
            if (result.succeeded()) {
                var rows = result.result().getRows();
                if (rows.size() > 0) {
                    promise.complete(rows.get(0));
                } else {
                    promise.complete(null);
                }
            } else {
                promise.fail(result.cause());
            }
        });
        
        return promise.future();
    }
    
    public Future<JsonArray> getUserInventory(int userId) {
        Promise<JsonArray> promise = Promise.promise();
        
        String query = """
            SELECT item_name, item_type, quantity, properties 
            FROM inventory 
            WHERE user_id = ?
            ORDER BY item_name
        """;
        JsonArray params = new JsonArray().add(userId);
        
        client.queryWithParams(query, params, result -> {
            if (result.succeeded()) {
                JsonArray inventory = new JsonArray();
                for (JsonObject item : result.result().getRows()) {
                    String propertiesStr = item.getString("PROPERTIES");
                    
                    JsonObject normalizedItem = new JsonObject()
                        .put("item_name", item.getString("ITEM_NAME"))
                        .put("item_type", item.getString("ITEM_TYPE"))
                        .put("quantity", item.getInteger("QUANTITY"));
                    
                    if (propertiesStr != null && !propertiesStr.trim().isEmpty()) {
                        try {
                            JsonObject properties = new JsonObject(propertiesStr);
                            normalizedItem.put("properties", properties);
                        } catch (Exception e) {
                            normalizedItem.put("properties", new JsonObject());
                        }
                    } else {
                        normalizedItem.put("properties", new JsonObject());
                    }
                    inventory.add(normalizedItem);
                }
                promise.complete(inventory);
            } else {
                promise.fail(result.cause());
            }
        });
        
        return promise.future();
    }
}