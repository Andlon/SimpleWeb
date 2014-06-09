package org.andlon.simpleweb;

import java.util.Queue;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Created by Andreas on 31.05.2014.
 *
 * Incremental HTTP Request parser
 */
public class HttpRequestBuilder {
    private Queue<HttpRequest> m_requests = new ArrayDeque<HttpRequest>();
    private State m_state = States.BEGIN;
    private IncrementalHttpRequest m_request;
    private StringBuilder m_builder = new StringBuilder();

    public void add(char c) throws MalformedRequestException {
        if (m_state.process(this, c)) {
            m_builder.append(c);
        }
    }

    public HttpRequest request() {
        return m_request;
    }

    private void transition(State state) {
        m_state = state;
    }

    private void initiateRequest(String requestType) throws MalformedRequestException {
        m_request = IncrementalHttpRequest.fromRequestString(requestType);

        if (m_request == null) {
            throw new MalformedRequestException("Invalid request type: ".concat(requestType));
        }
    }

    private String takeText() {
        String text = m_builder.toString();
        m_builder.setLength(0);
        return text;
    }

    private MutableHttpRequest mutableRequest() { return m_request; }

    static private interface MutableHttpRequest extends HttpRequest {
        public void setVersion(String version);
        public void setUri(String uri);
    }

    static private class IncrementalHttpRequest implements MutableHttpRequest {
        private Type m_type;
        private String m_version = new String();
        private String m_uri = new String();

        public IncrementalHttpRequest(Type type) {
            m_type = type;
        }

        public Type type() { return m_type; }
        public Map<String, String> headers() { return new HashMap <String, String>(); }
        public String body() { return new String(); }



        @Override
        public String uri() {
            return m_uri;
        }

        public void setUri(String uri) {
            m_uri = uri;
        }

        @Override
        public String version() {
            return m_version;
        }

        public void setVersion(String version) {
            m_version = version;
        }

        private static final HashMap<String, Type> typeMap = new HashMap<String, Type>() {{
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
            Type type = typeMap.get(request);
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
                return true;
            }
        },
        HEADER_VALUE {
            @Override
            public boolean process(HttpRequestBuilder builder, char c) throws MalformedRequestException {
                return true;
            }
        },
        HEADER_CR {
            @Override
            public boolean process(HttpRequestBuilder builder, char c) throws MalformedRequestException {
                return true;
            }
        },
        BOUNDARY {
            @Override
            public boolean process(HttpRequestBuilder builder, char c) throws MalformedRequestException {
                return true;
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
