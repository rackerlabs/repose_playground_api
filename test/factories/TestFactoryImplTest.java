package factories;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.TestRequest;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by dimi5963 on 3/7/16.
 */
public class TestFactoryImplTest {

    @Test
    public void testTranslateRequestSuccess() {

        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
        objectNode.put("method", "POST");
        objectNode.put("url", "/v2/servers");
        objectNode.put("body", "{'message':'success'}");
        ArrayNode headers = objectNode.objectNode().arrayNode();
        ObjectNode headerNode = JsonNodeFactory.instance.objectNode();
        headerNode.put("name", "test");
        headerNode.put("value", "T1");
        headers.add(headerNode);
        objectNode.put("headers", headers);


        TestRequest testRequest =
                new TestFactoryImpl().translateRequest(objectNode);

        Map<String, String> headerMap = new HashMap<String, String>(){
            {
                put("test", "T1");
            }
        };

        assertEquals("POST", testRequest.getMethod());
        assertEquals("/v2/servers", testRequest.getUri());
        assertEquals("{'message':'success'}", testRequest.getBody());
        assertEquals(headerMap, testRequest.getHeaders());

    }

    @Test
    public void testTranslateRequestRequestBodyNull() {
        assertNull(new TestFactoryImpl().translateRequest(null));

        Map<String, String> headerMap = new HashMap<String, String>(){
            {
                put("test", "T1");
            }
        };
    }

    @Test
    public void testTranslateRequestHeadersNull() {

        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
        objectNode.put("method", "POST");
        objectNode.put("url", "/v2/servers");
        objectNode.put("body", "{'message':'success'}");
        ArrayNode headers = objectNode.objectNode().arrayNode();
        objectNode.put("headers", headers);


        TestRequest testRequest =
                new TestFactoryImpl().translateRequest(objectNode);

        Map<String, String> headerMap = new HashMap<String, String>();

        assertEquals("POST", testRequest.getMethod());
        assertEquals("/v2/servers", testRequest.getUri());
        assertEquals("{'message':'success'}", testRequest.getBody());
        assertEquals(headerMap, testRequest.getHeaders());

    }

    @Test
    public void testTranslateRequestNoHeaders() {

        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
        objectNode.put("method", "POST");
        objectNode.put("url", "/v2/servers");
        objectNode.put("body", "{'message':'success'}");


        TestRequest testRequest =
                new TestFactoryImpl().translateRequest(objectNode);

        Map<String, String> headerMap = new HashMap<String, String>();

        assertEquals("POST", testRequest.getMethod());
        assertEquals("/v2/servers", testRequest.getUri());
        assertEquals("{'message':'success'}", testRequest.getBody());
        assertEquals(headerMap, testRequest.getHeaders());

    }

    @Test
    public void testTranslateRequestDefaults() {

        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();


        TestRequest testRequest =
                new TestFactoryImpl().translateRequest(objectNode);

        Map<String, String> headerMap = new HashMap<String, String>();

        assertEquals("GET", testRequest.getMethod());
        assertEquals("/", testRequest.getUri());
        assertEquals("", testRequest.getBody());
        assertEquals(headerMap, testRequest.getHeaders());

    }

    @Test
    public void testGenerateDebugMessageMapSuccess() {
        List<String> httpDebugLogList = new ArrayList<String>(){
            {
                add("2016-03-07 21:57:25,645 263018283 [qtp285433336-4446] DEBUG org.apache.http.impl.conn.PoolingClientConnectionManager - Connection request: [route: {}->http://repose-origin-732438-7-3-2-0:8000][total kept alive: 0; route allocated: 0 of 200; total allocated: 0 of 400]");
                add("2016-03-07 21:57:25,655 263018293 [qtp285433336-4446] DEBUG org.apache.http.impl.conn.PoolingClientConnectionManager - Connection leased: [id: 0][route: {}->http://repose-origin-732438-7-3-2-0:8000][total kept alive: 0; route allocated: 1 of 200; total allocated: 1 of 400]");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.impl.conn.PoolingClientConnectionManager - Connection [id: 0][route: {}->http://repose-origin-732438-7-3-2-0:8000] can be kept alive indefinitely");

                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  >> \"GET / HTTP/1.1[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  >> \"via: 1.1 localhost:8080 (Repose/7.3.2.0)[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  >> \"x-forwarded-for: 172.17.0.1[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  >> \"Host: repose-origin-732438-7-3-2-0:8000[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  >> \"x-trans-id: eyJyZXF1ZXN0SWQiOiIyOTNmMzYwMS1jOWUxLTQ5ZjctOWJhMC01MGE5OTVjZGJkNzkiLCJvcmlnaW4iOm51bGx9[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  >> \"x-pp-user: dima;q=0.3[\\r][\\n]\"");

                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  << \"HTTP/1.1 200 OK[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  << \"X-Powered-By: Express[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  << \"content-type: application/json[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  << \"Content-Length: 24[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  << \"Date: Mon, 07 Mar 2016 21:57:25 GMT[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  << \"Connection: keep-alive[\\r][\\n]\"");

                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.impl.conn.PoolingClientConnectionManager - Connection released: [id: 0][route: {}->http://repose-origin-732438-7-3-2-0:8000][total kept alive: 1; route allocated: 1 of 200; total allocated: 1 of 400]");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.impl.conn.PoolingClientConnectionManager - Connection released: [id: 0][route: {}->http://repose-origin-732438-7-3-2-0:8000][total kept alive: 1; route allocated: 1 of 200; total allocated: 1 of 400]");
            }
        };

        Map<?,?> debugMessageMap = new TestFactoryImpl().generateDebugMessageMap(httpDebugLogList);
        assertEquals(2, debugMessageMap.size());

        List<String> poolMessages =
                (List<String>)debugMessageMap.get("poolMessages");

        assertEquals(5, poolMessages.size());
        assertTrue(poolMessages.contains("2016-03-07 21:57:25,645 263018283 [qtp285433336-4446] DEBUG org.apache.http.impl.conn." +
                "PoolingClientConnectionManager - Connection request: [route: {}->" +
                "http://repose-origin-732438-7-3-2-0:8000][total kept alive: 0; route allocated: 0 of 200; " +
                "total allocated: 0 of 400]"));

        List<Map<String, List<String>>> externalRequests =
                (List<Map<String, List<String>>>)debugMessageMap.get("externalRequests");

        //verify only 1 entry
        assertEquals(1, externalRequests.size());

        Map<String, List<String>> singleRequest = externalRequests.get(0);

        assertNotNull(singleRequest.get("request"));
        assertNotNull(singleRequest.get("response"));

        List<String> request = singleRequest.get("request");
        List<String> response = singleRequest.get("response");

        assertEquals(6, request.size());
        assertEquals(6, response.size());

        for(String entry:request){
            assertTrue("Check if " + entry + " starts with >>", entry.startsWith(">>"));
        }

        for(String entry:response){
            assertTrue("Check if " + entry + " starts with <<", entry.startsWith("<<"));
        }
    }

    @Test
    public void testGenerateDebugMessageMapMultiEntrySuccess() {
        List<String> httpDebugLogList = new ArrayList<String>(){
            {
                add("2016-03-07 21:57:25,645 263018283 [qtp285433336-4446] DEBUG org.apache.http.impl.conn.PoolingClientConnectionManager - Connection request: [route: {}->http://repose-origin-732438-7-3-2-0:8000][total kept alive: 0; route allocated: 0 of 200; total allocated: 0 of 400]");
                add("2016-03-07 21:57:25,655 263018293 [qtp285433336-4446] DEBUG org.apache.http.impl.conn.PoolingClientConnectionManager - Connection leased: [id: 0][route: {}->http://repose-origin-732438-7-3-2-0:8000][total kept alive: 0; route allocated: 1 of 200; total allocated: 1 of 400]");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.impl.conn.PoolingClientConnectionManager - Connection [id: 0][route: {}->http://repose-origin-732438-7-3-2-0:8000] can be kept alive indefinitely");

                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  >> \"GET / HTTP/1.1[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  >> \"via: 1.1 localhost:8080 (Repose/7.3.2.0)[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  >> \"x-forwarded-for: 172.17.0.1[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  >> \"Host: repose-origin-732438-7-3-2-0:8000[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  >> \"x-trans-id: eyJyZXF1ZXN0SWQiOiIyOTNmMzYwMS1jOWUxLTQ5ZjctOWJhMC01MGE5OTVjZGJkNzkiLCJvcmlnaW4iOm51bGx9[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  >> \"x-pp-user: dima;q=0.3[\\r][\\n]\"");

                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  << \"HTTP/1.1 200 OK[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  << \"X-Powered-By: Express[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  << \"content-type: application/json[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  << \"Content-Length: 24[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  << \"Date: Mon, 07 Mar 2016 21:57:25 GMT[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  << \"Connection: keep-alive[\\r][\\n]\"");

                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  >> \"GET / HTTP/1.1[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  >> \"via: 1.1 localhost:8080 (Repose/7.3.2.0)[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  >> \"x-forwarded-for: 172.17.0.1[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  >> \"Host: repose-origin-732438-7-3-2-0:8000[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  >> \"x-trans-id: eyJyZXF1ZXN0SWQiOiIyOTNmMzYwMS1jOWUxLTQ5ZjctOWJhMC01MGE5OTVjZGJkNzkiLCJvcmlnaW4iOm51bGx9[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  >> \"x-pp-user: dima;q=0.3[\\r][\\n]\"");

                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  << \"HTTP/1.1 200 OK[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  << \"X-Powered-By: Express[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  << \"content-type: application/json[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  << \"Content-Length: 24[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  << \"Date: Mon, 07 Mar 2016 21:57:25 GMT[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  << \"Connection: keep-alive[\\r][\\n]\"");

                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.impl.conn.PoolingClientConnectionManager - Connection released: [id: 0][route: {}->http://repose-origin-732438-7-3-2-0:8000][total kept alive: 1; route allocated: 1 of 200; total allocated: 1 of 400]");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.impl.conn.PoolingClientConnectionManager - Connection released: [id: 0][route: {}->http://repose-origin-732438-7-3-2-0:8000][total kept alive: 1; route allocated: 1 of 200; total allocated: 1 of 400]");
            }
        };

        Map<?,?> debugMessageMap = new TestFactoryImpl().generateDebugMessageMap(httpDebugLogList);
        assertEquals(2, debugMessageMap.size());

        List<String> poolMessages =
                (List<String>)debugMessageMap.get("poolMessages");

        assertEquals(5, poolMessages.size());
        assertTrue(poolMessages.contains("2016-03-07 21:57:25,645 263018283 [qtp285433336-4446] DEBUG org.apache.http.impl.conn." +
                "PoolingClientConnectionManager - Connection request: [route: {}->" +
                "http://repose-origin-732438-7-3-2-0:8000][total kept alive: 0; route allocated: 0 of 200; " +
                "total allocated: 0 of 400]"));

        List<Map<String, List<String>>> externalRequests =
                (List<Map<String, List<String>>>)debugMessageMap.get("externalRequests");

        //verify only 2 entries
        assertEquals(2, externalRequests.size());

        for(Map<String, List<String>> singleRequest:externalRequests) {

            assertNotNull(singleRequest.get("request"));
            assertNotNull(singleRequest.get("response"));

            List<String> request = singleRequest.get("request");
            List<String> response = singleRequest.get("response");

            assertEquals(6, request.size());
            assertEquals(6, response.size());

            for (String entry : request) {
                assertTrue("Check if " + entry + " starts with >>", entry.startsWith(">>"));
            }

            for (String entry : response) {
                assertTrue("Check if " + entry + " starts with <<", entry.startsWith("<<"));
            }
        }
    }

    @Test
    public void testGenerateDebugMessageMapNullHttpDebugList() {
        List<String> httpDebugLogList = null;

        Map<?,?> debugMessageMap = new TestFactoryImpl().generateDebugMessageMap(httpDebugLogList);
        assertEquals(2, debugMessageMap.size());

        List<String> poolMessages =
                (List<String>)debugMessageMap.get("poolMessages");

        assertEquals(0, poolMessages.size());

        List<Map<String, List<String>>> externalRequests =
                (List<Map<String, List<String>>>)debugMessageMap.get("externalRequests");

        //verify NO entry
        assertEquals(0, externalRequests.size());

    }

    @Test
    public void testGenerateDebugMessageMapNoPoolMessages() {
        List<String> httpDebugLogList = new ArrayList<String>(){
            {
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  >> \"GET / HTTP/1.1[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  >> \"via: 1.1 localhost:8080 (Repose/7.3.2.0)[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  >> \"x-forwarded-for: 172.17.0.1[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  >> \"Host: repose-origin-732438-7-3-2-0:8000[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  >> \"x-trans-id: eyJyZXF1ZXN0SWQiOiIyOTNmMzYwMS1jOWUxLTQ5ZjctOWJhMC01MGE5OTVjZGJkNzkiLCJvcmlnaW4iOm51bGx9[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  >> \"x-pp-user: dima;q=0.3[\\r][\\n]\"");

                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  << \"HTTP/1.1 200 OK[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  << \"X-Powered-By: Express[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  << \"content-type: application/json[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  << \"Content-Length: 24[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  << \"Date: Mon, 07 Mar 2016 21:57:25 GMT[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  << \"Connection: keep-alive[\\r][\\n]\"");
            }
        };

        Map<?,?> debugMessageMap = new TestFactoryImpl().generateDebugMessageMap(httpDebugLogList);
        assertEquals(2, debugMessageMap.size());

        List<String> poolMessages =
                (List<String>)debugMessageMap.get("poolMessages");

        assertEquals(0, poolMessages.size());

        List<Map<String, List<String>>> externalRequests =
                (List<Map<String, List<String>>>)debugMessageMap.get("externalRequests");

        //verify only 1 entry
        assertEquals(1, externalRequests.size());

        Map<String, List<String>> singleRequest = externalRequests.get(0);

        assertNotNull(singleRequest.get("request"));
        assertNotNull(singleRequest.get("response"));

        List<String> request = singleRequest.get("request");
        List<String> response = singleRequest.get("response");

        assertEquals(6, request.size());
        assertEquals(6, response.size());

        for(String entry:request){
            assertTrue("Check if " + entry + " starts with >>", entry.startsWith(">>"));
        }

        for(String entry:response){
            assertTrue("Check if " + entry + " starts with <<", entry.startsWith("<<"));
        }
    }

    @Test
    public void testGenerateDebugMessageMapRequestWithoutResponse() {
        List<String> httpDebugLogList = new ArrayList<String>(){
            {
                add("2016-03-07 21:57:25,645 263018283 [qtp285433336-4446] DEBUG org.apache.http.impl.conn.PoolingClientConnectionManager - Connection request: [route: {}->http://repose-origin-732438-7-3-2-0:8000][total kept alive: 0; route allocated: 0 of 200; total allocated: 0 of 400]");
                add("2016-03-07 21:57:25,655 263018293 [qtp285433336-4446] DEBUG org.apache.http.impl.conn.PoolingClientConnectionManager - Connection leased: [id: 0][route: {}->http://repose-origin-732438-7-3-2-0:8000][total kept alive: 0; route allocated: 1 of 200; total allocated: 1 of 400]");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.impl.conn.PoolingClientConnectionManager - Connection [id: 0][route: {}->http://repose-origin-732438-7-3-2-0:8000] can be kept alive indefinitely");

                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  >> \"GET / HTTP/1.1[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  >> \"via: 1.1 localhost:8080 (Repose/7.3.2.0)[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  >> \"x-forwarded-for: 172.17.0.1[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  >> \"Host: repose-origin-732438-7-3-2-0:8000[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  >> \"x-trans-id: eyJyZXF1ZXN0SWQiOiIyOTNmMzYwMS1jOWUxLTQ5ZjctOWJhMC01MGE5OTVjZGJkNzkiLCJvcmlnaW4iOm51bGx9[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  >> \"x-pp-user: dima;q=0.3[\\r][\\n]\"");

                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.impl.conn.PoolingClientConnectionManager - Connection released: [id: 0][route: {}->http://repose-origin-732438-7-3-2-0:8000][total kept alive: 1; route allocated: 1 of 200; total allocated: 1 of 400]");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.impl.conn.PoolingClientConnectionManager - Connection released: [id: 0][route: {}->http://repose-origin-732438-7-3-2-0:8000][total kept alive: 1; route allocated: 1 of 200; total allocated: 1 of 400]");
            }
        };

        Map<?,?> debugMessageMap = new TestFactoryImpl().generateDebugMessageMap(httpDebugLogList);
        assertEquals(2, debugMessageMap.size());

        List<String> poolMessages =
                (List<String>)debugMessageMap.get("poolMessages");

        assertEquals(5, poolMessages.size());
        assertTrue(poolMessages.contains("2016-03-07 21:57:25,645 263018283 [qtp285433336-4446] DEBUG org.apache.http.impl.conn." +
                "PoolingClientConnectionManager - Connection request: [route: {}->" +
                "http://repose-origin-732438-7-3-2-0:8000][total kept alive: 0; route allocated: 0 of 200; " +
                "total allocated: 0 of 400]"));

        List<Map<String, List<String>>> externalRequests =
                (List<Map<String, List<String>>>)debugMessageMap.get("externalRequests");

        //verify only 1 entry
        assertEquals("Assert there is only one external requests entry", 1, externalRequests.size());

        Map<String, List<String>> singleRequest = externalRequests.get(0);

        assertNotNull("Assert there is a request entry", singleRequest.get("request"));
        assertNull("Assert there is not a response entry", singleRequest.get("response"));

        List<String> request = singleRequest.get("request");

        assertEquals(6, request.size());

        for(String entry:request){
            assertTrue("Check if " + entry + " starts with >>", entry.startsWith(">>"));
        }
    }

    @Test
    public void testGenerateDebugMessageMapResponseWithoutRequest() {
        List<String> httpDebugLogList = new ArrayList<String>(){
            {
                add("2016-03-07 21:57:25,645 263018283 [qtp285433336-4446] DEBUG org.apache.http.impl.conn.PoolingClientConnectionManager - Connection request: [route: {}->http://repose-origin-732438-7-3-2-0:8000][total kept alive: 0; route allocated: 0 of 200; total allocated: 0 of 400]");
                add("2016-03-07 21:57:25,655 263018293 [qtp285433336-4446] DEBUG org.apache.http.impl.conn.PoolingClientConnectionManager - Connection leased: [id: 0][route: {}->http://repose-origin-732438-7-3-2-0:8000][total kept alive: 0; route allocated: 1 of 200; total allocated: 1 of 400]");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.impl.conn.PoolingClientConnectionManager - Connection [id: 0][route: {}->http://repose-origin-732438-7-3-2-0:8000] can be kept alive indefinitely");

                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  << \"HTTP/1.1 200 OK[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  << \"X-Powered-By: Express[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  << \"content-type: application/json[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  << \"Content-Length: 24[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  << \"Date: Mon, 07 Mar 2016 21:57:25 GMT[\\r][\\n]\"");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.wire -  << \"Connection: keep-alive[\\r][\\n]\"");

                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.impl.conn.PoolingClientConnectionManager - Connection released: [id: 0][route: {}->http://repose-origin-732438-7-3-2-0:8000][total kept alive: 1; route allocated: 1 of 200; total allocated: 1 of 400]");
                add("2016-03-07 21:57:25,723 263018361 [qtp285433336-4446] DEBUG org.apache.http.impl.conn.PoolingClientConnectionManager - Connection released: [id: 0][route: {}->http://repose-origin-732438-7-3-2-0:8000][total kept alive: 1; route allocated: 1 of 200; total allocated: 1 of 400]");
            }
        };

        Map<?,?> debugMessageMap = new TestFactoryImpl().generateDebugMessageMap(httpDebugLogList);
        assertEquals(2, debugMessageMap.size());

        List<String> poolMessages =
                (List<String>)debugMessageMap.get("poolMessages");

        assertEquals(5, poolMessages.size());
        assertTrue(poolMessages.contains("2016-03-07 21:57:25,645 263018283 [qtp285433336-4446] DEBUG org.apache.http.impl.conn." +
                "PoolingClientConnectionManager - Connection request: [route: {}->" +
                "http://repose-origin-732438-7-3-2-0:8000][total kept alive: 0; route allocated: 0 of 200; " +
                "total allocated: 0 of 400]"));

        List<Map<String, List<String>>> externalRequests =
                (List<Map<String, List<String>>>)debugMessageMap.get("externalRequests");

        //verify there are no entries
        assertEquals(0, externalRequests.size());

    }
}