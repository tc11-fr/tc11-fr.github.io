package com.tc11fr.mcp;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * MCP (Model Context Protocol) Server Resource.
 * 
 * This resource provides a minimal MCP-compatible HTTP endpoint for the 
 * GitHub Copilot coding agent to connect during development.
 * 
 * Reference: https://docs.github.com/en/enterprise-cloud@latest/copilot/how-tos/use-copilot-agents/coding-agent/extend-coding-agent-with-mcp
 */
@Path("/mcp")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class McpResource {

    @Inject
    @ConfigProperty(name = "mcp.server.enabled", defaultValue = "true")
    boolean mcpEnabled;

    @Inject
    @ConfigProperty(name = "mcp.server.path", defaultValue = "/mcp")
    String mcpPath;

    @Inject
    @ConfigProperty(name = "mcp.server.port", defaultValue = "8080")
    int mcpPort;

    /**
     * GET /mcp - Returns the server status and capabilities.
     * Used by the Copilot coding agent to verify the MCP server is running.
     */
    @GET
    public McpStatusResponse getStatus() {
        return new McpStatusResponse(
            "ok",
            "1.0",
            mcpEnabled,
            new McpCapabilities(true, true, true)
        );
    }

    /**
     * POST /mcp - Accepts agent messages and returns acknowledgment.
     * This is a placeholder endpoint implementing the MCP HTTP surface
     * for the Copilot coding agent to register and receive simple acknowledgments.
     */
    @POST
    public Response handleMessage(McpMessage message) {
        if (!mcpEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity(new McpErrorResponse("error", "MCP server is disabled"))
                .build();
        }

        // Process the message based on type
        McpResponse response = switch (message.type()) {
            case "initialize" -> new McpResponse(
                "initialize_result",
                message.id(),
                new McpInitializeResult("tc11-mcp-server", "1.0.0", new McpCapabilities(true, true, true))
            );
            case "ping" -> new McpResponse(
                "pong",
                message.id(),
                null
            );
            case "tools/list" -> new McpResponse(
                "tools/list_result",
                message.id(),
                new McpToolsListResult(java.util.List.of())
            );
            case "resources/list" -> new McpResponse(
                "resources/list_result",
                message.id(),
                new McpResourcesListResult(java.util.List.of())
            );
            case "prompts/list" -> new McpResponse(
                "prompts/list_result",
                message.id(),
                new McpPromptsListResult(java.util.List.of())
            );
            default -> new McpResponse(
                "ack",
                message.id(),
                new McpAckResult("Message received", message.type())
            );
        };

        return Response.ok(response).build();
    }

    // --- POJO Records for JSON serialization ---

    /**
     * MCP server status response.
     */
    public record McpStatusResponse(
        String status,
        String protocolVersion,
        boolean enabled,
        McpCapabilities capabilities
    ) {}

    /**
     * MCP server capabilities.
     */
    public record McpCapabilities(
        boolean tools,
        boolean resources,
        boolean prompts
    ) {}

    /**
     * Incoming MCP message from the agent.
     */
    public record McpMessage(
        String type,
        String id,
        Object params
    ) {}

    /**
     * Generic MCP response.
     */
    public record McpResponse(
        String type,
        String id,
        Object result
    ) {}

    /**
     * MCP error response.
     */
    public record McpErrorResponse(
        String status,
        String message
    ) {}

    /**
     * Initialize result payload.
     */
    public record McpInitializeResult(
        String serverName,
        String serverVersion,
        McpCapabilities capabilities
    ) {}

    /**
     * Acknowledgment result payload.
     */
    public record McpAckResult(
        String message,
        String receivedType
    ) {}

    /**
     * Tools list result.
     */
    public record McpToolsListResult(
        java.util.List<Object> tools
    ) {}

    /**
     * Resources list result.
     */
    public record McpResourcesListResult(
        java.util.List<Object> resources
    ) {}

    /**
     * Prompts list result.
     */
    public record McpPromptsListResult(
        java.util.List<Object> prompts
    ) {}
}
