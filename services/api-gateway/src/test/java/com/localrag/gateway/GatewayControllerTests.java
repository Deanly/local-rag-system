package com.localrag.gateway;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayControllerTests {
    @Test
    void reportsUpOnlyWhenEveryDownstreamServiceIsUp() {
        assertTrue(GatewayController.allServicesUp(Map.of(
                "sourceRegistry", Map.of("status", "UP"),
                "indexer", Map.of("status", "UP"),
                "retrieval", Map.of("status", "UP"),
                "mcpBridge", Map.of("status", "UP")
        )));
    }

    @Test
    void reportsDownWhenAnyDownstreamServiceIsDown() {
        assertFalse(GatewayController.allServicesUp(Map.of(
                "sourceRegistry", Map.of("status", "UP"),
                "indexer", Map.of("status", "DOWN"),
                "retrieval", Map.of("status", "UP"),
                "mcpBridge", Map.of("status", "UP")
        )));
    }
}
