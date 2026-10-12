package util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import model.Speaker;
import model.Sponsor;
import model.Talk;

/**
 * HTTP caching for the generated banner images, which are expensive to render and are fetched in bursts by
 * social network crawlers.
 *
 * Endpoints call {@link #check} before rendering: if the client already has the current version we answer
 * 304 without rendering anything, otherwise the validators and Cache-Control are added to the 200 response.
 */
@ApplicationScoped
public class BannerCache {

    private static final String PROPERTY = BannerCache.class.getName();

    @ConfigProperty(name = "rivieradev.banner.max-age", defaultValue = "1d")
    Duration maxAge;

    // changes with every build, so that templates, backgrounds and messages invalidate cached banners
    // (filtered by Maven, see microprofile-config.properties)
    @ConfigProperty(name = "rivieradev.build-id", defaultValue = "@")
    String configuredBuildId;

    private String buildId;

    @PostConstruct
    void init() {
        // If the build did not filter the resources, fall back to the start time of the application: a new
        // deployment always means a restart
        buildId = configuredBuildId.startsWith("@") ? "started-" + Instant.now() : configuredBuildId;
    }

    @Inject
    ContainerRequestContext requestContext;

    /**
     * Everything that goes into a banner: the ETag is a hash of it, and Last-Modified is the most recent date.
     */
    public class Key {
        private final StringBuilder identity = new StringBuilder();
        private Date lastModified;

        private Key(String kind, String language) {
            identity.append(buildId).append('|').append(kind).append('|').append(language);
        }

        public Key add(Talk talk) {
            return add(talk.id, talk.lastUpdated);
        }

        public Key add(Speaker speaker) {
            return add(speaker.id, speaker.lastUpdated);
        }

        public Key add(Sponsor sponsor) {
            return add(sponsor.id, sponsor.lastUpdated);
        }

        private Key add(Long id, Date lastUpdated) {
            identity.append('|').append(id).append(':').append(lastUpdated == null ? 0 : lastUpdated.getTime());
            if (lastUpdated != null && (lastModified == null || lastUpdated.after(lastModified))) {
                lastModified = lastUpdated;
            }
            return this;
        }
    }

    public Key key(String kind, String language) {
        return new Key(kind, language);
    }

    /**
     * Throws a 304 response if the client has the current version of the banner, otherwise schedules the
     * caching headers for the 200 response.
     */
    public void check(Request request, Key key) {
        EntityTag etag = new EntityTag(hash(key.identity.toString()));
        // Workaround for https://github.com/quarkusio/quarkus/issues/41110
        // HTTP dates have a one second resolution
        Date lastModified = key.lastModified == null ? null : new Date((key.lastModified.getTime() / 1000) * 1000);
        ResponseBuilder notModified = lastModified == null
                ? request.evaluatePreconditions(etag)
                : request.evaluatePreconditions(lastModified, etag);
        Validators validators = new Validators(etag, lastModified);
        if (notModified != null) {
            Response response = notModified.build();
            validators.apply(response.getHeaders(), cacheControl());
            throw new WebApplicationException(response);
        }
        requestContext.setProperty(PROPERTY, validators);
    }

    @ServerResponseFilter
    public void addCachingHeaders(ContainerRequestContext request, ContainerResponseContext response) {
        Object validators = request.getProperty(PROPERTY);
        if (validators != null && response.getStatus() == 200) {
            ((Validators) validators).apply(response.getHeaders(), cacheControl());
        }
    }

    private String cacheControl() {
        return "public, max-age=" + maxAge.toSeconds();
    }

    private static String hash(String string) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(string.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private record Validators(EntityTag etag, Date lastModified) {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        void apply(jakarta.ws.rs.core.MultivaluedMap headers, String cacheControl) {
            headers.putSingle(HttpHeaders.ETAG, etag);
            if (lastModified != null) {
                headers.putSingle(HttpHeaders.LAST_MODIFIED, lastModified);
            }
            headers.putSingle(HttpHeaders.CACHE_CONTROL, cacheControl);
        }
    }
}
