package graphql.buffer;

import org.eclipse.microprofile.graphql.Name;

@Name("ChannelsInput")
public class ChannelsInput {
    public String organizationId;

    public ChannelsInput() {
    }

    public ChannelsInput(String organizationId) {
        this.organizationId = organizationId;
    }
}
