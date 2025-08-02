package com.gameserver.api;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public abstract class BaseTest {
    
    protected Vertx vertx;
    
    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext testContext) {
        this.vertx = vertx;
        testContext.completeNow();
    }
    
    @AfterEach
    void tearDown(VertxTestContext testContext) {
        if (vertx != null) {
            vertx.close(testContext.succeeding(response -> testContext.completeNow()));
        } else {
            testContext.completeNow();
        }
    }
}