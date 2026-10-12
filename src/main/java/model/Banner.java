package model;

import java.sql.Blob;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;

/**
 * A rendered banner image for a talk, speaker or sponsor in a given language.
 *
 * This is memoized so that we only render a banner again when what it shows has changed:
 * {@link #etag} identifies the version of the banner that {@link #png} holds. This is not a field of the
 * entities themselves so it is not part of the backoffice forms, the JSON export, and storing it does not
 * trigger their lifecycle callbacks.
 */
@Entity
public class Banner extends PanacheEntity {

    // exactly one of these is set, and the banner is deleted with it
    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    public Talk talk;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    public Speaker speaker;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    public Sponsor sponsor;

    @NotNull
    public String language;

    @NotNull
    public String etag;

    // a large object: only the reference is loaded with the entity
    public Blob png;

    public static Banner find(PanacheEntityBase owner, String language) {
        return find(ownerField(owner) + " = ?1 and language = ?2", owner, language).firstResult();
    }

    /**
     * Stores the image, creating the banner if needed.
     */
    public static void store(PanacheEntityBase owner, String language, String etag, byte[] png) {
        Banner banner = find(owner, language);
        if (banner == null) {
            banner = new Banner();
            banner.language = language;
            if (owner instanceof Talk talk) {
                banner.talk = talk;
            } else if (owner instanceof Speaker speaker) {
                banner.speaker = speaker;
            } else if (owner instanceof Sponsor sponsor) {
                banner.sponsor = sponsor;
            } else {
                throw new IllegalArgumentException("Unsupported banner owner: " + owner);
            }
        } else {
            banner.deleteImage();
        }
        banner.etag = etag;
        banner.png = Panache.getSession().getLobHelper().createBlob(png);
        banner.persist();
    }

    /**
     * Deletes the large object, which Hibernate does not do when we replace the Blob or delete the row.
     */
    private void deleteImage() {
        if (png != null) {
            getEntityManager().createNativeQuery("select lo_unlink(png) from Banner where id = ?1")
                    .setParameter(1, id).getResultList();
            png = null;
        }
    }

    private static String ownerField(PanacheEntityBase owner) {
        if (owner instanceof Talk) {
            return "talk";
        } else if (owner instanceof Speaker) {
            return "speaker";
        } else if (owner instanceof Sponsor) {
            return "sponsor";
        }
        throw new IllegalArgumentException("Unsupported banner owner: " + owner);
    }
}
