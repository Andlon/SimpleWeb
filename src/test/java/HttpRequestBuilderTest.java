import org.testng.annotations.*;
import org.testng.Assert;
import org.andlon.simpleweb.*;
import java.util.Optional;

/**
 * Created by Andreas on 08.06.2014.
 */


public class HttpRequestBuilderTest {
    private String simpleRequest = "POST /index.html HTTP/1.1\r\nHost: www.example.com";

    @BeforeClass
    public void setUp() {

    }

    @Test()
    public void testRequestLine() throws MalformedRequestException {
        HttpRequestBuilder builder = new HttpRequestBuilder();

        for (char c : simpleRequest.toCharArray()) {
            builder.add(c);
        }

        Optional<HttpRequest> requestOptional = builder.request();
        Assert.assertTrue(requestOptional.isPresent());

        HttpRequest request = requestOptional.get();
        Assert.assertEquals(request.type(), HttpRequest.Type.POST);
        Assert.assertEquals(request.uri(), "/index.html");
        Assert.assertEquals(request.version(), "HTTP/1.1");
    }
}
