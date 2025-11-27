# Quarkus MCP Server for GitHub Copilot Coding Agent

This document explains how to run the Quarkus development server with the MCP (Model Context Protocol) endpoint for local GitHub Copilot coding agent integration.

## Overview

The MCP server provides a minimal HTTP endpoint at `/mcp` that implements the MCP protocol surface, allowing the GitHub Copilot coding agent to connect and interact during development.

## Prerequisites

- Java 21 or higher
- Maven 3.9+ (or use the included Maven wrapper `./mvnw`)

## Running the Development Server

Start the Quarkus development server:

```bash
./mvnw quarkus:dev
```

The server will start on port 8080. The MCP endpoint will be available at:

```
http://localhost:8080/mcp
```

## MCP Endpoint

### GET /mcp

Returns the MCP server status and capabilities.

**Example Response:**

```json
{
  "status": "ok",
  "protocolVersion": "1.0",
  "enabled": true,
  "capabilities": {
    "tools": true,
    "resources": true,
    "prompts": true
  }
}
```

### POST /mcp

Accepts agent messages and returns acknowledgments. Supported message types:

- `initialize` - Initialize the MCP connection
- `ping` - Health check
- `tools/list` - List available tools
- `resources/list` - List available resources
- `prompts/list` - List available prompts

**Example Request:**

```json
{
  "type": "initialize",
  "id": "1",
  "params": {}
}
```

**Example Response:**

```json
{
  "type": "initialize_result",
  "id": "1",
  "result": {
    "serverName": "tc11-mcp-server",
    "serverVersion": "1.0.0",
    "capabilities": {
      "tools": true,
      "resources": true,
      "prompts": true
    }
  }
}
```

## Health Check

The server includes SmallRye Health endpoints:

- `/q/health` - Overall health status
- `/q/health/live` - Liveness probe
- `/q/health/ready` - Readiness probe

## Configuration

The following properties can be configured in `src/main/resources/application.properties`:

| Property | Default | Description |
|----------|---------|-------------|
| `quarkus.http.port` | 8080 | HTTP server port |
| `mcp.server.enabled` | true | Enable/disable the MCP server |
| `mcp.server.path` | /mcp | MCP endpoint path |
| `mcp.server.port` | 8080 | MCP server port (same as HTTP) |

## Configuring the Local Copilot Coding Agent

To connect the GitHub Copilot coding agent to this MCP server:

1. Start the Quarkus dev server:
   ```bash
   ./mvnw quarkus:dev
   ```

2. Configure the Copilot coding agent to point at the MCP endpoint:
   ```
   http://localhost:8080/mcp
   ```

3. Verify the connection by checking the server logs or hitting the GET endpoint:
   ```bash
   curl http://localhost:8080/mcp
   ```

## DevContainer Support

If using VS Code with DevContainers, port 8080 is automatically forwarded. You can start the dev server from the terminal inside the container.

## Reference Documentation

- [GitHub MCP Integration Docs](https://docs.github.com/en/enterprise-cloud@latest/copilot/how-tos/use-copilot-agents/coding-agent/extend-coding-agent-with-mcp)
- [Quarkus Documentation](https://quarkus.io/guides/)
- [Quarkus RESTEasy Reactive Guide](https://quarkus.io/guides/resteasy-reactive)

## Development Notes

This is a minimal placeholder implementation of the MCP HTTP surface. It provides the basic endpoints needed for the Copilot coding agent to register and receive acknowledgments. The implementation can be extended to support additional MCP features as needed.
