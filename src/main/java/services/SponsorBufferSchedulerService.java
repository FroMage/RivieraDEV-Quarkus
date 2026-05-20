package services;

import graphql.buffer.Account;
import graphql.buffer.BufferClient;
import graphql.buffer.Channel;
import graphql.buffer.ChannelsInput;
import graphql.buffer.Mode;
import graphql.buffer.SchedulingType;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduler;
import io.smallrye.graphql.client.GraphQLClient;
import io.smallrye.graphql.client.Response;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import jakarta.transaction.Transactional;
import model.BufferPost;
import model.Sponsor;
import model.SponsorShip;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class SponsorBufferSchedulerService {

    private static final String SCHEDULER_IDENTITY = "sponsor-buffer-processor";

    @Inject
    Scheduler scheduler;

    @Inject
    SponsorSummaryService summaryService;

    @Inject
    BufferClient bufferClient;

    @Inject
    @GraphQLClient("buffer")
    DynamicGraphQLClient dynamicClient;

    @ConfigProperty(name = "rivieradev.base-url")
    String baseUrl;

    private volatile boolean isRunning = false;
    private List<String> shortTextChannelIds;
    private String twitterChannelId;
    private String linkedInChannelId;

    public void startScheduler() {
        if (isRunning) {
            Log.warn("Sponsor scheduler already running");
            return;
        }

        Account account = bufferClient.account();
        if (account.organizations == null || account.organizations.isEmpty()) {
            throw new RuntimeException("No organizations found in Buffer account");
        }
        String organizationId = account.organizations.get(0).id;
        Log.infof("Using Buffer organization: %s (%s)", account.organizations.get(0).name, organizationId);

        List<Channel> channels = bufferClient.channels(new ChannelsInput(organizationId));

        twitterChannelId = channels.stream()
                .filter(c -> "twitter".equalsIgnoreCase(c.service))
                .map(c -> c.id)
                .findFirst()
                .orElse(null);

        shortTextChannelIds = channels.stream()
                .filter(c -> "twitter".equalsIgnoreCase(c.service)
                        || "bluesky".equalsIgnoreCase(c.service))
                .map(c -> c.id)
                .collect(Collectors.toList());

        linkedInChannelId = channels.stream()
                .filter(c -> "linkedin".equalsIgnoreCase(c.service))
                .map(c -> c.id)
                .findFirst()
                .orElse(null);

        if (shortTextChannelIds.isEmpty() && linkedInChannelId == null) {
            throw new RuntimeException("No Twitter, Bluesky, or LinkedIn channel found in Buffer account");
        }

        isRunning = true;

        Log.infof("Starting Sponsor Buffer scheduler for %d short-text channels and LinkedIn: %s",
                shortTextChannelIds.size(), linkedInChannelId != null ? "yes" : "no");

        scheduler.newJob(SCHEDULER_IDENTITY)
                .setInterval("30s")
                .setTask(executionContext -> {
                    processNextSponsor();
                })
                .schedule();
    }

    @Transactional
    public void processNextSponsor() {
        if (!isRunning) {
            return;
        }

        Sponsor sponsor = Sponsor.find("bufferPost IS NULL AND level != ?1 ORDER BY id", SponsorShip.PreviousYears).firstResult();

        if (sponsor == null) {
            Log.info("No more sponsors to process, stopping scheduler");
            stopScheduler();
            return;
        }

        try {
            Log.infof("Processing sponsor #%d: %s", sponsor.id, sponsor.company);

            String description = sponsor.aboutEN != null && !sponsor.aboutEN.isBlank()
                    ? sponsor.aboutEN
                    : sponsor.about;

            if (description == null || description.isBlank()) {
                Log.warnf("Sponsor #%d has no description, marking as skipped", sponsor.id);
                BufferPost bp = new BufferPost();
                bp.sponsor = sponsor;
                bp.error = "SKIPPED";
                bp.persist();
                return;
            }

            String sponsorUrl = baseUrl + "/sponsor/" + sponsor.id;

            String twitterName = TalkBufferSchedulerService.nameWithHandle(sponsor.company, sponsor.twitterAccount);

            Log.infof("Calling Gemini API for sponsor #%d", sponsor.id);
            String shortText = summaryService.composeSponsorPost(twitterName, sponsorUrl, description);
            Log.infof("Generated tweet (%d chars): %s", shortText.length(), shortText);

            if (shortText.length() > 280) {
                Log.warnf("Tweet exceeds 280 chars (%d), truncating", shortText.length());
                int maxTextLength = 280 - sponsorUrl.length() - 5;
                shortText = shortText.replace(sponsorUrl, "").stripTrailing();
                if (shortText.length() > maxTextLength) {
                    shortText = shortText.substring(0, maxTextLength) + "...";
                }
                shortText = shortText + " " + sponsorUrl;
            }

            Instant nextSlot = computeNextSlot();

            String mutation = """
                    mutation($input: CreatePostInput!) {
                        createPost(input: $input) {
                            __typename
                            ... on PostActionSuccess {
                                post { id dueAt }
                            }
                            ... on UnexpectedError { message }
                            ... on RestProxyError { message code }
                            ... on InvalidInputError { message }
                        }
                    }
                    """;

            String linkedInName = TalkBufferSchedulerService.nameWithHandle(sponsor.company, sponsor.linkedInAccount);
            String linkedInText = linkedInName + "\n\n" + description + "\n\n" + sponsorUrl;

            BufferPost bp = new BufferPost();
            bp.sponsor = sponsor;
            bp.scheduledDate = nextSlot;

            for (String channelId : shortTextChannelIds) {
                String postId = postToBuffer(mutation, shortText, channelId, nextSlot, sponsor.id);
                if (channelId.equals(twitterChannelId)) {
                    bp.twitterPostId = postId;
                } else {
                    bp.blueskyPostId = postId;
                }
            }

            if (linkedInChannelId != null) {
                bp.linkedInPostId = postToBuffer(mutation, linkedInText, linkedInChannelId, nextSlot, sponsor.id);
            }

            bp.persist();

            Log.infof("Successfully scheduled sponsor #%d (twitter=%s, bluesky=%s, linkedin=%s)",
                    sponsor.id, bp.twitterPostId, bp.blueskyPostId, bp.linkedInPostId);

        } catch (Exception e) {
            Log.errorf(e, "Error processing sponsor #%d: %s", sponsor.id, e.getMessage());
            BufferPost bp = new BufferPost();
            bp.sponsor = sponsor;
            bp.error = e.getMessage();
            bp.persist();
        }
    }

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final LocalTime MIDDAY = LocalTime.of(12, 0);

    private Instant computeNextSlot() {
        BufferPost latest = BufferPost.find("sponsor IS NOT NULL AND scheduledDate IS NOT NULL ORDER BY scheduledDate DESC").firstResult();
        Instant maxScheduled = latest != null ? latest.scheduledDate : null;

        ZonedDateTime now = ZonedDateTime.now(PARIS);
        ZonedDateTime nextSlot;

        if (maxScheduled == null) {
            nextSlot = now.plusDays(1).with(MIDDAY);
        } else {
            ZonedDateTime last = maxScheduled.atZone(PARIS);
            nextSlot = last.plusDays(1).with(MIDDAY);
        }

        if (!nextSlot.isAfter(now)) {
            nextSlot = now.plusDays(1).with(MIDDAY);
        }

        Log.infof("Next scheduled slot: %s", nextSlot);
        return nextSlot.toInstant();
    }

    private String postToBuffer(String mutation, String text, String channelId, Instant scheduledAt, Long sponsorId) throws Exception {
        Map<String, Object> postInput = Map.of(
                "channelId", channelId,
                "text", text,
                "mode", Mode.customScheduled.name(),
                "schedulingType", SchedulingType.automatic.name(),
                "dueAt", scheduledAt.toString()
        );
        Map<String, Object> variables = Map.of("input", postInput);

        Response response = dynamicClient.executeSync(mutation, variables);
        if (response.hasError()) {
            throw new RuntimeException("Buffer API error for sponsor #" + sponsorId + ": "
                    + response.getErrors().stream().map(e -> e.getMessage()).collect(Collectors.joining(", ")));
        }

        JsonObject data = response.getData();
        JsonObject createPost = data.getJsonObject("createPost");
        String typename = createPost.getString("__typename");

        if ("PostActionSuccess".equals(typename)) {
            String postId = createPost.getJsonObject("post").getString("id");
            Log.infof("Scheduled Buffer post %s for sponsor #%d on channel %s", postId, sponsorId, channelId);
            return postId;
        } else {
            String message = createPost.getString("message", "Unknown error");
            throw new RuntimeException("Buffer API error (" + typename + "): " + message);
        }
    }

    public void stopScheduler() {
        if (!isRunning) {
            return;
        }

        scheduler.unscheduleJob(SCHEDULER_IDENTITY);
        isRunning = false;
        Log.info("Sponsor Buffer scheduler stopped");
    }

    public boolean isRunning() {
        return isRunning;
    }

    public SchedulerStatus getStatus() {
        long totalSponsors = Sponsor.count("level != ?1", SponsorShip.PreviousYears);
        long processedSponsors = Sponsor.count("bufferPost IS NOT NULL AND level != ?1", SponsorShip.PreviousYears);
        long remainingSponsors = Sponsor.count("bufferPost IS NULL AND level != ?1", SponsorShip.PreviousYears);

        return new SchedulerStatus(isRunning, totalSponsors, processedSponsors, remainingSponsors);
    }

    public static class SchedulerStatus {
        public boolean running;
        public long totalSponsors;
        public long processedSponsors;
        public long remainingSponsors;

        public SchedulerStatus(boolean running, long totalSponsors, long processedSponsors, long remainingSponsors) {
            this.running = running;
            this.totalSponsors = totalSponsors;
            this.processedSponsors = processedSponsors;
            this.remainingSponsors = remainingSponsors;
        }
    }
}
