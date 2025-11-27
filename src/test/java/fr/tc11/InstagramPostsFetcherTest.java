package fr.tc11;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for InstagramPostsFetcher Graph API parsing logic.
 */
@QuarkusTest
class InstagramPostsFetcherTest {

    @Inject
    InstagramPostsFetcher fetcher;

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
}
