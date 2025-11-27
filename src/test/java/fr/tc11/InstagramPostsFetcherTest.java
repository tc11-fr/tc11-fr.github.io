package fr.tc11;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for InstagramPostsFetcher parsing logic.
 */
@QuarkusTest
class InstagramPostsFetcherTest {

    @Inject
    InstagramPostsFetcher fetcher;

    // ========== Graph API Response Parsing Tests ==========

    @Test
    void testParseMediaResponseWithValidData() {
        // Simulated Graph API response with media data
        String jsonResponse = """
            {
                "data": [
                    {
                        "id": "12345",
                        "permalink": "https://www.instagram.com/p/ABC123DEF45/",
                        "media_type": "IMAGE",
                        "timestamp": "2024-01-15T10:30:00+0000"
                    },
                    {
                        "id": "67890",
                        "permalink": "https://www.instagram.com/p/XYZ789GHI01/",
                        "media_type": "VIDEO",
                        "timestamp": "2024-01-14T15:45:00+0000"
                    }
                ],
                "paging": {
                    "cursors": {
                        "before": "abc",
                        "after": "xyz"
                    }
                }
            }
            """;

        List<String> urls = fetcher.testParseMediaResponse(jsonResponse);

        assertNotNull(urls);
        assertEquals(2, urls.size());
        assertEquals("https://www.instagram.com/p/ABC123DEF45/", urls.get(0));
        assertEquals("https://www.instagram.com/p/XYZ789GHI01/", urls.get(1));
    }

    @Test
    void testParseMediaResponseWithEmptyData() {
        String jsonResponse = """
            {
                "data": []
            }
            """;

        List<String> urls = fetcher.testParseMediaResponse(jsonResponse);

        assertNotNull(urls);
        assertTrue(urls.isEmpty());
    }

    @Test
    void testParseMediaResponseWithMissingPermalink() {
        // Response with some items missing permalink
        String jsonResponse = """
            {
                "data": [
                    {
                        "id": "12345",
                        "permalink": "https://www.instagram.com/p/ABC123DEF45/",
                        "media_type": "IMAGE"
                    },
                    {
                        "id": "67890",
                        "media_type": "VIDEO"
                    }
                ]
            }
            """;

        List<String> urls = fetcher.testParseMediaResponse(jsonResponse);

        assertNotNull(urls);
        assertEquals(1, urls.size());
        assertEquals("https://www.instagram.com/p/ABC123DEF45/", urls.get(0));
    }

    @Test
    void testParseMediaResponseWithInvalidJson() {
        String invalidJson = "not valid json";

        List<String> urls = fetcher.testParseMediaResponse(invalidJson);

        assertNotNull(urls);
        assertTrue(urls.isEmpty());
    }

    @Test
    void testParseMediaResponseWithCarouselAlbum() {
        // Response with carousel album type (multiple images in one post)
        String jsonResponse = """
            {
                "data": [
                    {
                        "id": "12345",
                        "permalink": "https://www.instagram.com/p/CAROUSEL123/",
                        "media_type": "CAROUSEL_ALBUM",
                        "timestamp": "2024-01-15T10:30:00+0000"
                    },
                    {
                        "id": "67890",
                        "permalink": "https://www.instagram.com/reel/REEL12345/",
                        "media_type": "VIDEO",
                        "timestamp": "2024-01-14T15:45:00+0000"
                    }
                ]
            }
            """;

        List<String> urls = fetcher.testParseMediaResponse(jsonResponse);

        assertNotNull(urls);
        assertEquals(2, urls.size());
        assertTrue(urls.contains("https://www.instagram.com/p/CAROUSEL123/"));
        assertTrue(urls.contains("https://www.instagram.com/reel/REEL12345/"));
    }

    @Test
    void testParseMediaResponseWithNullData() {
        String jsonResponse = """
            {
                "error": {
                    "message": "Some error",
                    "type": "OAuthException",
                    "code": 190
                }
            }
            """;

        List<String> urls = fetcher.testParseMediaResponse(jsonResponse);

        assertNotNull(urls);
        assertTrue(urls.isEmpty());
    }

    // ========== HTML Extraction Tests (for headless browser fallback) ==========

    @Test
    void testExtractPostUrlsFromHtmlWithPostLinks() {
        String html = """
            <html>
            <body>
                <a href="/p/DMc_B-kNmxf/">Post 1</a>
                <a href="/p/DK5HR3bgmSY/">Post 2</a>
                <a href="/p/DKhw5Octojb/">Post 3</a>
            </body>
            </html>
            """;

        List<String> urls = fetcher.testExtractPostUrlsFromHtml(html);

        assertNotNull(urls);
        assertEquals(3, urls.size());
        assertTrue(urls.contains("https://www.instagram.com/p/DMc_B-kNmxf"));
        assertTrue(urls.contains("https://www.instagram.com/p/DK5HR3bgmSY"));
        assertTrue(urls.contains("https://www.instagram.com/p/DKhw5Octojb"));
    }

    @Test
    void testExtractPostUrlsFromHtmlWithReelLinks() {
        // Reels are converted to /p/ URLs because /p/ format works for embedding both posts and reels
        String html = """
            <html>
            <body>
                <a href="/reel/ABC123DEF45/">Reel 1</a>
                <a href="/p/XYZ789GHI01/">Post 1</a>
            </body>
            </html>
            """;

        List<String> urls = fetcher.testExtractPostUrlsFromHtml(html);

        assertNotNull(urls);
        assertEquals(2, urls.size());
        // Both reels and posts use /p/ format for consistent embedding behavior
        assertTrue(urls.contains("https://www.instagram.com/p/ABC123DEF45"));
        assertTrue(urls.contains("https://www.instagram.com/p/XYZ789GHI01"));
    }

    @Test
    void testExtractPostUrlsFromHtmlDeduplicates() {
        String html = """
            <html>
            <body>
                <a href="/p/ABC123DEF45/">Post 1</a>
                <a href="/p/ABC123DEF45/">Post 1 again</a>
                <div data-href="/p/ABC123DEF45/"></div>
            </body>
            </html>
            """;

        List<String> urls = fetcher.testExtractPostUrlsFromHtml(html);

        assertNotNull(urls);
        assertEquals(1, urls.size());
        assertEquals("https://www.instagram.com/p/ABC123DEF45", urls.get(0));
    }

    @Test
    void testExtractPostUrlsFromHtmlEmpty() {
        String html = "<html><body>No Instagram content here</body></html>";

        List<String> urls = fetcher.testExtractPostUrlsFromHtml(html);

        assertNotNull(urls);
        assertTrue(urls.isEmpty());
    }

    @Test
    void testExtractPostUrlsFromHtmlMaxLimit() {
        // HTML with more than MAX_POSTS (6) shortcodes
        StringBuilder html = new StringBuilder("<html><body>");
        for (int i = 0; i < 10; i++) {
            html.append(String.format("<a href=\"/p/SHORTCODE%02d/\">Post %d</a>", i, i));
        }
        html.append("</body></html>");

        List<String> urls = fetcher.testExtractPostUrlsFromHtml(html.toString());

        assertNotNull(urls);
        // Should be limited to MAX_POSTS (6)
        assertEquals(6, urls.size());
    }
}
