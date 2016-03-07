package models;

import java.util.Map;

/**
 * Created by dimi5963 on 3/6/16.
 */
public class TestRequest {

    private String method;
    private String uri;
    private String body;
    private Map<String, String> headers;

    public TestRequest(String method, String uri, String body, Map<String, String> headers) {
        this.method = method;
        this.uri = uri;
        this.body = body;
        this.headers = headers;
    }

    public String getMethod() {
        return method;
    }

    public String getUri() {
        return uri;
    }

    public String getBody() {
        return body;
    }

    public Map<String,String> getHeaders(){
        return headers;
    }
}
