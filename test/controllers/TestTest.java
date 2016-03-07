package controllers;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import exceptions.InternalServerException;
import models.User;
import play.api.mvc.RequestHeader;
import play.mvc.Http;
import play.mvc.Result;
import services.IUserService;
import services.TestService;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static play.test.Helpers.*;

/**
 * Created by dimi5963 on 3/6/16.
 */
public class TestTest {

    //test test repose instance

    @org.junit.Test
    public void testTestSuccess() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {

            //set up mock user
            User user = new User();
            user.setTenant("111");
            user.setPassword("pass");
            user.setToken("fake-token");
            user.setUserid("1");
            user.setUsername("fake-user");

            ObjectNode requestJson = JsonNodeFactory.instance.objectNode();
            requestJson.put("request", "yes");
            requestJson.put("message", "test");

            ObjectNode responseJson = JsonNodeFactory.instance.objectNode();
            responseJson.put("response", "yes");
            responseJson.put("message", "test");

            IUserService userServiceMock = mock(IUserService.class);
            TestService testServiceMock = mock(TestService.class);

            when(userServiceMock.isValid(anyString())).thenReturn(true);
            when(userServiceMock.findByToken(anyString())).thenReturn(user);
            try {
                when(testServiceMock.testReposeInstance(any(), anyString(), any())).thenReturn(responseJson);
            }catch(InternalServerException ise){
                fail(ise.getLocalizedMessage());
            }

            Map<String, String> flashData = Collections.emptyMap();
            Map<String, Object> argData = Collections.emptyMap();
            RequestHeader header = mock(RequestHeader.class);
            Http.RequestBody requestBody = mock(Http.RequestBody.class);
            when(requestBody.asJson()).thenReturn(requestJson);
            Http.Request request = mock(Http.Request.class);
            when(request.body()).thenReturn(requestBody);
            when(request.getHeader("Token")).thenReturn("fake-token");
            Http.Context context = new Http.Context(2L, header, request, flashData, flashData, argData);
            Http.Context.current.set(context);

            Result result = new Test(userServiceMock, testServiceMock).test("1");
            assertEquals(200, result.status());
            assertEquals("{\"response\":\"yes\",\"message\":\"test\"}", contentAsString(result));

            verify(userServiceMock).isValid(anyString());
            verify(userServiceMock, times(1)).findByToken(anyString());
            verify(request, times(1)).getHeader(anyString());
            try {
                verify(testServiceMock).testReposeInstance(any(), anyString(), any());
            }catch(InternalServerException ise){
                fail(ise.getLocalizedMessage());
            }
        });
    }

    @org.junit.Test
    public void testTestNoBody() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {

            //set up mock user
            User user = new User();
            user.setTenant("111");
            user.setPassword("pass");
            user.setToken("fake-token");
            user.setUserid("1");
            user.setUsername("fake-user");

            ObjectNode requestJson = JsonNodeFactory.instance.objectNode();
            requestJson.put("request", "yes");
            requestJson.put("message", "test");

            ObjectNode responseJson = JsonNodeFactory.instance.objectNode();
            responseJson.put("response", "yes");
            responseJson.put("message", "test");

            IUserService userServiceMock = mock(IUserService.class);
            TestService testServiceMock = mock(TestService.class);

            when(userServiceMock.isValid(anyString())).thenReturn(true);
            when(userServiceMock.findByToken(anyString())).thenReturn(user);
            try {
                when(testServiceMock.testReposeInstance(any(), anyString(), any())).thenReturn(responseJson);
            }catch(InternalServerException ise){
                fail(ise.getLocalizedMessage());
            }

            Map<String, String> flashData = Collections.emptyMap();
            Map<String, Object> argData = Collections.emptyMap();
            RequestHeader header = mock(RequestHeader.class);
            Http.Request request = mock(Http.Request.class);
            when(request.getHeader("Token")).thenReturn("fake-token");
            Http.Context context = new Http.Context(2L, header, request, flashData, flashData, argData);
            Http.Context.current.set(context);

            Result result = new Test(userServiceMock, testServiceMock).test("1");
            assertEquals(400, result.status());
            assertEquals("Not a proper request.", contentAsString(result));

            verify(userServiceMock, never()).isValid(anyString());
            verify(userServiceMock, never()).findByToken(anyString());
            verify(request, never()).getHeader(anyString());
            try {
                verify(testServiceMock, never()).testReposeInstance(any(), anyString(), any());
            }catch(InternalServerException ise){
                fail(ise.getLocalizedMessage());
            }
        });
    }

    @org.junit.Test
    public void testTestUnauthorized() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {
            IUserService userServiceMock = mock(IUserService.class);
            TestService testServiceMock = mock(TestService.class);

            ObjectNode requestJson = JsonNodeFactory.instance.objectNode();
            requestJson.put("request", "yes");
            requestJson.put("message", "test");

            when(userServiceMock.isValid(anyString())).thenReturn(false);

            Map<String, String> flashData = Collections.emptyMap();
            Map<String, Object> argData = Collections.emptyMap();
            RequestHeader header = mock(RequestHeader.class);
            Http.RequestBody requestBody = mock(Http.RequestBody.class);
            when(requestBody.asJson()).thenReturn(requestJson);
            Http.Request request = mock(Http.Request.class);
            when(request.body()).thenReturn(requestBody);
            when(request.getHeader("Token")).thenReturn("fake-token");
            Http.Context context = new Http.Context(2L, header, request, flashData, flashData, argData);
            Http.Context.current.set(context);

            Result result = new Test(userServiceMock, testServiceMock).test("1");
            assertEquals(401, result.status());

            verify(userServiceMock).isValid(anyString());
            verify(userServiceMock, never()).findByToken(anyString());
            verify(request, times(1)).getHeader(anyString());

            try {
                verify(testServiceMock, never()).testReposeInstance(any(), anyString(), any());
            }catch(InternalServerException ise){
                fail(ise.getLocalizedMessage());
            }
        });
    }

    @org.junit.Test
    public void testTestNoToken() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {
            IUserService userServiceMock = mock(IUserService.class);
            TestService testServiceMock = mock(TestService.class);

            ObjectNode requestJson = JsonNodeFactory.instance.objectNode();
            requestJson.put("request", "yes");
            requestJson.put("message", "test");

            when(userServiceMock.isValid(anyString())).thenReturn(false);

            Map<String, String> flashData = Collections.emptyMap();
            Map<String, Object> argData = Collections.emptyMap();
            RequestHeader header = mock(RequestHeader.class);
            Http.RequestBody requestBody = mock(Http.RequestBody.class);
            when(requestBody.asJson()).thenReturn(requestJson);
            Http.Request request = mock(Http.Request.class);
            when(request.body()).thenReturn(requestBody);
            when(request.getHeader("Token")).thenReturn(null);
            Http.Context context = new Http.Context(2L, header, request, flashData, flashData, argData);
            Http.Context.current.set(context);

            Result result = new Test(userServiceMock, testServiceMock).test("1");
            assertEquals(401, result.status());

            verify(userServiceMock).isValid(anyString());
            verify(userServiceMock, never()).findByToken(anyString());
            verify(request, times(1)).getHeader(anyString());

            try {
                verify(testServiceMock, never()).testReposeInstance(any(), anyString(), any());
            }catch(InternalServerException ise){
                fail(ise.getLocalizedMessage());
            }
        });

    }

    @org.junit.Test
    public void testTestNullUser() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {

            IUserService userServiceMock = mock(IUserService.class);
            TestService testServiceMock = mock(TestService.class);

            ObjectNode requestJson = JsonNodeFactory.instance.objectNode();
            requestJson.put("request", "yes");
            requestJson.put("message", "test");

            when(userServiceMock.isValid(anyString())).thenReturn(true);
            when(userServiceMock.findByToken(anyString())).thenReturn(null);

            Map<String, String> flashData = Collections.emptyMap();
            Map<String, Object> argData = Collections.emptyMap();
            RequestHeader header = mock(RequestHeader.class);
            Http.RequestBody requestBody = mock(Http.RequestBody.class);
            when(requestBody.asJson()).thenReturn(requestJson);
            Http.Request request = mock(Http.Request.class);
            when(request.body()).thenReturn(requestBody);
            when(request.getHeader("Token")).thenReturn("fake-token");
            Http.Context context = new Http.Context(2L, header, request, flashData, flashData, argData);
            Http.Context.current.set(context);

            Result result = new Test(userServiceMock, testServiceMock).test("1");
            assertEquals(401, result.status());

            verify(userServiceMock).isValid(anyString());
            verify(userServiceMock, times(1)).findByToken(anyString());
            verify(request, times(1)).getHeader(anyString());

            try {
                verify(testServiceMock, never()).testReposeInstance(any(), anyString(), any());
            }catch(InternalServerException ise){
                fail(ise.getLocalizedMessage());
            }
        });
    }

    @org.junit.Test
    public void testTestNull() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {

            //set up mock user
            User user = new User();
            user.setTenant("111");
            user.setPassword("pass");
            user.setToken("fake-token");
            user.setUserid("1");
            user.setUsername("fake-user");

            ObjectNode requestJson = JsonNodeFactory.instance.objectNode();
            requestJson.put("request", "yes");
            requestJson.put("message", "test");

            IUserService userServiceMock = mock(IUserService.class);
            TestService testServiceMock = mock(TestService.class);

            when(userServiceMock.isValid(anyString())).thenReturn(true);
            when(userServiceMock.findByToken(anyString())).thenReturn(user);
            try {
                when(testServiceMock.testReposeInstance(any(), anyString(), any())).thenReturn(null);
            }catch(InternalServerException ise){
                fail(ise.getLocalizedMessage());
            }

            Map<String, String> flashData = Collections.emptyMap();
            Map<String, Object> argData = Collections.emptyMap();
            RequestHeader header = mock(RequestHeader.class);
            Http.RequestBody requestBody = mock(Http.RequestBody.class);
            when(requestBody.asJson()).thenReturn(requestJson);
            Http.Request request = mock(Http.Request.class);
            when(request.body()).thenReturn(requestBody);
            when(request.getHeader("Token")).thenReturn("fake-token");
            Http.Context context = new Http.Context(2L, header, request, flashData, flashData, argData);
            Http.Context.current.set(context);

            Result result = new Test(userServiceMock, testServiceMock).test("1");
            assertEquals(200, result.status());
            assertEquals("", contentAsString(result));

            verify(userServiceMock).isValid(anyString());
            verify(userServiceMock, times(1)).findByToken(anyString());
            verify(request, times(1)).getHeader(anyString());
            try {
                verify(testServiceMock).testReposeInstance(any(), any(), any());
            }catch(InternalServerException ise){
                fail(ise.getLocalizedMessage());
            }
        });
    }

    @org.junit.Test
    public void testTestInternalServerException() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {
            //set up mock user
            User user = new User();
            user.setTenant("111");
            user.setPassword("pass");
            user.setToken("fake-token");
            user.setUserid("1");
            user.setUsername("fake-user");

            ObjectNode requestJson = JsonNodeFactory.instance.objectNode();
            requestJson.put("request", "yes");
            requestJson.put("message", "test");

            IUserService userServiceMock = mock(IUserService.class);
            TestService testServiceMock = mock(TestService.class);

            when(userServiceMock.isValid(anyString())).thenReturn(true);
            when(userServiceMock.findByToken(anyString())).thenReturn(user);
            try {
                when(testServiceMock.testReposeInstance(any(), anyString(), any()))
                        .thenThrow(new InternalServerException("all the things!"));
            }catch(InternalServerException ise){
                fail(ise.getLocalizedMessage());
            }

            Map<String, String> flashData = Collections.emptyMap();
            Map<String, Object> argData = Collections.emptyMap();
            RequestHeader header = mock(RequestHeader.class);
            Http.RequestBody requestBody = mock(Http.RequestBody.class);
            when(requestBody.asJson()).thenReturn(requestJson);
            Http.Request request = mock(Http.Request.class);
            when(request.body()).thenReturn(requestBody);
            when(request.getHeader("Token")).thenReturn("fake-token");
            Http.Context context = new Http.Context(2L, header, request, flashData, flashData, argData);
            Http.Context.current.set(context);

            Result result = new Test(userServiceMock, testServiceMock).test("1");
            assertEquals(500, result.status());
            assertEquals(contentAsString(result), "{\"message\":\"all the things!\"}");

            verify(userServiceMock).isValid(anyString());
            verify(userServiceMock, times(1)).findByToken(anyString());
            verify(request, times(1)).getHeader(anyString());
            try {
                verify(testServiceMock).testReposeInstance(any(), anyString(), any());
            }catch(InternalServerException ise){
                fail(ise.getLocalizedMessage());
            }
        });
    }
}