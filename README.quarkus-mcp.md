# Quarkus MCP Server for GitHub Copilot Coding Agent

This document describes how to run the Quarkus-based MCP (Model Context Protocol) server for local development with the GitHub Copilot Coding Agent.

## Overview

The MCP server provides HTTP endpoints that allow the GitHub Copilot local coding agent to connect and interact with your development environment. This is a lightweight implementation that supports the basic MCP protocol operations.

For full documentation on the MCP protocol and Copilot Coding Agent integration, see:
- [GitHub Docs: Extend Coding Agent with MCP](https://docs.github.com/en/enterprise-cloud@latest/copilot/how-tos/use-copilot-agents/coding-agent/extend-coding-agent-with-mcp)

## Quick Start

### Prerequisites

- Java 21 or higher
- Maven 3.9+ (or use the included Maven wrapper `./mvnw`)

### Running the Development Server

Start the Quarkus development server:

```bash
./mvnw quarkus:dev
```

The server will start on port 8080 with hot-reload enabled.

### MCP Endpoint

The MCP endpoint is available at:

```
http://localhost:8080/mcp
```

#### Endpoints

| Method | Path   | Description                          |
|--------|--------|--------------------------------------|
| GET    | `/mcp` | Returns MCP server status            |
| POST   | `/mcp` | Accepts agent messages and responds  |

#### Example Requests

Check server status:

```bash
curl http://localhost:8080/mcp
```

Response:
```json
{
  "status": "ok",
  "serverName": "TC11 MCP Server",
  "version": "1.0.0",
  "mcpEnabled": true
}
```

Send a message:

```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{"id": "1", "method": "test", "params": {}}'
```

Response:
```json
{
  "id": "1",
  "status": "acknowledged",
  "message": "Message received by TC11 MCP Server"
}
```

## Configuring Copilot Coding Agent

To configure the local Copilot coding agent to connect to this MCP server:

1. Start the Quarkus dev server: `./mvnw quarkus:dev`
2. Configure your Copilot agent to point at: `http://localhost:8080/mcp`
3. The agent should be able to connect and receive acknowledgements

## Configuration Properties

The following properties can be configured in `src/main/resources/application.properties`:

| Property            | Default  | Description                           |
|---------------------|----------|---------------------------------------|
| `quarkus.http.port` | `8080`   | HTTP server port                      |
| `mcp.server.enabled`| `true`   | Enable/disable MCP server             |
| `mcp.server.path`   | `/mcp`   | MCP endpoint path                     |
| `mcp.server.port`   | `8080`   | MCP server port (same as HTTP port)   |

## Development with DevContainers

If you're using VS Code with DevContainers, port 8080 is automatically forwarded. You can start the dev server directly in the terminal.

## Health Checks

The application includes SmallRye Health for health checks:

- Liveness: `http://localhost:8080/q/health/live`
- Readiness: `http://localhost:8080/q/health/ready`
- All: `http://localhost:8080/q/health`

## Further Reading

- [GitHub Copilot Coding Agent Documentation](https://docs.github.com/en/enterprise-cloud@latest/copilot/how-tos/use-copilot-agents/coding-agent/extend-coding-agent-with-mcp)
- [Quarkus Documentation](https://quarkus.io/guides/)
- [RESTEasy Reactive Guide](https://quarkus.io/guides/resteasy-reactive)
