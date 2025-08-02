package com.gameserver.api;

import io.vertx.core.json.JsonObject;

public class TestUtils {
    
    public static JsonObject createLoginRequest(String username, String password) {
        return new JsonObject()
            .put("username", username)
            .put("password", password);
    }
    
    public static JsonObject createValidLoginRequest() {
        return createLoginRequest("player1", "password123");
    }
    
    public static JsonObject createInvalidLoginRequest() {
        return createLoginRequest("player1", "wrongpassword");
    }
    
    public static boolean hasItem(io.vertx.core.json.JsonArray inventory, String itemName) {
        for (int i = 0; i < inventory.size(); i++) {
            JsonObject item = inventory.getJsonObject(i);
            if (itemName.equals(item.getString("item_name"))) {
                return true;
            }
        }
        return false;
    }
    
    public static JsonObject getItem(io.vertx.core.json.JsonArray inventory, String itemName) {
        for (int i = 0; i < inventory.size(); i++) {
            JsonObject item = inventory.getJsonObject(i);
            if (itemName.equals(item.getString("item_name"))) {
                return item;
            }
        }
        return null;
    }
    
    public static void assertItemExists(io.vertx.core.json.JsonArray inventory, String itemName, String itemType, int quantity) {
        JsonObject item = getItem(inventory, itemName);
        if (item == null) {
            throw new AssertionError("Item not found: " + itemName);
        }
        
        if (!itemType.equals(item.getString("item_type"))) {
            throw new AssertionError("Expected item type " + itemType + " but got " + item.getString("item_type"));
        }
        
        if (quantity != item.getInteger("quantity")) {
            throw new AssertionError("Expected quantity " + quantity + " but got " + item.getInteger("quantity"));
        }
    }
    
    public static void assertResponseSuccess(JsonObject response, int expectedUserId, String expectedUsername) {
        if (!response.getBoolean("success", false)) {
            throw new AssertionError("Expected successful response");
        }
        
        if (expectedUserId != response.getInteger("userId", -1)) {
            throw new AssertionError("Expected userId " + expectedUserId + " but got " + response.getInteger("userId"));
        }
        
        if (!expectedUsername.equals(response.getString("username"))) {
            throw new AssertionError("Expected username " + expectedUsername + " but got " + response.getString("username"));
        }
    }
    
    public static void assertErrorResponse(JsonObject response, String expectedError) {
        String actualError = response.getString("error");
        if (!expectedError.equals(actualError)) {
            throw new AssertionError("Expected error '" + expectedError + "' but got '" + actualError + "'");
        }
    }
}