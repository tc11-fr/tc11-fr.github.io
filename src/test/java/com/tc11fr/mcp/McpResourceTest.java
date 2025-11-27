package com.tc11fr.mcp;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

/**
 * Unit tests for the MCP Resource endpoint.
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
                .body("protocolVersion", is("1.0"))
                .body("enabled", is(true))
                .body("capabilities", notNullValue())
                .body("capabilities.tools", is(true))
                .body("capabilities.resources", is(true))
                .body("capabilities.prompts", is(true));
    }

    @Test
    void testPostInitialize() {
        given()
            .contentType("application/json")
            .body("{\"type\":\"initialize\",\"id\":\"test-1\",\"params\":{}}")
            .when().post("/mcp")
            .then()
                .statusCode(200)
                .body("type", is("initialize_result"))
                .body("id", is("test-1"))
                .body("result.serverName", is("tc11-mcp-server"))
                .body("result.serverVersion", is("1.0.0"));
    }

    @Test
    void testPostPing() {
        given()
            .contentType("application/json")
            .body("{\"type\":\"ping\",\"id\":\"ping-1\",\"params\":{}}")
            .when().post("/mcp")
            .then()
                .statusCode(200)
                .body("type", is("pong"))
                .body("id", is("ping-1"));
    }

    @Test
    void testPostToolsList() {
        given()
            .contentType("application/json")
            .body("{\"type\":\"tools/list\",\"id\":\"tools-1\",\"params\":{}}")
            .when().post("/mcp")
            .then()
                .statusCode(200)
                .body("type", is("tools/list_result"))
                .body("id", is("tools-1"))
                .body("result.tools", notNullValue());
    }

    @Test
    void testPostUnknownType() {
        given()
            .contentType("application/json")
            .body("{\"type\":\"unknown\",\"id\":\"unk-1\",\"params\":{}}")
            .when().post("/mcp")
            .then()
                .statusCode(200)
                .body("type", is("ack"))
                .body("id", is("unk-1"))
                .body("result.message", is("Message received"))
                .body("result.receivedType", is("unknown"));
    }
}
