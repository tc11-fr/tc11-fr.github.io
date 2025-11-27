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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service to fetch Instagram posts from a public profile during site generation.
 * Uses the public Instagram profile page to extract post shortcodes.
 * Falls back to existing instagram.json if fetching fails.
 */
@Startup
@ApplicationScoped
public class InstagramPostsFetcher {

    private static final Logger LOG = Logger.getLogger(InstagramPostsFetcher.class);
    
    // Default User-Agent - configurable to avoid outdated browser version issues
    private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (compatible; TC11SiteBot/1.0)";
    private static final String INSTAGRAM_PROFILE_URL = "https://www.instagram.com/%s/";
    
    // Pattern to find shortcodes in JSON format (used by _sharedData and other embedded data)
    private static final Pattern JSON_SHORTCODE_PATTERN = Pattern.compile(
            "\"shortcode\"\\s*:\\s*\"([A-Za-z0-9_-]{10,12})\"");
    
    // Pattern to find post URLs in links
    private static final Pattern POST_URL_PATTERN = Pattern.compile(
            "/p/([A-Za-z0-9_-]{10,12})");
    
    // Pattern to find reel URLs in links  
    private static final Pattern REEL_URL_PATTERN = Pattern.compile(
            "/reel/([A-Za-z0-9_-]{10,12})");
    
    private static final int MAX_POSTS = 6;
    private static final int CONNECT_TIMEOUT_SECONDS = 10;
    private static final int REQUEST_TIMEOUT_SECONDS = 30;

    @ConfigProperty(name = "tc11.instagram.username", defaultValue = "tc11assb")
    String instagramUsername;

    @ConfigProperty(name = "tc11.instagram.enabled", defaultValue = "true")
    boolean enabled;

    @ConfigProperty(name = "tc11.instagram.output-path", defaultValue = "content/instagram.json")
    String outputPath;
    
    @ConfigProperty(name = "tc11.instagram.user-agent", defaultValue = DEFAULT_USER_AGENT)
    String userAgent;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    void init() {
        if (!enabled) {
            LOG.info("Instagram posts fetcher is disabled");
            return;
        }

        LOG.infof("Attempting to fetch Instagram posts for @%s", instagramUsername);
        
        List<String> existingPosts = readExistingPosts();
        
        try {
            List<String> fetchedUrls = fetchInstagramPosts(instagramUsername);
            if (!fetchedUrls.isEmpty()) {
                writePostsToFile(fetchedUrls);
                LOG.infof("Successfully fetched and wrote %d Instagram posts", fetchedUrls.size());
            } else if (!existingPosts.isEmpty()) {
                LOG.infof("No new Instagram posts found, keeping %d existing posts from instagram.json", existingPosts.size());
            } else {
                LOG.warn("No Instagram posts available - instagram.json will be empty or missing");
            }
        } catch (Exception e) {
            if (!existingPosts.isEmpty()) {
                LOG.infof("Failed to fetch Instagram posts (%s). Using %d existing posts from instagram.json", 
                        e.getMessage(), existingPosts.size());
            } else {
                LOG.warnf("Failed to fetch Instagram posts: %s. No existing instagram.json to fall back to.", e.getMessage());
            }
        }
    }

    /**
     * Fetches Instagram posts from the public profile page.
     * Parses the HTML to extract post shortcodes.
     */
    List<String> fetchInstagramPosts(String username) throws IOException, InterruptedException {
        String profileUrl = String.format(INSTAGRAM_PROFILE_URL, username);
        
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(profileUrl))
                .header("User-Agent", userAgent)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.5")
                .header("Sec-Fetch-Dest", "document")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-Site", "none")
                .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            LOG.warnf("Instagram returned status %d for profile %s", response.statusCode(), username);
            return List.of();
        }

        String html = response.body();
        return extractPostUrls(html);
    }

    /**
     * Extracts Instagram post URLs from the HTML page.
     * Tries multiple extraction methods for robustness.
     * Note: Instagram shortcodes work with both /p/ and /reel/ URLs,
     * but we use /p/ which works for both post types.
     */
    List<String> extractPostUrls(String html) {
        Set<String> shortcodes = new LinkedHashSet<>();
        
        // Method 1: Try parsing embedded JSON data (window._sharedData)
        extractFromSharedData(html, shortcodes);
        
        // Method 2: Try parsing require("ScheduledServerJS") data
        extractFromScheduledServerJS(html, shortcodes);
        
        // Method 3: Fallback - scan for shortcodes directly in HTML/JSON
        extractFromPatterns(html, shortcodes);
        
        // Convert shortcodes to full URLs
        // Instagram /p/ URLs work for both posts and reels
        List<String> postUrls = new ArrayList<>();
        for (String shortcode : shortcodes) {
            if (postUrls.size() >= MAX_POSTS) break;
            postUrls.add("https://www.instagram.com/p/" + shortcode);
        }
        
        return postUrls;
    }

    private void extractFromSharedData(String html, Set<String> shortcodes) {
        Pattern sharedDataPattern = Pattern.compile("window\\._sharedData\\s*=\\s*(\\{.*?\\})\\s*;", Pattern.DOTALL);
        Matcher matcher = sharedDataPattern.matcher(html);
        
        if (matcher.find()) {
            try {
                String jsonData = matcher.group(1);
                JsonNode root = objectMapper.readTree(jsonData);
                
                // Navigate to the posts in the JSON structure
                JsonNode edges = root.path("entry_data")
                        .path("ProfilePage")
                        .path(0)
                        .path("graphql")
                        .path("user")
                        .path("edge_owner_to_timeline_media")
                        .path("edges");
                
                if (edges.isArray()) {
                    for (JsonNode edge : edges) {
                        String shortcode = edge.path("node").path("shortcode").asText();
                        if (shortcode != null && !shortcode.isEmpty()) {
                            shortcodes.add(shortcode);
                        }
                    }
                }
            } catch (Exception e) {
                LOG.debugf("Failed to parse _sharedData: %s", e.getMessage());
            }
        }
    }

    private void extractFromScheduledServerJS(String html, Set<String> shortcodes) {
        // Modern Instagram pages use ScheduledServerJS with encoded JSON
        // This pattern may change as Instagram updates their internal API
        // Multiple fallback patterns to improve resilience
        try {
            // Try primary pattern
            Pattern jsonPattern = Pattern.compile("\"xdt_api__v1__feed__user_timeline_graphql_connection\".*?\"edges\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
            Matcher matcher = jsonPattern.matcher(html);
            
            if (matcher.find()) {
                String edgesJson = matcher.group(1);
                Matcher shortcodeMatcher = Pattern.compile("\"code\"\\s*:\\s*\"([A-Za-z0-9_-]+)\"").matcher(edgesJson);
                while (shortcodeMatcher.find()) {
                    String code = shortcodeMatcher.group(1);
                    if (code.length() >= 10 && code.length() <= 12) {
                        shortcodes.add(code);
                    }
                }
            }
        } catch (Exception e) {
            // Pattern matching failed - this is expected if Instagram changes their page structure
            LOG.debugf("Failed to parse ScheduledServerJS data: %s", e.getMessage());
        }
    }

    private void extractFromPatterns(String html, Set<String> shortcodes) {
        // Extract from JSON shortcode patterns
        Matcher jsonMatcher = JSON_SHORTCODE_PATTERN.matcher(html);
        while (jsonMatcher.find()) {
            shortcodes.add(jsonMatcher.group(1));
        }
        
        // Extract from post URLs (/p/)
        Matcher postMatcher = POST_URL_PATTERN.matcher(html);
        while (postMatcher.find()) {
            shortcodes.add(postMatcher.group(1));
        }
        
        // Extract from reel URLs (/reel/)
        Matcher reelMatcher = REEL_URL_PATTERN.matcher(html);
        while (reelMatcher.find()) {
            shortcodes.add(reelMatcher.group(1));
        }
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
}
