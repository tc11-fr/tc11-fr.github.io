package com.tc11fr.mcp;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

/**
 * Unit tests for the MCP Resource endpoint.
 * These tests verify the MCP server endpoints are working correctly.
 */
@QuarkusTest
class McpResourceTest {

    @Test
    void testGetMcpStatus() {
        given()
            .when().get("/mcp")
            .then()
                .statusCode(200)
                .body("status", is("ok"))
                .body("serverName", is("TC11 MCP Server"))
                .body("version", is("1.0.0"))
                .body("mcpEnabled", is(true));
    }

    @Test
    void testPostMcpMessage() {
        given()
            .contentType("application/json")
            .body("{\"id\": \"test-1\", \"method\": \"test\", \"params\": {}}")
            .when().post("/mcp")
            .then()
                .statusCode(200)
                .body("id", is("test-1"))
                .body("status", is("acknowledged"))
                .body("message", notNullValue());
    }

    @Test
    void testPostMcpMessageWithoutId() {
        given()
            .contentType("application/json")
            .body("{\"method\": \"test\", \"params\": {}}")
            .when().post("/mcp")
            .then()
                .statusCode(200)
                .body("id", is("unknown"))
                .body("status", is("acknowledged"));
    }
}
