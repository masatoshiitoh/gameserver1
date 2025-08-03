package com.gameserver.api;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;

public class ApiServerApplication extends AbstractVerticle {

    private DatabaseService databaseService;
    private JwtService jwtService;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        if (databaseService == null) {
            databaseService = new DatabaseService(vertx);
        }
        if (jwtService == null) {
            jwtService = new JwtService(vertx);
        }
        
        databaseService.init()
            .onSuccess(v -> {
                System.out.println("Database initialized successfully");
                startPromise.complete();
            })
            .onFailure(startPromise::fail);
    }

    protected Router createRouter() {
        Router router = Router.router(vertx);
        
        router.route().handler(CorsHandler.create().addRelativeOrigin(".*"));
        router.route().handler(BodyHandler.create());
        
        router.post("/api/login").handler(this::handleLogin);
        router.get("/api/inventory").handler(this::authenticateToken).handler(this::handleGetInventory);
        
        router.route().failureHandler(this::handleFailure);
        
        return router;
    }

    private void handleLogin(RoutingContext context) {
        JsonObject requestBody = context.getBodyAsJson();
        
        if (requestBody == null) {
            context.response()
                .setStatusCode(400)
                .putHeader("content-type", "application/json")
                .end(new JsonObject().put("error", "Request body is required").encode());
            return;
        }
        
        String username = requestBody.getString("username");
        String password = requestBody.getString("password");
        
        if (username == null || password == null) {
            context.response()
                .setStatusCode(400)
                .putHeader("content-type", "application/json")
                .end(new JsonObject().put("error", "Username and password are required").encode());
            return;
        }
        
        databaseService.authenticateUser(username, password)
            .onSuccess(user -> {
                if (user != null) {
                    int userId = user.getInteger("ID");
                    String userUsername = user.getString("USERNAME");
                    String accessToken = jwtService.generateToken(userId, userUsername);
                    
                    JsonObject response = new JsonObject()
                        .put("success", true)
                        .put("userId", userId)
                        .put("username", userUsername)
                        .put("accessToken", accessToken);
                    
                    context.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json")
                        .end(response.encode());
                } else {
                    context.response()
                        .setStatusCode(401)
                        .putHeader("content-type", "application/json")
                        .end(new JsonObject().put("error", "Invalid credentials").encode());
                }
            })
            .onFailure(error -> {
                context.response()
                    .setStatusCode(500)
                    .putHeader("content-type", "application/json")
                    .end(new JsonObject().put("error", "Internal server error").encode());
            });
    }

    private void authenticateToken(RoutingContext context) {
        String authHeader = context.request().getHeader("Authorization");
        
        if (authHeader == null || authHeader.trim().isEmpty()) {
            context.response()
                .setStatusCode(401)
                .putHeader("content-type", "application/json")
                .end(new JsonObject().put("error", "Authorization header is required").encode());
            return;
        }
        
        jwtService.validateToken(authHeader)
            .onSuccess(user -> {
                // Store user info in context for later use
                context.put("userId", jwtService.getUserIdFromToken(authHeader));
                context.put("username", jwtService.getUsernameFromToken(authHeader));
                context.next();
            })
            .onFailure(error -> {
                context.response()
                    .setStatusCode(401)
                    .putHeader("content-type", "application/json")
                    .end(new JsonObject().put("error", "Invalid or expired token").encode());
            });
    }

    private void handleGetInventory(RoutingContext context) {
        Integer userId = context.get("userId");
        
        if (userId == null) {
            context.response()
                .setStatusCode(400)
                .putHeader("content-type", "application/json")
                .end(new JsonObject().put("error", "User ID not found in token").encode());
            return;
        }
        
        databaseService.getUserInventory(userId)
            .onSuccess(inventory -> {
                JsonObject response = new JsonObject()
                    .put("userId", userId)
                    .put("inventory", inventory);
                
                context.response()
                    .setStatusCode(200)
                    .putHeader("content-type", "application/json")
                    .end(response.encode());
            })
            .onFailure(error -> {
                context.response()
                    .setStatusCode(500)
                    .putHeader("content-type", "application/json")
                    .end(new JsonObject().put("error", "Failed to retrieve inventory").encode());
            });
    }

    private void handleFailure(RoutingContext context) {
        Throwable failure = context.failure();
        int statusCode = context.statusCode();
        
        // Handle JSON parsing errors
        if (failure != null && failure.getMessage() != null && 
            (failure.getMessage().contains("JSON") || failure.getMessage().contains("parse"))) {
            statusCode = 400;
        } else if (statusCode == -1) {
            statusCode = 500;
        }
        
        JsonObject errorResponse = new JsonObject()
            .put("error", failure != null ? failure.getMessage() : "Unknown error");
        
        context.response()
            .setStatusCode(statusCode)
            .putHeader("content-type", "application/json")
            .end(errorResponse.encode());
    }

    public void setDatabaseService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    public void setJwtService(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        ApiServerApplication app = new ApiServerApplication();
        
        vertx.deployVerticle(app)
            .onSuccess(id -> {
                vertx.createHttpServer()
                    .requestHandler(app.createRouter())
                    .listen(8080)
                    .onSuccess(server -> {
                        System.out.println("HTTP server started on port " + server.actualPort());
                        System.out.println("API available at: http://localhost:" + server.actualPort() + "/api");
                    })
                    .onFailure(error -> {
                        System.err.println("Failed to start HTTP server: " + error.getMessage());
                        vertx.close();
                    });
            })
            .onFailure(error -> {
                System.err.println("Failed to deploy verticle: " + error.getMessage());
                vertx.close();
            });
    }
}