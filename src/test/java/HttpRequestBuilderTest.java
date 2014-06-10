import org.testng.annotations.*;
import org.testng.Assert;
import org.andlon.simpleweb.*;
import java.util.Optional;

/**
 * Created by Andreas on 08.06.2014.
 */


public class HttpRequestBuilderTest {
    private final String SIMPLEREQUEST = "POST /index.html HTTP/1.1\r\nHost: www.example.com\r\n\r\n";

    @BeforeClass
    public void setup() {

    }

    @DataProvider(name = "simpleRequests")
    public Object[][] createSimpleRequests() {
        return new Object[][] {
                { HttpRequest.Type.POST, "POST /index.html HTTP/1.1\r\nHost: www.example.com\r\n\r\n" },
                { HttpRequest.Type.GET, "GET /index.html HTTP/1.1\r\nHost: www.example.com\r\n\r\n" },
                { HttpRequest.Type.HEAD, "HEAD /index.html HTTP/1.1\r\nHost: www.example.com\r\n\r\n" },
                { HttpRequest.Type.PUT, "PUT /index.html HTTP/1.1\r\nHost: www.example.com\r\n\r\n" },
                { HttpRequest.Type.DELETE, "DELETE /index.html HTTP/1.1\r\nHost: www.example.com\r\n\r\n" },
                { HttpRequest.Type.TRACE, "TRACE /index.html HTTP/1.1\r\nHost: www.example.com\r\n\r\n" },
                { HttpRequest.Type.OPTIONS, "OPTIONS /index.html HTTP/1.1\r\nHost: www.example.com\r\n\r\n" },
                { HttpRequest.Type.CONNECT, "CONNECT /index.html HTTP/1.1\r\nHost: www.example.com\r\n\r\n" },
                { HttpRequest.Type.PATCH, "PATCH /index.html HTTP/1.1\r\nHost: www.example.com\r\n\r\n" }
        };
    }

    @DataProvider(name = "simpleRequestsExceptFail")
    public Object[][] createSimpleRequestsExpectedToFail() {
        return new Object[][] {
                { "GET  /index.html HTTP/1.1\r\nHost: www.example.com\r\n\r\n" },   // Extra space after GET
                { "POST HTTP/1.1\r\nHost: www.example.com\r\n\r\n" },               // No URI
                { "GET / HTTP/1.1\nHost: www.example.com\r\n\r\n"},                 // No \r
                { "NA / HTTP/1.1\r\nHost: www.example.com\r\n\r\n"}                 // Invalid method
        };
    }

    @Test(dataProvider = "simpleRequests")
    public void testRequestTypes(HttpRequest.Type expectedType, String requestLine) throws MalformedRequestException {
        HttpRequestBuilder builder = new HttpRequestBuilder();

        for (char c : requestLine.toCharArray())
            builder.add(c);

        HttpRequest request = builder.request();
        Assert.assertNotNull(request);
        Assert.assertEquals(request.type(), expectedType);
        Assert.assertEquals(request.uri(), "/index.html");
        Assert.assertEquals(request.version(), "HTTP/1.1");
        Assert.assertTrue(builder.isComplete());
    }

    @Test(dataProvider = "simpleRequestsExceptFail", expectedExceptions = MalformedRequestException.class)
    public void testMalformedSimpleRequests(String requestLine)
            throws MalformedRequestException {
        HttpRequestBuilder builder = new HttpRequestBuilder();

        for (char c : requestLine.toCharArray())
            builder.add(c);
    }

    @Test()
    public void testHostField() throws MalformedRequestException {
        HttpRequestBuilder builder = new HttpRequestBuilder();

        for (char c : SIMPLEREQUEST.toCharArray())
            builder.add(c);

        HttpRequest request = builder.request();
        Assert.assertNotNull(request);
        Assert.assertEquals(request.host(), "www.example.com");
    }
}
