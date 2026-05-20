package graphql.buffer;

import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Query;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@GraphQLClientApi(configKey = "buffer")
@ApplicationScoped
public interface BufferClient {

    @Query
    Account account();

    @Query
    List<Channel> channels(@Name("input") @NonNull ChannelsInput input);
}
