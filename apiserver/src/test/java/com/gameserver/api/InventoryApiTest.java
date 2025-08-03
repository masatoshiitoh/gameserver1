package com.gameserver.api;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InventoryApiTest extends BaseTest {
    
    private WebClient client;
    private final int port = 8082;
    private String deploymentId;
    
    private void loginAndExecute(String username, String password, 
                                 java.util.function.Consumer<String> onSuccess, 
                                 VertxTestContext testContext) {
        JsonObject loginRequest = new JsonObject()
            .put("username", username)
            .put("password", password);
        
        client.post(port, "localhost", "/api/login")
            .sendJsonObject(loginRequest)
            .onSuccess(loginResponse -> {
                String accessToken = loginResponse.bodyAsJsonObject().getString("accessToken");
                onSuccess.accept(accessToken);
            })
            .onFailure(testContext::failNow);
    }
    
    @BeforeEach
    void deployVerticle(VertxTestContext testContext) {
        client = WebClient.create(vertx);
        
        String dbName = "test_inventory_" + System.currentTimeMillis() + "_" + Thread.currentThread().threadId();
        DatabaseService dbService = new DatabaseService(vertx, dbName);
        
        ApiServerApplication app = new ApiServerApplication();
        app.setDatabaseService(dbService);
        
        vertx.deployVerticle(app)
            .onSuccess(id -> {
                deploymentId = id;
                vertx.createHttpServer()
                    .requestHandler(app.createRouter())
                    .listen(port)
                    .onSuccess(server -> testContext.completeNow())
                    .onFailure(testContext::failNow);
            })
            .onFailure(testContext::failNow);
    }
    
    @AfterEach
    void undeployVerticle(VertxTestContext testContext) {
        if (deploymentId != null) {
            vertx.undeploy(deploymentId)
                .onSuccess(v -> testContext.completeNow())
                .onFailure(testContext::failNow);
        } else {
            testContext.completeNow();
        }
    }
    
    @Test
    void testGetInventorySuccess(VertxTestContext testContext) {
        // First login to get token
        JsonObject loginRequest = new JsonObject()
            .put("username", "player1")
            .put("password", "password123");
        
        client.post(port, "localhost", "/api/login")
            .sendJsonObject(loginRequest)
            .onSuccess(loginResponse -> {
                String accessToken = loginResponse.bodyAsJsonObject().getString("accessToken");
                
                client.get(port, "localhost", "/api/inventory")
                    .putHeader("Authorization", "Bearer " + accessToken)
                    .send()
                    .onSuccess(response -> {
                        testContext.verify(() -> {
                            assertEquals(200, response.statusCode());
                            assertEquals("application/json", response.getHeader("content-type"));
                            
                            JsonObject responseBody = response.bodyAsJsonObject();
                            assertNotNull(responseBody);
                            assertEquals(1, responseBody.getInteger("userId"));
                            
                            JsonArray inventory = responseBody.getJsonArray("inventory");
                            assertNotNull(inventory);
                            assertTrue(inventory.size() > 0);
                            
                            JsonObject firstItem = inventory.getJsonObject(0);
                            assertNotNull(firstItem.getString("item_name"));
                            assertNotNull(firstItem.getString("item_type"));
                            assertNotNull(firstItem.getInteger("quantity"));
                        });
                        testContext.completeNow();
                    })
                    .onFailure(testContext::failNow);
            })
            .onFailure(testContext::failNow);
    }
    
    @Test
    void testGetInventoryPlayer1Items(VertxTestContext testContext) {
        loginAndExecute("player1", "password123", accessToken -> {
            client.get(port, "localhost", "/api/inventory")
                .putHeader("Authorization", "Bearer " + accessToken)
                .send()
                .onSuccess(response -> {
                    testContext.verify(() -> {
                        assertEquals(200, response.statusCode());
                        
                        JsonObject responseBody = response.bodyAsJsonObject();
                        JsonArray inventory = responseBody.getJsonArray("inventory");
                        
                        boolean hasIronSword = false;
                        boolean hasHealthPotion = false;
                        boolean hasLeatherArmor = false;
                        
                        for (int i = 0; i < inventory.size(); i++) {
                            JsonObject item = inventory.getJsonObject(i);
                            String itemName = item.getString("item_name");
                            
                            switch (itemName) {
                                case "Iron Sword" -> {
                                    hasIronSword = true;
                                    assertEquals("weapon", item.getString("item_type"));
                                    assertEquals(1, item.getInteger("quantity"));
                                    JsonObject properties = item.getJsonObject("properties");
                                    assertNotNull(properties);
                                    assertEquals(50, properties.getInteger("damage"));
                                    assertEquals(100, properties.getInteger("durability"));
                                }
                                case "Health Potion" -> {
                                    hasHealthPotion = true;
                                    assertEquals("consumable", item.getString("item_type"));
                                    assertEquals(5, item.getInteger("quantity"));
                                    JsonObject properties = item.getJsonObject("properties");
                                    assertNotNull(properties);
                                    assertEquals(25, properties.getInteger("healing"));
                                }
                                case "Leather Armor" -> {
                                    hasLeatherArmor = true;
                                    assertEquals("armor", item.getString("item_type"));
                                    assertEquals(1, item.getInteger("quantity"));
                                    JsonObject properties = item.getJsonObject("properties");
                                    assertNotNull(properties);
                                    assertEquals(20, properties.getInteger("defense"));
                                    assertEquals(80, properties.getInteger("durability"));
                                }
                            }
                        }
                        
                        assertTrue(hasIronSword, "Player 1 should have Iron Sword");
                        assertTrue(hasHealthPotion, "Player 1 should have Health Potion");
                        assertTrue(hasLeatherArmor, "Player 1 should have Leather Armor");
                    });
                    testContext.completeNow();
                })
                .onFailure(testContext::failNow);
        }, testContext);
    }
    
    @Test
    void testGetInventoryPlayer2Items(VertxTestContext testContext) {
        loginAndExecute("player2", "password456", accessToken -> {
            client.get(port, "localhost", "/api/inventory")
                .putHeader("Authorization", "Bearer " + accessToken)
                .send()
                .onSuccess(response -> {
                    testContext.verify(() -> {
                        assertEquals(200, response.statusCode());
                        
                        JsonObject responseBody = response.bodyAsJsonObject();
                        JsonArray inventory = responseBody.getJsonArray("inventory");
                        
                        boolean hasMagicStaff = false;
                        boolean hasManaPotion = false;
                        
                        for (int i = 0; i < inventory.size(); i++) {
                            JsonObject item = inventory.getJsonObject(i);
                            String itemName = item.getString("item_name");
                            
                            if ("Magic Staff".equals(itemName)) {
                                hasMagicStaff = true;
                                assertEquals("weapon", item.getString("item_type"));
                                assertEquals(1, item.getInteger("quantity"));
                                JsonObject properties = item.getJsonObject("properties");
                                assertNotNull(properties);
                                assertEquals(75, properties.getInteger("damage"));
                                assertEquals(10, properties.getInteger("mana_cost"));
                            } else if ("Mana Potion".equals(itemName)) {
                                hasManaPotion = true;
                                assertEquals("consumable", item.getString("item_type"));
                                assertEquals(3, item.getInteger("quantity"));
                                JsonObject properties = item.getJsonObject("properties");
                                assertNotNull(properties);
                                assertEquals(50, properties.getInteger("mana_restore"));
                            }
                        }
                        
                        assertTrue(hasMagicStaff, "Player 2 should have Magic Staff");
                        assertTrue(hasManaPotion, "Player 2 should have Mana Potion");
                    });
                    testContext.completeNow();
                })
                .onFailure(testContext::failNow);
        }, testContext);
    }
    
    @Test
    void testGetInventoryAdminItems(VertxTestContext testContext) {
        loginAndExecute("admin", "admin123", accessToken -> {
            client.get(port, "localhost", "/api/inventory")
                .putHeader("Authorization", "Bearer " + accessToken)
                .send()
                .onSuccess(response -> {
                    testContext.verify(() -> {
                        assertEquals(200, response.statusCode());
                        
                        JsonObject responseBody = response.bodyAsJsonObject();
                        JsonArray inventory = responseBody.getJsonArray("inventory");
                        
                        boolean hasAdminKey = false;
                        
                        for (int i = 0; i < inventory.size(); i++) {
                            JsonObject item = inventory.getJsonObject(i);
                            String itemName = item.getString("item_name");
                            
                            if ("Admin Key".equals(itemName)) {
                                hasAdminKey = true;
                                assertEquals("special", item.getString("item_type"));
                                assertEquals(1, item.getInteger("quantity"));
                                JsonObject properties = item.getJsonObject("properties");
                                assertNotNull(properties);
                                assertEquals("admin", properties.getString("access_level"));
                            }
                        }
                        
                        assertTrue(hasAdminKey, "Admin should have Admin Key");
                    });
                    testContext.completeNow();
                })
                .onFailure(testContext::failNow);
        }, testContext);
    }
    
    @Test
    void testGetInventoryWithoutAuthentication(VertxTestContext testContext) {
        client.get(port, "localhost", "/api/inventory")
            .send()
            .onSuccess(response -> {
                testContext.verify(() -> {
                    assertEquals(401, response.statusCode());
                    
                    JsonObject responseBody = response.bodyAsJsonObject();
                    assertEquals("Authorization header is required", responseBody.getString("error"));
                });
                testContext.completeNow();
            })
            .onFailure(testContext::failNow);
    }
    
    
    
    @Test
    void testInventoryResponseStructure(VertxTestContext testContext) {
        loginAndExecute("player1", "password123", accessToken -> {
            client.get(port, "localhost", "/api/inventory")
                .putHeader("Authorization", "Bearer " + accessToken)
                .send()
                .onSuccess(response -> {
                    testContext.verify(() -> {
                        JsonObject responseBody = response.bodyAsJsonObject();
                        
                        assertTrue(responseBody.containsKey("userId"));
                        assertTrue(responseBody.containsKey("inventory"));
                        
                        JsonArray inventory = responseBody.getJsonArray("inventory");
                        if (inventory.size() > 0) {
                            JsonObject item = inventory.getJsonObject(0);
                            assertTrue(item.containsKey("item_name"));
                            assertTrue(item.containsKey("item_type"));
                            assertTrue(item.containsKey("quantity"));
                            assertTrue(item.containsKey("properties"));
                        }
                    });
                    testContext.completeNow();
                })
                .onFailure(testContext::failNow);
        }, testContext);
    }
}