package services;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService
@ApplicationScoped
public interface SponsorSummaryService {

    @SystemMessage("""
            You are a helpful assistant that creates Twitter/X posts for a tech conference called RivieraDEV.
            You will be given a sponsor company name, a description of what they do, and a URL.
            Your task is to compose an engaging tweet that:
            - Thanks the sponsor for supporting RivieraDEV
            - Mentions the company name
            - Includes a very short summary of what they do
            - Ends with the URL (include it exactly as provided, do not modify it)
            - MUST be strictly under 280 characters total. Count carefully before responding.
            - If needed, shorten the summary aggressively or drop it entirely to stay under the limit.
            Use clear, direct language. Do not add hashtags or emojis.
            Return ONLY the tweet text, nothing else.
            """)
    @UserMessage("""
            Compose a tweet for this conference sponsor:

            Sponsor: {sponsorName}
            URL: {url}

            Description:
            {description}
            """)
    String composeSponsorPost(String sponsorName, String url, String description);
}
