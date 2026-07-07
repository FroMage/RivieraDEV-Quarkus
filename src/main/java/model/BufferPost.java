package model;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.logging.Log;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PreRemove;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.validator.constraints.Length;
import services.TalkBufferSchedulerService;

import java.sql.Types;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Entity
public class BufferPost extends PanacheEntity {

    @OneToOne
    public Talk talk;

    @OneToOne
    public Sponsor sponsor;

    @JdbcTypeCode(Types.LONGVARCHAR)
    @Length(max = 10000)
    public String error;

    public String twitterPostId;
    public String blueskyPostId;
    public String linkedInPostId;

    public Instant scheduledDate;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")
            .withZone(ZoneId.of("Europe/Paris"));

    public String getFormattedScheduledDate() {
        return scheduledDate != null ? FORMATTER.format(scheduledDate) : "";
    }

    @PreRemove
    public void preRemove() {
        if (scheduledDate != null && scheduledDate.isAfter(Instant.now())) {
            try {
                TalkBufferSchedulerService service = Arc.container().instance(TalkBufferSchedulerService.class).get();
                service.cancelBufferApiPosts(this);
            } catch (Exception e) {
                Log.warnf(e, "Failed to cancel Buffer API posts for BufferPost #%d", id);
            }
        }
    }
}
