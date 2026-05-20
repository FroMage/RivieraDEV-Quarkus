package graphql.buffer;

import org.eclipse.microprofile.graphql.Name;

import java.util.ArrayList;
import java.util.List;

@Name("CreatePostInput")
public class CreatePostInput {
    public String text;
    public String channelId;
    public SchedulingType schedulingType;
    public Mode mode;
    public String dueAt;
    public List<AssetInput> assets = new ArrayList<>();

    public CreatePostInput() {
    }

    public CreatePostInput(String text, String channelId, SchedulingType schedulingType, Mode mode, String dueAt) {
        this.text = text;
        this.channelId = channelId;
        this.schedulingType = schedulingType;
        this.mode = mode;
        this.dueAt = dueAt;
    }
}
