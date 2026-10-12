package util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Blob;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.common.core.BlockingOperationSupport;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;

import io.quarkiverse.renarde.pdf.Pdf;
import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.logging.Log;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.runtime.ExecutorRecorder;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import model.Banner;
import model.Speaker;
import model.Sponsor;
import model.Talk;

/**
 * Caching of the generated banner images, which are expensive to render and are fetched in bursts by social
 * network crawlers.
 *
 * <ul>
 * <li>HTTP: every banner has an ETag/Last-Modified, so clients which have it get a 304.</li>
 * <li>Memoization: the rendered image is stored in a {@link Banner} along with its ETag, and rendered again
 * only when the ETag is no longer the current one. This way we also do not render one banner per client.</li>
 * </ul>
 *
 * The ETag is a hash of everything the banner depends on, including the build, so that changing a template or
 * a background invalidates both.
 */
@ApplicationScoped
public class BannerCache {

    private static final String PENDING_STORE = BannerCache.class.getName() + ".pending";

    @ConfigProperty(name = "rivieradev.banner.max-age", defaultValue = "1d")
    Duration maxAge;

    // changes with every build, so that templates, backgrounds and messages invalidate cached banners
    // (filtered by Maven, see microprofile-config.properties)
    @ConfigProperty(name = "rivieradev.build-id", defaultValue = "@")
    String configuredBuildId;

    // we only render, and store, banners for the languages we have
    @ConfigProperty(name = "quarkus.locales")
    List<String> languages;

    private String buildId;

    @Inject
    ContainerRequestContext requestContext;

    @PostConstruct
    void init() {
        // If the build did not filter the resources, fall back to the start time of the application: a new
        // deployment always means a restart
        buildId = configuredBuildId.startsWith("@") ? "started-" + Instant.now() : configuredBuildId;
    }

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
        checkLanguage(language);
        return new Key(kind, language);
    }

    private void checkLanguage(String language) {
        if (!languages.contains(language)) {
            throw new NotFoundException();
        }
    }

    /**
     * Returns the banner for this owner (talk, speaker, sponsor) and language: a 304 if the client has the
     * current version, otherwise the memoized image if it is current, and otherwise a rendering of the template
     * which is memoized once the image has been generated.
     */
    public Response serve(Request request, PanacheEntityBase owner, String language, Key key,
            Supplier<TemplateInstance> template) {
        EntityTag etag = new EntityTag(hash(key.identity.toString()));
        // Workaround for https://github.com/quarkusio/quarkus/issues/41110
        // HTTP dates have a one second resolution
        Date lastModified = key.lastModified == null ? null : new Date((key.lastModified.getTime() / 1000) * 1000);

        ResponseBuilder notModified = lastModified == null
                ? request.evaluatePreconditions(etag)
                : request.evaluatePreconditions(lastModified, etag);
        if (notModified != null) {
            return addHeaders(notModified, etag, lastModified).build();
        }

        byte[] memoized = memoized(owner, language, etag.getValue());
        if (memoized != null) {
            Log.debugf("Serving memoized banner for %s in %s", owner, language);
            return addHeaders(Response.ok(memoized, Pdf.IMAGE_PNG_TYPE), etag, lastModified).build();
        }

        Log.debugf("Rendering banner for %s in %s", owner, language);
        // the HTML is turned into the image by the renarde-pdf response filter, and we store the result
        requestContext.setProperty(PENDING_STORE, new PendingStore(owner.getClass(), idOf(owner), language, etag.getValue()));
        return addHeaders(Response.ok(template.get().render(), Pdf.IMAGE_PNG_TYPE), etag, lastModified).build();
    }

    private ResponseBuilder addHeaders(ResponseBuilder response, EntityTag etag, Date lastModified) {
        CacheControl cacheControl = new CacheControl();
        cacheControl.setMaxAge((int) maxAge.toSeconds());
        // public is the default for CacheControl, but we do not want it to be implicit
        cacheControl.setPrivate(false);
        response.tag(etag).cacheControl(cacheControl);
        if (lastModified != null) {
            response.lastModified(lastModified);
        }
        return response;
    }

    private byte[] memoized(PanacheEntityBase owner, String language, String etag) {
        // reading a large object needs a transaction
        return QuarkusTransaction.joiningExisting().call(() -> {
            Banner banner = Banner.find(owner, language);
            if (banner == null || banner.png == null || !etag.equals(banner.etag)) {
                return null;
            }
            try {
                Blob png = banner.png;
                return png.getBytes(1, (int) png.length());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Stores the image once it has been rendered, which is done by a response filter of renarde-pdf. Ours has a
     * lower priority, which for response filters means that it runs after.
     */
    @ServerResponseFilter(priority = 1000)
    public Uni<Void> storeRenderedBanner(ContainerRequestContext request, ContainerResponseContext response) {
        Object pending = request.getProperty(PENDING_STORE);
        if (pending == null || response.getStatus() != 200 || !(response.getEntity() instanceof byte[] png)) {
            return Uni.createFrom().nullItem();
        }
        PendingStore store = (PendingStore) pending;
        if (BlockingOperationSupport.isBlockingAllowed()) {
            store.store(png);
            return Uni.createFrom().nullItem();
        }
        return Uni.createFrom().<Void> item(() -> {
            store.store(png);
            return null;
        }).runSubscriptionOn(ExecutorRecorder.getCurrent());
    }

    private record PendingStore(Class<?> ownerType, Long ownerId, String language, String etag) {
        void store(byte[] png) {
            try {
                QuarkusTransaction.requiringNew().run(() -> {
                    PanacheEntityBase owner = (PanacheEntityBase) Panache.getEntityManager().getReference(ownerType, ownerId);
                    Banner.store(owner, language, etag, png);
                });
            } catch (RuntimeException e) {
                // this can happen if the banner is requested concurrently and another request stored it first,
                // or if the owner has been deleted since: the image is still correct for this request, and the next
                // request will try again
                Log.warnf(e, "Failed to store the banner of %s %s in %s", ownerType.getSimpleName(), ownerId, language);
            }
        }
    }

    private static Long idOf(PanacheEntityBase owner) {
        if (owner instanceof Talk talk) {
            return talk.id;
        } else if (owner instanceof Speaker speaker) {
            return speaker.id;
        } else if (owner instanceof Sponsor sponsor) {
            return sponsor.id;
        }
        throw new IllegalArgumentException("Unsupported banner owner: " + owner);
    }

    private static String hash(String string) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(string.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
