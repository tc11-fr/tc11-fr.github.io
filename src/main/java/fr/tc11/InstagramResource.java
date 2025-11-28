package fr.tc11;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/**
 * REST endpoint to serve Instagram posts as JSON.
 * 
 * This serves the in-memory Instagram posts fetched at startup,
 * replacing the need for a static instagram.json file that might
 * not be picked up correctly by the static site generator.
 */
@Path("/instagram.json")
public class InstagramResource {

    @Inject
    InstagramPostsFetcher fetcher;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getInstagramPosts() {
        return fetcher.getInstagramPosts();
    }
}
