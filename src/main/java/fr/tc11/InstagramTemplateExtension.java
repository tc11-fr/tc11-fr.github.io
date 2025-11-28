package fr.tc11;

import io.quarkus.qute.TemplateExtension;
import jakarta.enterprise.inject.spi.CDI;

import java.util.List;

/**
 * Qute template extension to expose Instagram posts to templates.
 * 
 * Usage in templates: {instagram:posts}
 * 
 * This allows the instagram.json file to be generated at build time
 * with dynamically fetched Instagram post URLs.
 */
@TemplateExtension(namespace = "instagram")
public class InstagramTemplateExtension {

    // Cache the fetcher instance since Instagram posts are fetched once at startup
    private static volatile InstagramPostsFetcher cachedFetcher;

    /**
     * Returns the list of Instagram post URLs.
     * Used in Qute templates to generate instagram.json content.
     * 
     * @return list of Instagram post URLs
     */
    public static List<String> posts() {
        if (cachedFetcher == null) {
            cachedFetcher = CDI.current().select(InstagramPostsFetcher.class).get();
        }
        return cachedFetcher.getInstagramPosts();
    }
}
