package com.gameserver.api;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseServiceTest extends BaseTest {
    
    private DatabaseService databaseService;
    
    @BeforeEach
    void setUpDatabase(VertxTestContext testContext) {
        String dbName = "test_db_" + System.currentTimeMillis() + "_" + Thread.currentThread().getId();
        databaseService = new DatabaseService(vertx, dbName);
        databaseService.init()
            .onSuccess(v -> testContext.completeNow())
            .onFailure(testContext::failNow);
    }
    
    @Test
    void testAuthenticateUserSuccess(VertxTestContext testContext) {
        databaseService.authenticateUser("player1", "password123")
            .onSuccess(user -> {
                testContext.verify(() -> {
                    assertNotNull(user, "User should not be null");
                    assertNotNull(user.getInteger("ID"), "User ID should not be null");
                    assertEquals(1, user.getInteger("ID").intValue());
                    assertEquals("player1", user.getString("USERNAME"));
                });
                testContext.completeNow();
            })
            .onFailure(testContext::failNow);
    }
    
    @Test
    void testAuthenticateUserInvalidPassword(VertxTestContext testContext) {
        databaseService.authenticateUser("player1", "wrongpassword")
            .onSuccess(user -> {
                testContext.verify(() -> {
                    assertNull(user);
                });
                testContext.completeNow();
            })
            .onFailure(testContext::failNow);
    }
    
    @Test
    void testAuthenticateUserInvalidUsername(VertxTestContext testContext) {
        databaseService.authenticateUser("nonexistent", "password123")
            .onSuccess(user -> {
                testContext.verify(() -> {
                    assertNull(user);
                });
                testContext.completeNow();
            })
            .onFailure(testContext::failNow);
    }
    
    @Test
    void testGetUserInventorySuccess(VertxTestContext testContext) {
        databaseService.getUserInventory(1)
            .onSuccess(inventory -> {
                testContext.verify(() -> {
                    assertNotNull(inventory);
                    assertTrue(inventory instanceof JsonArray);
                    assertTrue(inventory.size() > 0);
                    
                    JsonObject firstItem = inventory.getJsonObject(0);
                    assertNotNull(firstItem.getString("item_name"));
                    assertNotNull(firstItem.getString("item_type"));
                    assertNotNull(firstItem.getInteger("quantity"));
                });
                testContext.completeNow();
            })
            .onFailure(testContext::failNow);
    }
    
    @Test
    void testGetUserInventoryEmptyForNonexistentUser(VertxTestContext testContext) {
        databaseService.getUserInventory(999)
            .onSuccess(inventory -> {
                testContext.verify(() -> {
                    assertNotNull(inventory);
                    assertTrue(inventory instanceof JsonArray);
                    assertEquals(0, inventory.size());
                });
                testContext.completeNow();
            })
            .onFailure(testContext::failNow);
    }
    
    @Test
    void testGetUserInventorySpecificItems(VertxTestContext testContext) {
        databaseService.getUserInventory(1)
            .onSuccess(inventory -> {
                testContext.verify(() -> {
                    assertNotNull(inventory);
                    assertTrue(inventory.size() >= 3);
                    
                    boolean hasIronSword = false;
                    boolean hasHealthPotion = false;
                    boolean hasLeatherArmor = false;
                    
                    for (int i = 0; i < inventory.size(); i++) {
                        JsonObject item = inventory.getJsonObject(i);
                        String itemName = item.getString("item_name");
                        
                        if ("Iron Sword".equals(itemName)) {
                            hasIronSword = true;
                            assertEquals("weapon", item.getString("item_type"));
                            assertEquals(1, item.getInteger("quantity"));
                            JsonObject properties = item.getJsonObject("properties");
                            assertNotNull(properties);
                            assertEquals(50, properties.getInteger("damage"));
                        } else if ("Health Potion".equals(itemName)) {
                            hasHealthPotion = true;
                            assertEquals("consumable", item.getString("item_type"));
                            assertEquals(5, item.getInteger("quantity"));
                        } else if ("Leather Armor".equals(itemName)) {
                            hasLeatherArmor = true;
                            assertEquals("armor", item.getString("item_type"));
                            assertEquals(1, item.getInteger("quantity"));
                        }
                    }
                    
                    assertTrue(hasIronSword, "Should have Iron Sword");
                    assertTrue(hasHealthPotion, "Should have Health Potion");
                    assertTrue(hasLeatherArmor, "Should have Leather Armor");
                });
                testContext.completeNow();
            })
            .onFailure(testContext::failNow);
    }
}