package fr.tc11;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for InstagramPostsFetcher extraction logic.
 */
@QuarkusTest
class InstagramPostsFetcherTest {

    @Inject
    InstagramPostsFetcher fetcher;

    @Test
    void testExtractPostUrlsFromSharedData() {
        // Simulated HTML with _sharedData containing post shortcodes
        String html = """
            <html>
            <script>window._sharedData = {
                "entry_data": {
                    "ProfilePage": [{
                        "graphql": {
                            "user": {
                                "edge_owner_to_timeline_media": {
                                    "edges": [
                                        {"node": {"shortcode": "ABC123DEF45"}},
                                        {"node": {"shortcode": "XYZ789GHI01"}}
                                    ]
                                }
                            }
                        }
                    }]
                }
            };</script>
            </html>
            """;

        List<String> urls = fetcher.extractPostUrls(html);

        assertNotNull(urls);
        assertEquals(2, urls.size());
        assertTrue(urls.contains("https://www.instagram.com/p/ABC123DEF45"));
        assertTrue(urls.contains("https://www.instagram.com/p/XYZ789GHI01"));
    }

    @Test
    void testExtractPostUrlsFromPatterns() {
        // HTML with direct post and reel links
        // Note: Reels are converted to /p/ URLs which work for embedding both posts and reels
        String html = """
            <html>
            <a href="/p/DMc_B-kNmxf">Post 1</a>
            <a href="/p/DK5HR3bgmSY">Post 2</a>
            <a href="/reel/DKhw5Octojb">Reel 1</a>
            </html>
            """;

        List<String> urls = fetcher.extractPostUrls(html);

        assertNotNull(urls);
        assertEquals(3, urls.size());
        assertTrue(urls.contains("https://www.instagram.com/p/DMc_B-kNmxf"));
        assertTrue(urls.contains("https://www.instagram.com/p/DK5HR3bgmSY"));
        // Reel shortcode is also converted to /p/ URL for consistent embedding
        assertTrue(urls.contains("https://www.instagram.com/p/DKhw5Octojb"));
    }

    @Test
    void testExtractPostUrlsFromJsonPattern() {
        // HTML with JSON shortcode pattern
        String html = """
            <html>
            <script>{"shortcode": "ABC123DEF45", "other": "data"}</script>
            <script>{"shortcode": "XYZ789GHI01"}</script>
            </html>
            """;

        List<String> urls = fetcher.extractPostUrls(html);

        assertNotNull(urls);
        assertEquals(2, urls.size());
        assertTrue(urls.contains("https://www.instagram.com/p/ABC123DEF45"));
        assertTrue(urls.contains("https://www.instagram.com/p/XYZ789GHI01"));
    }

    @Test
    void testExtractPostUrlsDeduplicates() {
        // HTML with duplicate shortcodes
        String html = """
            <html>
            <a href="/p/ABC123DEF45">Post 1</a>
            <a href="/p/ABC123DEF45">Post 1 again</a>
            <script>{"shortcode": "ABC123DEF45"}</script>
            </html>
            """;

        List<String> urls = fetcher.extractPostUrls(html);

        assertNotNull(urls);
        assertEquals(1, urls.size());
        assertEquals("https://www.instagram.com/p/ABC123DEF45", urls.get(0));
    }

    @Test
    void testExtractPostUrlsEmptyHtml() {
        List<String> urls = fetcher.extractPostUrls("");
        assertNotNull(urls);
        assertTrue(urls.isEmpty());
    }

    @Test
    void testExtractPostUrlsNoMatches() {
        String html = "<html><body>No Instagram content here</body></html>";
        List<String> urls = fetcher.extractPostUrls(html);
        assertNotNull(urls);
        assertTrue(urls.isEmpty());
    }

    @Test
    void testExtractPostUrlsMaxLimit() {
        // HTML with more than MAX_POSTS shortcodes
        StringBuilder html = new StringBuilder("<html>");
        for (int i = 0; i < 10; i++) {
            html.append(String.format("<a href=\"/p/SHORTCODE%02d\">Post %d</a>", i, i));
        }
        html.append("</html>");

        List<String> urls = fetcher.extractPostUrls(html.toString());

        assertNotNull(urls);
        // Should be limited to MAX_POSTS (6)
        assertEquals(6, urls.size());
    }
}
