package util;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService( 
		retrievalAugmentor = ScheduleDocumentRetreiver.class
)
public interface ScheduleAI {

    @SystemMessage("You are a computer science conference organiser") 
    @UserMessage("""
    			I want to find the talks from the conference program that match my interests and constraints.
                Give me the list of talks as a valid JSON array, without formatting, with each element being a JSON object containing the
                title as 'title', id as 'id', and reason in plain text why you recommend it as 'reason' object members.
                These are my interests and constraints: {topics}.
            """)
    String findTalks(String topics);
}