package services;

import graphql.buffer.Account;
import graphql.buffer.BufferClient;
import graphql.buffer.Channel;
import graphql.buffer.ChannelsInput;
import graphql.buffer.Mode;
import graphql.buffer.SchedulingType;
import io.quarkus.logging.Log;
import io.smallrye.graphql.client.GraphQLClient;
import io.smallrye.graphql.client.Response;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import jakarta.transaction.Transactional;
import model.BufferPost;
import model.Talk;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@ApplicationScoped
public class TalkBufferSchedulerService {

    static String nameWithHandle(String name, String handle) {
        if (handle != null && !handle.isBlank()) {
            String h = handle.startsWith("@") ? handle : "@" + handle;
            return name + " (" + h + ")";
        }
        return name;
    }

    @Inject
    ScheduledExecutorService executor;

    @Inject
    TalkSummaryService summaryService;

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
            Log.warn("Scheduler already running");
            return;
        }

        // Get organization ID
        Account account = bufferClient.account();
        if (account.organizations == null || account.organizations.isEmpty()) {
            throw new RuntimeException("No organizations found in Buffer account");
        }
        String organizationId = account.organizations.get(0).id;
        Log.infof("Using Buffer organization: %s (%s)", account.organizations.get(0).name, organizationId);

        // Get channel IDs
        List<Channel> channels = bufferClient.channels(new ChannelsInput(organizationId));
        channels.forEach(c -> Log.infof("Found channel: %s (%s)", c.service, c.id));

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

        Log.infof("Starting Buffer scheduler for %d short-text channels and LinkedIn: %s",
                shortTextChannelIds.size(), linkedInChannelId != null ? "yes" : "no");

        scheduleNext(0);
    }

    private void scheduleNext(long delaySeconds) {
        executor.schedule(() -> {
            try {
                processNextTalk();
            } finally {
                if (isRunning) {
                    scheduleNext(30);
                }
            }
        }, delaySeconds, TimeUnit.SECONDS);
    }

    @ActivateRequestContext
    @Transactional
    public void processNextTalk() {
        if (!isRunning) {
            return;
        }

        // Find first talk without buffer post, ordered by id
        Talk talk = Talk.find("bufferPost IS NULL ORDER BY id").firstResult();

        if (talk == null) {
            Log.info("No more talks to process, stopping scheduler");
            stopScheduler();
            return;
        }

        try {
            Log.infof("Processing talk #%d: %s", talk.id, talk.getTitle());

            // Get title and description
            String title = talk.titleEN != null && !talk.titleEN.isBlank()
                    ? talk.titleEN
                    : talk.titleFR;
            String description = talk.descriptionEN != null && !talk.descriptionEN.isBlank()
                    ? talk.descriptionEN
                    : talk.descriptionFR;

            if (title == null || title.isBlank() || description == null || description.isBlank()) {
                Log.warnf("Talk #%d has no title or description, marking as skipped", talk.id);
                BufferPost bp = new BufferPost();
                bp.talk = talk;
                bp.error = "SKIPPED";
                bp.persist();
                return;
            }

            String twitterSpeakers = talk.speakers.stream()
                    .map(s -> nameWithHandle(s.firstName + " " + s.lastName, s.twitterAccount))
                    .collect(Collectors.joining(", "));
            String linkedInSpeakers = talk.speakers.stream()
                    .map(s -> nameWithHandle(s.firstName + " " + s.lastName, s.linkedInAccount))
                    .collect(Collectors.joining(", "));
            String talkUrl = baseUrl + "/session/" + talk.id;

            // Call Gemini to compose tweet
            Log.infof("Calling Gemini API for talk #%d", talk.id);
            String shortText = summaryService.composeTweet(twitterSpeakers, title, talkUrl, description);
            Log.infof("Generated tweet (%d chars): %s", shortText.length(), shortText);

            // Hard limit: truncate text before the URL if AI exceeded 280 chars
            if (shortText.length() > 280) {
                Log.warnf("Tweet exceeds 280 chars (%d), truncating", shortText.length());
                int maxTextLength = 280 - talkUrl.length() - 5; // 5 for "... " + newline
                shortText = shortText.replace(talkUrl, "").stripTrailing();
                if (shortText.length() > maxTextLength) {
                    shortText = shortText.substring(0, maxTextLength) + "...";
                }
                shortText = shortText + " " + talkUrl;
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

            String linkedInText = linkedInSpeakers + " presents: " + title + "\n\n" + description + "\n\n" + talkUrl;
            if (linkedInText.length() > 3000) {
                String prefix = linkedInSpeakers + " presents: " + title + "\n\n";
                String suffix = "\n\n" + talkUrl;
                int maxDesc = 3000 - prefix.length() - suffix.length() - 3;
                linkedInText = prefix + description.substring(0, maxDesc) + "..." + suffix;
            }

            BufferPost bp = new BufferPost();
            bp.talk = talk;
            bp.scheduledDate = nextSlot;

            // Post to Twitter + Bluesky channels
            for (String channelId : shortTextChannelIds) {
                String postId = postToBuffer(mutation, shortText, channelId, nextSlot, talk.id);
                if (channelId.equals(twitterChannelId)) {
                    bp.twitterPostId = postId;
                } else {
                    bp.blueskyPostId = postId;
                }
            }

            // Post to LinkedIn
            if (linkedInChannelId != null) {
                bp.linkedInPostId = postToBuffer(mutation, linkedInText, linkedInChannelId, nextSlot, talk.id);
            }

            bp.persist();

            Log.infof("Successfully scheduled talk #%d (twitter=%s, bluesky=%s, linkedin=%s)",
                    talk.id, bp.twitterPostId, bp.blueskyPostId, bp.linkedInPostId);

        } catch (Exception e) {
            Log.errorf(e, "Error processing talk #%d: %s", talk.id, e.getMessage());
            BufferPost bp = new BufferPost();
            bp.talk = talk;
            bp.error = e.getMessage();
            bp.persist();
        }
    }

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final LocalTime MORNING = LocalTime.of(10, 0);
    private static final LocalTime AFTERNOON = LocalTime.of(14, 0);

    private Instant computeNextSlot() {
        BufferPost latest = BufferPost.find("scheduledDate IS NOT NULL ORDER BY scheduledDate DESC").firstResult();
        Instant maxScheduled = latest != null ? latest.scheduledDate : null;

        ZonedDateTime now = ZonedDateTime.now(PARIS);
        ZonedDateTime nextSlot;

        if (maxScheduled == null) {
            nextSlot = now.plusDays(1).with(MORNING);
        } else {
            ZonedDateTime last = maxScheduled.atZone(PARIS);
            if (last.toLocalTime().isBefore(AFTERNOON)) {
                nextSlot = last.with(AFTERNOON);
            } else {
                nextSlot = last.plusDays(1).with(MORNING);
            }
        }

        if (!nextSlot.isAfter(now)) {
            nextSlot = now.plusDays(1).with(MORNING);
        }

        Log.infof("Next scheduled slot: %s", nextSlot);
        return nextSlot.toInstant();
    }

    private String postToBuffer(String mutation, String text, String channelId, Instant scheduledAt, Long talkId) throws Exception {
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
            throw new RuntimeException("Buffer API error for talk #" + talkId + ": "
                    + response.getErrors().stream().map(e -> e.getMessage()).collect(Collectors.joining(", ")));
        }

        JsonObject data = response.getData();
        JsonObject createPost = data.getJsonObject("createPost");
        String typename = createPost.getString("__typename");

        if ("PostActionSuccess".equals(typename)) {
            String postId = createPost.getJsonObject("post").getString("id");
            Log.infof("Scheduled Buffer post %s for talk #%d on channel %s", postId, talkId, channelId);
            return postId;
        } else {
            String message = createPost.getString("message", "Unknown error");
            throw new RuntimeException("Buffer API error (" + typename + "): " + message);
        }
    }

    @Transactional
    public void deleteBufferPost(Long id) throws Exception {
        BufferPost bp = BufferPost.findById(id);
        if (bp == null) {
            throw new RuntimeException("BufferPost not found: " + id);
        }

        String mutation = """
                mutation($input: DeletePostInput!) {
                    deletePost(input: $input) {
                        __typename
                        ... on DeletePostSuccess { id }
                        ... on VoidMutationError { message }
                    }
                }
                """;

        for (String postId : List.of(
                bp.twitterPostId != null ? bp.twitterPostId : "",
                bp.blueskyPostId != null ? bp.blueskyPostId : "",
                bp.linkedInPostId != null ? bp.linkedInPostId : "")) {
            if (postId.isEmpty()) continue;
            try {
                Map<String, Object> variables = Map.of("input", Map.of("id", postId));
                Response response = dynamicClient.executeSync(mutation, variables);
                if (response.hasError()) {
                    Log.warnf("Failed to delete Buffer post %s: %s", postId,
                            response.getErrors().stream().map(e -> e.getMessage()).collect(Collectors.joining(", ")));
                    continue;
                }
                JsonObject result = response.getData().getJsonObject("deletePost");
                String typename = result.getString("__typename");
                if ("DeletePostSuccess".equals(typename)) {
                    Log.infof("Deleted Buffer post %s", postId);
                } else {
                    Log.warnf("Failed to delete Buffer post %s: %s", postId, result.getString("message", "Unknown error"));
                }
            } catch (Exception e) {
                Log.warnf(e, "Error deleting Buffer post %s", postId);
            }
        }

        if(bp.sponsor != null)
            bp.sponsor.bufferPost = null;
        if(bp.talk != null)
            bp.talk.bufferPost = null;
        bp.delete();
        if(bp.talk != null)
            Log.infof("Deleted BufferPost #%d for talk #%d", id, bp.talk.id);
        else
            Log.infof("Deleted BufferPost #%d for sponsor #%d", id, bp.sponsor.id);
    }

    public void stopScheduler() {
        if (!isRunning) {
            return;
        }

        isRunning = false;
        Log.info("Buffer scheduler stopped");
    }

    public boolean isRunning() {
        return isRunning;
    }

    public SchedulerStatus getStatus() {
        long totalTalks = Talk.count();
        long processedTalks = Talk.count("bufferPost IS NOT NULL");
        long remainingTalks = Talk.count("bufferPost IS NULL");

        return new SchedulerStatus(isRunning, totalTalks, processedTalks, remainingTalks);
    }

    public static class SchedulerStatus {
        public boolean running;
        public long totalTalks;
        public long processedTalks;
        public long remainingTalks;

        public SchedulerStatus(boolean running, long totalTalks, long processedTalks, long remainingTalks) {
            this.running = running;
            this.totalTalks = totalTalks;
            this.processedTalks = processedTalks;
            this.remainingTalks = remainingTalks;
        }
    }
}
