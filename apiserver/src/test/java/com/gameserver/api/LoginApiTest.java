package com.gameserver.api;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginApiTest extends BaseTest {
    
    private WebClient client;
    private final int port = 8081;
    private String deploymentId;
    
    @BeforeEach
    void deployVerticle(VertxTestContext testContext) {
        client = WebClient.create(vertx);
        
        String dbName = "test_login_" + System.currentTimeMillis() + "_" + Thread.currentThread().threadId();
        DatabaseService dbService = new DatabaseService(vertx, dbName);
        
        ApiServerApplication app = new ApiServerApplication();
        app.setDatabaseService(dbService);
        
        vertx.deployVerticle(app)
            .onSuccess(id -> {
                deploymentId = id;
                // Start HTTP server after database initialization
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
    void testLoginSuccess(VertxTestContext testContext) {
        JsonObject loginRequest = new JsonObject()
            .put("username", "player1")
            .put("password", "password123");
        
        client.post(port, "localhost", "/api/login")
            .sendJsonObject(loginRequest)
            .onSuccess(response -> {
                testContext.verify(() -> {
                    assertEquals(200, response.statusCode());
                    assertEquals("application/json", response.getHeader("content-type"));
                    
                    JsonObject responseBody = response.bodyAsJsonObject();
                    assertNotNull(responseBody);
                    assertTrue(responseBody.getBoolean("success"));
                    assertEquals(1, responseBody.getInteger("userId"));
                    assertEquals("player1", responseBody.getString("username"));
                    
                    // Verify access token is returned
                    String accessToken = responseBody.getString("accessToken");
                    assertNotNull(accessToken);
                    assertFalse(accessToken.trim().isEmpty());
                    assertTrue(accessToken.contains("."));
                });
                testContext.completeNow();
            })
            .onFailure(testContext::failNow);
    }
    
    @Test
    void testLoginInvalidCredentials(VertxTestContext testContext) {
        JsonObject loginRequest = new JsonObject()
            .put("username", "player1")
            .put("password", "wrongpassword");
        
        client.post(port, "localhost", "/api/login")
            .sendJsonObject(loginRequest)
            .onSuccess(response -> {
                testContext.verify(() -> {
                    assertEquals(401, response.statusCode());
                    assertEquals("application/json", response.getHeader("content-type"));
                    
                    JsonObject responseBody = response.bodyAsJsonObject();
                    assertNotNull(responseBody);
                    assertEquals("Invalid credentials", responseBody.getString("error"));
                });
                testContext.completeNow();
            })
            .onFailure(testContext::failNow);
    }
    
    @Test
    void testLoginMissingUsername(VertxTestContext testContext) {
        JsonObject loginRequest = new JsonObject()
            .put("password", "password123");
        
        client.post(port, "localhost", "/api/login")
            .sendJsonObject(loginRequest)
            .onSuccess(response -> {
                testContext.verify(() -> {
                    assertEquals(400, response.statusCode());
                    assertEquals("application/json", response.getHeader("content-type"));
                    
                    JsonObject responseBody = response.bodyAsJsonObject();
                    assertNotNull(responseBody);
                    assertEquals("Username and password are required", responseBody.getString("error"));
                });
                testContext.completeNow();
            })
            .onFailure(testContext::failNow);
    }
    
    @Test
    void testLoginMissingPassword(VertxTestContext testContext) {
        JsonObject loginRequest = new JsonObject()
            .put("username", "player1");
        
        client.post(port, "localhost", "/api/login")
            .sendJsonObject(loginRequest)
            .onSuccess(response -> {
                testContext.verify(() -> {
                    assertEquals(400, response.statusCode());
                    assertEquals("application/json", response.getHeader("content-type"));
                    
                    JsonObject responseBody = response.bodyAsJsonObject();
                    assertNotNull(responseBody);
                    assertEquals("Username and password are required", responseBody.getString("error"));
                });
                testContext.completeNow();
            })
            .onFailure(testContext::failNow);
    }
    
    @Test
    void testLoginEmptyRequestBody(VertxTestContext testContext) {
        client.post(port, "localhost", "/api/login")
            .send()
            .onSuccess(response -> {
                testContext.verify(() -> {
                    assertEquals(400, response.statusCode());
                    assertEquals("application/json", response.getHeader("content-type"));
                    
                    JsonObject responseBody = response.bodyAsJsonObject();
                    assertNotNull(responseBody);
                    assertEquals("Request body is required", responseBody.getString("error"));
                });
                testContext.completeNow();
            })
            .onFailure(testContext::failNow);
    }
    
    @Test
    void testLoginInvalidJson(VertxTestContext testContext) {
        client.post(port, "localhost", "/api/login")
            .putHeader("content-type", "application/json")
            .sendBuffer(Buffer.buffer("invalid json"))
            .onSuccess(response -> {
                testContext.verify(() -> {
                    assertEquals(400, response.statusCode());
                });
                testContext.completeNow();
            })
            .onFailure(testContext::failNow);
    }
    
    @Test
    void testLoginWithAllSampleUsers(VertxTestContext testContext) {
        JsonObject[] users = {
            new JsonObject().put("username", "player1").put("password", "password123"),
            new JsonObject().put("username", "player2").put("password", "password456"),
            new JsonObject().put("username", "admin").put("password", "admin123")
        };
        
        testLoginForUser(users, 0, testContext);
    }
    
    private void testLoginForUser(JsonObject[] users, int index, VertxTestContext testContext) {
        if (index >= users.length) {
            testContext.completeNow();
            return;
        }
        
        JsonObject user = users[index];
        client.post(port, "localhost", "/api/login")
            .sendJsonObject(user)
            .onSuccess(response -> {
                testContext.verify(() -> {
                    assertEquals(200, response.statusCode());
                    JsonObject responseBody = response.bodyAsJsonObject();
                    assertTrue(responseBody.getBoolean("success"));
                    assertEquals(user.getString("username"), responseBody.getString("username"));
                });
                testLoginForUser(users, index + 1, testContext);
            })
            .onFailure(testContext::failNow);
    }
}