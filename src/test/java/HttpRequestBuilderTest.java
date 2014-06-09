import org.testng.annotations.*;
import org.testng.Assert;
import org.andlon.simpleweb.*;
import java.util.Optional;

/**
 * Created by Andreas on 08.06.2014.
 */


public class HttpRequestBuilderTest {
    @BeforeClass
    public void setup() {

    }

    @DataProvider(name = "simpleRequests")
    public Object[][] createSimpleRequests() {
        return new Object[][] {
                { HttpRequest.Type.POST, "POST /index.html HTTP/1.1\r\nHost: www.example.com" },
                { HttpRequest.Type.GET, "GET /index.html HTTP/1.1\r\nHost: www.example.com" },
                { HttpRequest.Type.HEAD, "HEAD /index.html HTTP/1.1\r\nHost: www.example.com" },
                { HttpRequest.Type.PUT, "PUT /index.html HTTP/1.1\r\nHost: www.example.com" },
                { HttpRequest.Type.DELETE, "DELETE /index.html HTTP/1.1\r\nHost: www.example.com" },
                { HttpRequest.Type.TRACE, "TRACE /index.html HTTP/1.1\r\nHost: www.example.com" },
                { HttpRequest.Type.OPTIONS, "OPTIONS /index.html HTTP/1.1\r\nHost: www.example.com" },
                { HttpRequest.Type.CONNECT, "CONNECT /index.html HTTP/1.1\r\nHost: www.example.com" },
                { HttpRequest.Type.PATCH, "PATCH /index.html HTTP/1.1\r\nHost: www.example.com" }

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
    }
}
