package fr.tc11;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service to fetch Instagram posts during site generation.
 * 
 * Uses the following fallback chain:
 * 1. Instagram Graph API (if credentials configured)
 * 2. Headless browser scraping via Playwright (if no API credentials)
 * 3. Existing instagram.json file (if all else fails)
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
    
    // Instagram profile URL for headless browser scraping
    private static final String INSTAGRAM_PROFILE_URL = "https://www.instagram.com/%s/";
    
    // Pattern to find post shortcodes in the page
    private static final Pattern POST_LINK_PATTERN = Pattern.compile("/p/([A-Za-z0-9_-]+)");
    private static final Pattern REEL_LINK_PATTERN = Pattern.compile("/reel/([A-Za-z0-9_-]+)");
    
    private static final int MAX_POSTS = 6;
    private static final int CONNECT_TIMEOUT_SECONDS = 10;
    private static final int REQUEST_TIMEOUT_SECONDS = 30;
    private static final int BROWSER_TIMEOUT_MS = 30000;
    private static final int BROWSER_CONTENT_LOAD_WAIT_MS = 2000;

    @ConfigProperty(name = "tc11.instagram.enabled", defaultValue = "true")
    boolean enabled;

    @ConfigProperty(name = "tc11.instagram.output-path", defaultValue = "content/instagram.json")
    String outputPath;
    
    @ConfigProperty(name = "tc11.instagram.username", defaultValue = "tc11assb")
    String instagramUsername;
    
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
        List<String> fetchedUrls = null;
        
        // Try Graph API first if credentials are configured
        if (hasGraphApiCredentials()) {
            LOG.info("Fetching Instagram posts via Graph API");
            try {
                fetchedUrls = fetchInstagramPostsViaGraphApi();
                if (!fetchedUrls.isEmpty()) {
                    writePostsToFile(fetchedUrls);
                    LOG.infof("Successfully fetched %d Instagram posts via Graph API", fetchedUrls.size());
                    return;
                }
            } catch (Exception e) {
                LOG.warnf("Graph API failed: %s. Trying headless browser...", e.getMessage());
            }
        } else {
            LOG.info("Graph API credentials not configured. Trying headless browser scraping...");
        }
        
        // Fallback to headless browser scraping
        try {
            fetchedUrls = fetchInstagramPostsViaHeadlessBrowser();
            if (!fetchedUrls.isEmpty()) {
                writePostsToFile(fetchedUrls);
                LOG.infof("Successfully fetched %d Instagram posts via headless browser", fetchedUrls.size());
                return;
            }
        } catch (Exception e) {
            LOG.warnf("Headless browser scraping failed: %s", e.getMessage());
        }
        
        // Final fallback to existing instagram.json
        if (!existingPosts.isEmpty()) {
            LOG.infof("Using %d existing posts from instagram.json", existingPosts.size());
        } else {
            LOG.warn("No Instagram posts available - instagram.json will be empty or missing");
        }
    }

    /**
     * Checks if Graph API credentials are configured.
     */
    private boolean hasGraphApiCredentials() {
        return accessToken.isPresent() && !accessToken.get().isBlank() 
                && accountId.isPresent() && !accountId.get().isBlank();
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
     * Fetches Instagram posts using a headless browser (Playwright).
     * This method loads the Instagram profile page and extracts post links
     * after JavaScript has rendered the content.
     */
    List<String> fetchInstagramPostsViaHeadlessBrowser() {
        LOG.infof("Starting headless browser to scrape @%s", instagramUsername);
        
        try (Playwright playwright = Playwright.create()) {
            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setTimeout(BROWSER_TIMEOUT_MS);
            
            try (Browser browser = playwright.chromium().launch(launchOptions)) {
                BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                        .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"));
                
                Page page = context.newPage();
                
                String profileUrl = String.format(INSTAGRAM_PROFILE_URL, instagramUsername);
                LOG.debugf("Navigating to %s", profileUrl);
                
                // Navigate to the profile page and wait for network to be idle
                page.navigate(profileUrl, new Page.NavigateOptions()
                        .setTimeout(BROWSER_TIMEOUT_MS)
                        .setWaitUntil(WaitUntilState.NETWORKIDLE));
                
                // Wait a bit more for dynamic content to load
                page.waitForTimeout(BROWSER_CONTENT_LOAD_WAIT_MS);
                
                // Get the page content after JavaScript has executed
                String content = page.content();
                
                return extractPostUrlsFromHtml(content);
            }
        } catch (Exception e) {
            LOG.warnf("Headless browser error: %s", e.getMessage());
            throw new RuntimeException("Failed to scrape Instagram via headless browser", e);
        }
    }

    /**
     * Extracts Instagram post URLs from the rendered HTML page.
     * Uses /p/ URL format for all content types as it works for embedding both posts and reels.
     */
    List<String> extractPostUrlsFromHtml(String html) {
        Set<String> shortcodes = new LinkedHashSet<>();
        
        // Extract from post links (/p/)
        Matcher postMatcher = POST_LINK_PATTERN.matcher(html);
        while (postMatcher.find()) {
            String shortcode = postMatcher.group(1);
            if (shortcode.length() >= 10 && shortcode.length() <= 12) {
                shortcodes.add(shortcode);
            }
        }
        
        // Extract from reel links (/reel/) - we use /p/ format as it works for embedding both types
        Matcher reelMatcher = REEL_LINK_PATTERN.matcher(html);
        while (reelMatcher.find()) {
            String shortcode = reelMatcher.group(1);
            if (shortcode.length() >= 10 && shortcode.length() <= 12) {
                shortcodes.add(shortcode);
            }
        }
        
        // Convert shortcodes to full URLs using /p/ format (works for embedding both posts and reels)
        List<String> postUrls = new ArrayList<>();
        for (String shortcode : shortcodes) {
            if (postUrls.size() >= MAX_POSTS) break;
            postUrls.add("https://www.instagram.com/p/" + shortcode);
        }
        
        LOG.debugf("Extracted %d posts from HTML", postUrls.size());
        return postUrls;
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
    
    /**
     * For testing: extract post URLs from HTML.
     */
    List<String> testExtractPostUrlsFromHtml(String html) {
        return extractPostUrlsFromHtml(html);
    }
}
