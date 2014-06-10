package org.andlon.simpleweb;

import java.util.Map;

/**
 * Created by Andreas on 31.05.2014.
 */
public interface HttpRequest {
    public enum Type {
        GET,
        HEAD,
        POST,
        PUT,
        DELETE,
        TRACE,
        OPTIONS,
        CONNECT,
        PATCH
    }

    public Type type();
    public String uri();
    public String version();

    public String host();

    public Map<String, String> headers();
    public String body();
}
