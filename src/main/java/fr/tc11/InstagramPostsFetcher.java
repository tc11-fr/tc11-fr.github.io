package fr.tc11;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service to fetch Instagram posts using the Instagram Graph API during site generation.
 * Falls back to existing instagram.json if fetching fails or if no access token is configured.
 * 
 * To use this service, you need:
 * 1. An Instagram Professional account (Business or Creator)
 * 2. A Facebook Page connected to the Instagram account
 * 3. A Facebook App with Instagram Graph API permissions
 * 4. A long-lived access token configured via INSTAGRAM_ACCESS_TOKEN environment variable
 * 
 * @see <a href="https://developers.facebook.com/docs/instagram-api/">Instagram Graph API Documentation</a>
 */
@Startup
@ApplicationScoped
public class InstagramPostsFetcher {

    private static final Logger LOG = Logger.getLogger(InstagramPostsFetcher.class);
    
    // Instagram Graph API endpoints
    private static final String GRAPH_API_BASE = "https://graph.facebook.com/v21.0";
    private static final String MEDIA_FIELDS = "id,caption,media_type,media_url,permalink,thumbnail_url,timestamp";
    
    private static final int MAX_POSTS = 6;
    private static final int CONNECT_TIMEOUT_SECONDS = 10;
    private static final int REQUEST_TIMEOUT_SECONDS = 30;

    @ConfigProperty(name = "tc11.instagram.enabled", defaultValue = "true")
    boolean enabled;

    @ConfigProperty(name = "tc11.instagram.output-path", defaultValue = "content/instagram.json")
    String outputPath;
    
    // Access token from environment variable (recommended) or application.properties
    @ConfigProperty(name = "tc11.instagram.access-token")
    Optional<String> accessToken;
    
    // Instagram Business Account ID (required for Graph API)
    @ConfigProperty(name = "tc11.instagram.account-id")
    Optional<String> accountId;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;

    public InstagramPostsFetcher() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @PostConstruct
    void init() {
        if (!enabled) {
            LOG.info("Instagram posts fetcher is disabled");
            return;
        }

        List<String> existingPosts = readExistingPosts();
        
        // Check if Graph API credentials are configured
        if (accessToken.isEmpty() || accessToken.get().isBlank()) {
            LOG.info("Instagram Graph API access token not configured. Using existing instagram.json");
            if (existingPosts.isEmpty()) {
                LOG.warn("No existing instagram.json found. Instagram gallery will be empty.");
            }
            return;
        }
        
        if (accountId.isEmpty() || accountId.get().isBlank()) {
            LOG.info("Instagram account ID not configured. Using existing instagram.json");
            if (existingPosts.isEmpty()) {
                LOG.warn("No existing instagram.json found. Instagram gallery will be empty.");
            }
            return;
        }

        LOG.info("Fetching Instagram posts via Graph API");
        
        try {
            List<String> fetchedUrls = fetchInstagramPostsViaGraphApi();
            if (!fetchedUrls.isEmpty()) {
                writePostsToFile(fetchedUrls);
                LOG.infof("Successfully fetched and wrote %d Instagram posts", fetchedUrls.size());
            } else if (!existingPosts.isEmpty()) {
                LOG.info("No posts returned from API, keeping existing instagram.json");
            } else {
                LOG.warn("No Instagram posts available - instagram.json will be empty or missing");
            }
        } catch (Exception e) {
            LOG.warnf("Failed to fetch Instagram posts via Graph API: %s", e.getMessage());
            if (!existingPosts.isEmpty()) {
                LOG.infof("Using %d existing posts from instagram.json", existingPosts.size());
            }
        }
    }

    /**
     * Fetches Instagram posts using the Graph API.
     * Requires a valid access token and Instagram Business Account ID.
     */
    List<String> fetchInstagramPostsViaGraphApi() throws IOException, InterruptedException {
        String token = accessToken.orElseThrow(() -> new IllegalStateException("Access token not configured"));
        String igAccountId = accountId.orElseThrow(() -> new IllegalStateException("Account ID not configured"));
        
        // Build the API URL to fetch recent media
        String apiUrl = String.format("%s/%s/media?fields=%s&limit=%d&access_token=%s",
                GRAPH_API_BASE,
                URLEncoder.encode(igAccountId, StandardCharsets.UTF_8),
                URLEncoder.encode(MEDIA_FIELDS, StandardCharsets.UTF_8),
                MAX_POSTS,
                URLEncoder.encode(token, StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            String errorMessage = parseGraphApiError(response.body());
            throw new IOException("Graph API returned status " + response.statusCode() + ": " + errorMessage);
        }

        return parseMediaResponse(response.body());
    }

    /**
     * Parses the Graph API media response and extracts post permalinks.
     */
    List<String> parseMediaResponse(String jsonResponse) {
        List<String> postUrls = new ArrayList<>();
        
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode data = root.path("data");
            
            if (data.isArray()) {
                for (JsonNode media : data) {
                    String permalink = media.path("permalink").asText();
                    if (permalink != null && !permalink.isEmpty()) {
                        postUrls.add(permalink);
                    }
                }
            }
        } catch (Exception e) {
            LOG.warnf("Failed to parse Graph API response: %s", e.getMessage());
        }
        
        return postUrls;
    }

    /**
     * Parses error message from Graph API error response.
     */
    String parseGraphApiError(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode error = root.path("error");
            if (!error.isMissingNode()) {
                String message = error.path("message").asText("Unknown error");
                String type = error.path("type").asText("");
                int code = error.path("code").asInt(0);
                return String.format("%s (type: %s, code: %d)", message, type, code);
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return jsonResponse;
    }

    /**
     * Writes the fetched post URLs to the instagram.json file.
     */
    void writePostsToFile(List<String> postUrls) throws IOException {
        Path path = Path.of(outputPath);
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(postUrls);
        Files.writeString(path, json + "\n");
        LOG.infof("Wrote Instagram posts to %s", path.toAbsolutePath());
    }

    /**
     * Reads existing posts from instagram.json if it exists.
     */
    List<String> readExistingPosts() {
        Path path = Path.of(outputPath);
        if (Files.exists(path)) {
            try {
                String content = Files.readString(path);
                return objectMapper.readValue(content, new TypeReference<List<String>>() {});
            } catch (IOException e) {
                LOG.debugf("Failed to read existing instagram.json: %s", e.getMessage());
            }
        }
        return List.of();
    }
    
    // Package-private methods for testing
    
    /**
     * For testing: parse media response.
     */
    List<String> testParseMediaResponse(String jsonResponse) {
        return parseMediaResponse(jsonResponse);
    }
}
