package com.gameserver.api;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IntegrationTest extends BaseTest {
    
    private WebClient client;
    private int port = 8083;
    private String deploymentId;
    
    @BeforeEach
    void deployVerticle(VertxTestContext testContext) {
        client = WebClient.create(vertx);
        
        String dbName = "test_integration_" + System.currentTimeMillis() + "_" + Thread.currentThread().getId();
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
    void testFullUserWorkflow(VertxTestContext testContext) {
        JsonObject loginRequest = TestUtils.createValidLoginRequest();
        
        client.post(port, "localhost", "/api/login")
            .sendJsonObject(loginRequest)
            .onSuccess(loginResponse -> {
                testContext.verify(() -> {
                    assertEquals(200, loginResponse.statusCode());
                    JsonObject loginBody = loginResponse.bodyAsJsonObject();
                    TestUtils.assertResponseSuccess(loginBody, 1, "player1");
                });
                
                String accessToken = loginResponse.bodyAsJsonObject().getString("accessToken");
                int userId = loginResponse.bodyAsJsonObject().getInteger("userId");
                
                client.get(port, "localhost", "/api/inventory")
                    .putHeader("Authorization", "Bearer " + accessToken)
                    .send()
                    .onSuccess(inventoryResponse -> {
                        testContext.verify(() -> {
                            assertEquals(200, inventoryResponse.statusCode());
                            JsonObject inventoryBody = inventoryResponse.bodyAsJsonObject();
                            assertEquals(userId, inventoryBody.getInteger("userId"));
                            
                            JsonArray inventory = inventoryBody.getJsonArray("inventory");
                            assertNotNull(inventory);
                            assertTrue(inventory.size() > 0);
                            
                            TestUtils.assertItemExists(inventory, "Iron Sword", "weapon", 1);
                            TestUtils.assertItemExists(inventory, "Health Potion", "consumable", 5);
                            TestUtils.assertItemExists(inventory, "Leather Armor", "armor", 1);
                        });
                        testContext.completeNow();
                    })
                    .onFailure(testContext::failNow);
            })
            .onFailure(testContext::failNow);
    }
    
    @Test
    void testLoginThenInventoryForAllUsers(VertxTestContext testContext) {
        testUserWorkflow("player1", "password123", 1, testContext, () -> 
            testUserWorkflow("player2", "password456", 2, testContext, () ->
                testUserWorkflow("admin", "admin123", 3, testContext, testContext::completeNow)
            )
        );
    }
    
    private void testUserWorkflow(String username, String password, int expectedUserId, 
                                  VertxTestContext testContext, Runnable onComplete) {
        JsonObject loginRequest = TestUtils.createLoginRequest(username, password);
        
        client.post(port, "localhost", "/api/login")
            .sendJsonObject(loginRequest)
            .onSuccess(loginResponse -> {
                testContext.verify(() -> {
                    assertEquals(200, loginResponse.statusCode());
                    JsonObject loginBody = loginResponse.bodyAsJsonObject();
                    TestUtils.assertResponseSuccess(loginBody, expectedUserId, username);
                });
                
                String accessToken = loginResponse.bodyAsJsonObject().getString("accessToken");
                
                client.get(port, "localhost", "/api/inventory")
                    .putHeader("Authorization", "Bearer " + accessToken)
                    .send()
                    .onSuccess(inventoryResponse -> {
                        testContext.verify(() -> {
                            assertEquals(200, inventoryResponse.statusCode());
                            JsonObject inventoryBody = inventoryResponse.bodyAsJsonObject();
                            assertEquals(expectedUserId, inventoryBody.getInteger("userId"));
                            
                            JsonArray inventory = inventoryBody.getJsonArray("inventory");
                            assertNotNull(inventory);
                            assertTrue(inventory.size() > 0);
                        });
                        onComplete.run();
                    })
                    .onFailure(testContext::failNow);
            })
            .onFailure(testContext::failNow);
    }
    
    @Test
    void testInvalidLoginFollowedByValidInventoryRequest(VertxTestContext testContext) {
        JsonObject invalidLogin = TestUtils.createInvalidLoginRequest();
        
        client.post(port, "localhost", "/api/login")
            .sendJsonObject(invalidLogin)
            .onSuccess(loginResponse -> {
                testContext.verify(() -> {
                    assertEquals(401, loginResponse.statusCode());
                    JsonObject loginBody = loginResponse.bodyAsJsonObject();
                    TestUtils.assertErrorResponse(loginBody, "Invalid credentials");
                });
                
                JsonObject validLoginRequest = TestUtils.createValidLoginRequest();
                
                client.post(port, "localhost", "/api/login")
                    .sendJsonObject(validLoginRequest)
                    .onSuccess(validLoginResponse -> {
                        String validAccessToken = validLoginResponse.bodyAsJsonObject().getString("accessToken");
                        
                        client.get(port, "localhost", "/api/inventory")
                            .putHeader("Authorization", "Bearer " + validAccessToken)
                            .send()
                            .onSuccess(inventoryResponse -> {
                                testContext.verify(() -> {
                                    assertEquals(200, inventoryResponse.statusCode());
                                    JsonObject inventoryBody = inventoryResponse.bodyAsJsonObject();
                                    assertEquals(1, inventoryBody.getInteger("userId"));
                                });
                                testContext.completeNow();
                            })
                            .onFailure(testContext::failNow);
                    })
                    .onFailure(testContext::failNow);
            })
            .onFailure(testContext::failNow);
    }
}