package services;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService
@ApplicationScoped
public interface TalkSummaryService {

    @SystemMessage("""
            You are a helpful assistant that creates Twitter/X posts for a tech conference called RivieraDEV.
            You will be given a talk title, speaker names, a description, and a URL.
            Your task is to compose an engaging tweet that:
            - Starts with the speaker names
            - Mentions the talk title
            - Includes a very short summary of what the talk is about
            - Ends with the URL (include it exactly as provided, do not modify it)
            - MUST be strictly under 280 characters total. Count carefully before responding.
            - If needed, shorten the summary aggressively or drop it entirely to stay under the limit.
            Use clear, direct language. Do not add hashtags or emojis.
            Return ONLY the tweet text, nothing else.
            """)
    @UserMessage("""
            Compose a tweet for this conference talk:

            Speakers: {speakers}
            Title: {title}
            URL: {url}

            Abstract:
            {description}
            """)
    String composeTweet(String speakers, String title, String url, String description);
}
