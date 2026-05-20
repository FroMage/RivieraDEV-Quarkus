package graphql.buffer;

public class RestProxyError implements PostActionPayload {
    public String message;
    public String link;
    public Integer code;
}
