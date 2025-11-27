package com.tc11fr.mcp;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * MCP (Model Context Protocol) Resource for GitHub Copilot Coding Agent integration.
 * 
 * This JAX-RS resource exposes MCP-compatible HTTP endpoints that allow the
 * GitHub Copilot local coding agent to connect via the MCP protocol during development.
 * 
 * Reference: https://docs.github.com/en/enterprise-cloud@latest/copilot/how-tos/use-copilot-agents/coding-agent/extend-coding-agent-with-mcp
 */
@Path("/mcp")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class McpResource {

    /**
     * GET /mcp - Returns the MCP server status.
     * This endpoint allows agents to check if the MCP server is running and available.
     * 
     * @return JSON response with server status
     */
    @GET
    public McpStatusResponse getStatus() {
        return new McpStatusResponse(
            "ok",
            "TC11 MCP Server",
            "1.0.0",
            true
        );
    }

    /**
     * POST /mcp - Accepts agent messages and returns an acknowledgement.
     * This is a placeholder endpoint implementing the MCP HTTP surface so users
     * can run the Copilot coding agent locally and connect.
     * 
     * @param request The incoming MCP message from the agent
     * @return JSON response acknowledging the message
     */
    @POST
    public Response handleMessage(McpRequest request) {
        McpResponse response = new McpResponse(
            request.id() != null ? request.id() : "unknown",
            "acknowledged",
            "Message received by TC11 MCP Server"
        );
        return Response.ok(response).build();
    }

    /**
     * MCP Status Response POJO.
     */
    public record McpStatusResponse(
        String status,
        String serverName,
        String version,
        boolean mcpEnabled
    ) {}

    /**
     * MCP Request POJO for incoming agent messages.
     */
    public record McpRequest(
        String id,
        String method,
        Object params
    ) {}

    /**
     * MCP Response POJO for outgoing acknowledgements.
     */
    public record McpResponse(
        String id,
        String status,
        String message
    ) {}
}
