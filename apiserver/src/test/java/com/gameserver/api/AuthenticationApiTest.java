package com.gameserver.api;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticationApiTest extends BaseTest {
    
    private WebClient client;
    private final int port = 8084;
    private String deploymentId;
    private JwtService jwtService;
    
    @BeforeEach
    void deployVerticle(VertxTestContext testContext) {
        client = WebClient.create(vertx);
        
        String dbName = "test_auth_" + System.currentTimeMillis() + "_" + Thread.currentThread().threadId();
        DatabaseService dbService = new DatabaseService(vertx, dbName);
        jwtService = new JwtService(vertx);
        
        ApiServerApplication app = new ApiServerApplication();
        app.setDatabaseService(dbService);
        app.setJwtService(jwtService);
        
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
    void testLoginReturnsAccessToken(VertxTestContext testContext) {
        JsonObject loginRequest = new JsonObject()
            .put("username", "player1")
            .put("password", "password123");
        
        client.post(port, "localhost", "/api/login")
            .sendJsonObject(loginRequest)
            .onSuccess(response -> {
                testContext.verify(() -> {
                    assertEquals(200, response.statusCode());
                    
                    JsonObject responseBody = response.bodyAsJsonObject();
                    assertTrue(responseBody.getBoolean("success"));
                    assertEquals(1, responseBody.getInteger("userId"));
                    assertEquals("player1", responseBody.getString("username"));
                    
                    String accessToken = responseBody.getString("accessToken");
                    assertNotNull(accessToken);
                    assertFalse(accessToken.trim().isEmpty());
                    assertTrue(accessToken.contains("."));
                    
                    // Verify token contains correct user info
                    Integer userId = jwtService.getUserIdFromToken(accessToken);
                    String username = jwtService.getUsernameFromToken(accessToken);
                    assertEquals(1, userId);
                    assertEquals("player1", username);
                });
                testContext.completeNow();
            })
            .onFailure(testContext::failNow);
    }
    
    @Test
    void testInventoryWithValidToken(VertxTestContext testContext) {
        // First login to get token
        JsonObject loginRequest = new JsonObject()
            .put("username", "player1")
            .put("password", "password123");
        
        client.post(port, "localhost", "/api/login")
            .sendJsonObject(loginRequest)
            .onSuccess(loginResponse -> {
                String accessToken = loginResponse.bodyAsJsonObject().getString("accessToken");
                
                // Use token to access inventory
                client.get(port, "localhost", "/api/inventory")
                    .putHeader("Authorization", "Bearer " + accessToken)
                    .send()
                    .onSuccess(inventoryResponse -> {
                        testContext.verify(() -> {
                            assertEquals(200, inventoryResponse.statusCode());
                            
                            JsonObject responseBody = inventoryResponse.bodyAsJsonObject();
                            assertEquals(1, responseBody.getInteger("userId"));
                            assertNotNull(responseBody.getJsonArray("inventory"));
                        });
                        testContext.completeNow();
                    })
                    .onFailure(testContext::failNow);
            })
            .onFailure(testContext::failNow);
    }
    
    @Test
    void testInventoryWithoutToken(VertxTestContext testContext) {
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
    void testInventoryWithInvalidToken(VertxTestContext testContext) {
        client.get(port, "localhost", "/api/inventory")
            .putHeader("Authorization", "Bearer invalid.token.here")
            .send()
            .onSuccess(response -> {
                testContext.verify(() -> {
                    assertEquals(401, response.statusCode());
                    
                    JsonObject responseBody = response.bodyAsJsonObject();
                    assertEquals("Invalid or expired token", responseBody.getString("error"));
                });
                testContext.completeNow();
            })
            .onFailure(testContext::failNow);
    }
    
    @Test
    void testInventoryWithTokenWithoutBearerPrefix(VertxTestContext testContext) {
        // First login to get token
        JsonObject loginRequest = new JsonObject()
            .put("username", "player1")
            .put("password", "password123");
        
        client.post(port, "localhost", "/api/login")
            .sendJsonObject(loginRequest)
            .onSuccess(loginResponse -> {
                String accessToken = loginResponse.bodyAsJsonObject().getString("accessToken");
                
                // Use token without Bearer prefix
                client.get(port, "localhost", "/api/inventory")
                    .putHeader("Authorization", accessToken)
                    .send()
                    .onSuccess(inventoryResponse -> {
                        testContext.verify(() -> {
                            assertEquals(200, inventoryResponse.statusCode());
                            
                            JsonObject responseBody = inventoryResponse.bodyAsJsonObject();
                            assertEquals(1, responseBody.getInteger("userId"));
                        });
                        testContext.completeNow();
                    })
                    .onFailure(testContext::failNow);
            })
            .onFailure(testContext::failNow);
    }
    
    @Test
    void testMultipleUsersAuthentication(VertxTestContext testContext) {
        // Login as player1
        JsonObject loginRequest1 = new JsonObject()
            .put("username", "player1")
            .put("password", "password123");
        
        client.post(port, "localhost", "/api/login")
            .sendJsonObject(loginRequest1)
            .onSuccess(response1 -> {
                String token1 = response1.bodyAsJsonObject().getString("accessToken");
                
                // Login as player2
                JsonObject loginRequest2 = new JsonObject()
                    .put("username", "player2")
                    .put("password", "password456");
                
                client.post(port, "localhost", "/api/login")
                    .sendJsonObject(loginRequest2)
                    .onSuccess(response2 -> {
                        String token2 = response2.bodyAsJsonObject().getString("accessToken");
                        
                        testContext.verify(() -> {
                            // Verify tokens are different
                            assertNotEquals(token1, token2);
                            
                            // Verify each token has correct user info
                            assertEquals(1, jwtService.getUserIdFromToken(token1));
                            assertEquals("player1", jwtService.getUsernameFromToken(token1));
                            
                            assertEquals(2, jwtService.getUserIdFromToken(token2));
                            assertEquals("player2", jwtService.getUsernameFromToken(token2));
                        });
                        testContext.completeNow();
                    })
                    .onFailure(testContext::failNow);
            })
            .onFailure(testContext::failNow);
    }
    
    @Test
    void testTokenExpirationStructure(VertxTestContext testContext) {
        JsonObject loginRequest = new JsonObject()
            .put("username", "admin")
            .put("password", "admin123");
        
        client.post(port, "localhost", "/api/login")
            .sendJsonObject(loginRequest)
            .onSuccess(response -> {
                testContext.verify(() -> {
                    String accessToken = response.bodyAsJsonObject().getString("accessToken");
                    
                    // Verify token is not expired
                    assertFalse(jwtService.isTokenExpired(accessToken));
                    
                    // Verify token expiration is set to 24 hours
                    var claims = jwtService.extractClaims(accessToken);
                    Long iat = claims.getLong("iat");
                    Long exp = claims.getLong("exp");
                    
                    assertNotNull(iat);
                    assertNotNull(exp);
                    
                    // Should expire in 24 hours (86400 seconds)
                    long expectedDuration = exp - iat;
                    assertEquals(86400, expectedDuration, 5); // Allow 5 second tolerance
                });
                testContext.completeNow();
            })
            .onFailure(testContext::failNow);
    }
    
    @Test
    void testCompleteAuthenticationFlow(VertxTestContext testContext) {
        // 1. Login
        JsonObject loginRequest = new JsonObject()
            .put("username", "player2")
            .put("password", "password456");
        
        client.post(port, "localhost", "/api/login")
            .sendJsonObject(loginRequest)
            .onSuccess(loginResponse -> {
                testContext.verify(() -> {
                    assertEquals(200, loginResponse.statusCode());
                    assertTrue(loginResponse.bodyAsJsonObject().getBoolean("success"));
                });
                
                String accessToken = loginResponse.bodyAsJsonObject().getString("accessToken");
                
                // 2. Access protected inventory endpoint
                client.get(port, "localhost", "/api/inventory")
                    .putHeader("Authorization", "Bearer " + accessToken)
                    .send()
                    .onSuccess(inventoryResponse -> {
                        testContext.verify(() -> {
                            assertEquals(200, inventoryResponse.statusCode());
                            
                            JsonObject inventoryBody = inventoryResponse.bodyAsJsonObject();
                            assertEquals(2, inventoryBody.getInteger("userId"));
                            
                            var inventory = inventoryBody.getJsonArray("inventory");
                            assertNotNull(inventory);
                            assertTrue(inventory.size() > 0);
                            
                            // Verify player2's specific items
                            boolean hasMagicStaff = false;
                            for (int i = 0; i < inventory.size(); i++) {
                                JsonObject item = inventory.getJsonObject(i);
                                if ("Magic Staff".equals(item.getString("item_name"))) {
                                    hasMagicStaff = true;
                                    break;
                                }
                            }
                            assertTrue(hasMagicStaff, "Player2 should have Magic Staff");
                        });
                        testContext.completeNow();
                    })
                    .onFailure(testContext::failNow);
            })
            .onFailure(testContext::failNow);
    }
}