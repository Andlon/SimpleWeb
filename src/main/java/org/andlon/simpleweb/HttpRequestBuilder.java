package org.andlon.simpleweb;

import java.util.Queue;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Andreas on 31.05.2014.
 *
 * Incremental HTTP Request parser
 */
public class HttpRequestBuilder {
    private State state = States.BEGIN;
    private IncrementalHttpRequest request;
    private StringBuilder builder = new StringBuilder();
    private String currentHeader = new String();
    private HashMap<String, String> headers = new HashMap<String, String>();

    public void add(char c) throws MalformedRequestException {
        if (state.process(this, c)) {
            builder.append(c);
        }
    }

    public HttpRequest request() {
        return request;
    }

    public boolean isComplete() {
        return state == States.END;
    }

    private void transition(State state) {
        this.state = state;
    }

    private void initiateRequest(String requestType) throws MalformedRequestException {
        request = IncrementalHttpRequest.fromRequestString(requestType);

        if (request == null) {
            throw new MalformedRequestException("Invalid request type: ".concat(requestType));
        }
    }

    private String takeText() {
        String text = builder.toString();
        builder.setLength(0);
        return text;
    }

    private void setCurrentHeaderName(String header) {
        currentHeader = header;
    }

    private String currentHeaderName() {
        return currentHeader;
    }

    private void setHeader(String name, String value) {
        // Since headers are case-insensitive, we only store lower-case names,
        // and per the HTTP specifications we trim the values
        headers.put(name.toLowerCase(), value.trim());
    }

    private void commitHeaders() {
        assert(request != null);

        // Extract known headers
        request.setHost(headers.remove("host"));
    }

    private boolean hasBody() {
        return false;
    }

    private MutableHttpRequest mutableRequest() { return request; }

    static private interface MutableHttpRequest extends HttpRequest {
        public void setVersion(String version);
        public void setUri(String uri);
        public void setHost(String host);
    }

    static private class IncrementalHttpRequest implements MutableHttpRequest {
        private Type type;
        private String version = new String();
        private String uri = new String();
        private String host;

        public IncrementalHttpRequest(Type type) {
            this.type = type;
        }

        @Override
        public Type type() { return type; }

        @Override
        public String uri() {
            return uri;
        }

        @Override
        public String version() {
            return version;
        }

        @Override
        public String host() { return host; }

        @Override
        public Map<String, String> headers() { return new HashMap <String, String>(); }

        @Override
        public String body() { return new String(); }

        @Override
        public void setUri(String uri) {
            this.uri = uri;
        }

        @Override
        public void setVersion(String version) {
            this.version = version;
        }

        @Override
        public void setHost(String host) { this.host = host; }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();

            builder.append(type().toString());
            builder.append(' ');
            builder.append(uri());
            builder.append(' ');
            builder.append(version());
            builder.append("\r\n");

            // Skip headers for now
            builder.append("\r\n");

            return builder.toString();
        }

        private static final HashMap<String, Type> TYPEMAP = new HashMap<String, Type>() {{
            put("GET", Type.GET);
            put("HEAD", Type.HEAD);
            put("POST", Type.POST);
            put("PUT", Type.PUT);
            put("DELETE", Type.DELETE);
            put("TRACE", Type.TRACE);
            put("OPTIONS", Type.OPTIONS);
            put("CONNECT", Type.CONNECT);
            put("PATCH", Type.PATCH);
        }};

        static public IncrementalHttpRequest fromRequestString(String request) {
            Type type = TYPEMAP.get(request);
            return type == null ? null : new IncrementalHttpRequest(type);
        }
    }

    private interface State {
        /**
         * Processes a single character of a request, before appending the character to the line string.
         * CR or LF characters are not added to the line string.
         * @param builder
         * @param c The character to be processed
         * @throws MalformedRequestException
         * @return returns true if the character should be added to the current line, otherwise false.
         */
        public boolean process(HttpRequestBuilder builder, char c) throws MalformedRequestException;
    }

    private enum States implements State {
        BEGIN {
            @Override
            public boolean process(HttpRequestBuilder builder, char c) throws MalformedRequestException {
                switch (c) {
                    case '\r':
                        builder.transition(BEGIN_CR);
                    case '\n':
                        return false;
                    default:
                        builder.transition(REQUESTLINE_TYPE);
                        return true;
                }
            }
        },
        BEGIN_CR {
            @Override
            public boolean process(HttpRequestBuilder builder, char c) throws MalformedRequestException {
                switch (c) {
                    case '\r':
                        throw(new MalformedRequestException("Invalid character sequence: \\r\\r."));
                    case '\n':
                        return false;
                    default:
                        builder.transition(REQUESTLINE_TYPE);
                        return true;
                }
            }
        },
        REQUESTLINE_TYPE {
            @Override
            public boolean process(HttpRequestBuilder builder, char c) throws MalformedRequestException {
                switch (c) {
                    case ' ':
                        String type = builder.takeText();
                        builder.initiateRequest(type);
                        builder.transition(REQUESTLINE_URI);
                        return false;
                    case '\r':
                    case '\n':
                        throw(new MalformedRequestException("Request line terminated prematurely."));
                    default:
                        return true;
                }
            }
        },
        REQUESTLINE_URI {
            @Override
            public boolean process(HttpRequestBuilder builder, char c) throws MalformedRequestException {
                switch (c) {
                    case ' ':
                        String uri = builder.takeText();
                        if (uri.isEmpty())
                            throw(new MalformedRequestException("URI cannot be empty."));
                        builder.mutableRequest().setUri(uri);
                        builder.transition(REQUESTLINE_VERSION);
                        return false;
                    case '\r':
                    case '\n':
                        throw(new MalformedRequestException("Request line terminated prematurely."));
                    default:
                        return true;
                }
            }
        },
        REQUESTLINE_VERSION {
            @Override
            public boolean process(HttpRequestBuilder builder, char c) throws MalformedRequestException {
                switch (c) {
                    case '\r':
                        String version = builder.takeText();
                        if (version.isEmpty())
                            throw(new MalformedRequestException("Version string cannot be empty."));
                        builder.mutableRequest().setVersion(version);
                        builder.transition(REQUESTLINE_CR);
                        return false;
                    case '\n':
                        throw(new MalformedRequestException("Newline without preceding carriage return."));
                    default:
                        return true;
                }
            }
        },
        REQUESTLINE_CR {
            @Override
            public boolean process(HttpRequestBuilder builder, char c) throws MalformedRequestException {
                switch (c) {
                    case '\n':
                        builder.transition(HEADER_KEY);
                        return false;
                    default:
                        throw(new MalformedRequestException("Expected newline, got " + c));
                }
            }
        },
        HEADER_KEY {
            @Override
            public boolean process(HttpRequestBuilder builder, char c) throws MalformedRequestException {
                switch (c) {
                    case ':':
                        String header = builder.takeText();
                        if (header.isEmpty())
                            throw(new MalformedRequestException("Header value cannot be empty."));

                        builder.setCurrentHeaderName(header);
                        builder.transition(HEADER_VALUE);
                        return false;
                    case '\r':
                        String text = builder.takeText();
                        if (text.isEmpty()) {
                            // Signals headers are completely specified, move on to HEADER_KEY_CR,
                            // which checks whether '\n' is the next character
                            builder.transition(BOUNDARY_CR);
                            return false;
                        }
                    case '\n':
                        throw(new MalformedRequestException(
                                "Unexpected carriage return or newline character in header name."));
                    default:
                        return true;
                }
            }
        },
        HEADER_VALUE {
            @Override
            public boolean process(HttpRequestBuilder builder, char c) throws MalformedRequestException {
                switch (c) {
                    case '\r':
                        String value = builder.takeText();
                        String header = builder.currentHeaderName();
                        builder.setHeader(header, value);
                        builder.transition(HEADER_CR);
                        return false;
                    case '\n':
                        // Note: currently don't support multi-line header fields. My impression so far
                        // is that this is a deprecated feature anyway - need to do more research
                        throw(new MalformedRequestException("Unexpected newline in field value."));
                    default:
                        return true;
                }
            }
        },
        HEADER_CR {
            @Override
            public boolean process(HttpRequestBuilder builder, char c) throws MalformedRequestException {
                switch (c) {
                    case '\n':
                        builder.transition(HEADER_KEY);
                        return false;
                    default:
                        throw(new MalformedRequestException("Expected newline, got " + c));
                }
            }
        },
        BOUNDARY_CR {
            @Override
            public boolean process(HttpRequestBuilder builder, char c) throws MalformedRequestException {
                switch (c) {
                    case '\n':
                        builder.commitHeaders();
                        builder.transition(builder.hasBody() ? BODY : END);
                        return false;
                    default:
                        throw(new MalformedRequestException("Expected newline. Got " + c));
                }
            }
        },
        BODY {
            @Override
            public boolean process(HttpRequestBuilder builder, char c) throws MalformedRequestException {
                return true;
            }
        },
        END {
            @Override
            public boolean process(HttpRequestBuilder builder, char c) throws MalformedRequestException {
                return false;
            }
        }
    }
}
