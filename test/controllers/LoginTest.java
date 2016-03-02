package controllers;

import exceptions.InternalServerException;
import exceptions.UnauthorizedException;
import models.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.Logger;
import play.api.mvc.RequestHeader;
import play.mvc.Http;
import play.mvc.Result;
import play.test.FakeApplication;
import play.test.Helpers;
import services.IAuthService;
import services.IUserService;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static play.test.Helpers.*;

public class LoginTest {


    private FakeApplication app;

    @Before
    public void setUp(){
        //start application
        app = fakeApplication(inMemoryDatabase("test"));
        Helpers.start(app);

    }

    @After
    public void tearDown() {
        Helpers.stop(app);

    }

    @Test
    public void testIndexSuccess() {
        //set up mock user
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");

        IUserService userServiceMock = mock(IUserService.class);
        IAuthService authServiceMock = mock(IAuthService.class);
        when(userServiceMock.isValid(anyString())).thenReturn(true);
        when(userServiceMock.findByToken(anyString())).thenReturn(user);
        Map<String, String> flashData = Collections.emptyMap();
        Map<String, Object> argData = Collections.emptyMap();
        play.api.mvc.RequestHeader header = mock(play.api.mvc.RequestHeader.class);
        Http.Request request = mock(Http.Request.class);
        when(request.getHeader("Token")).thenReturn("fake-token");
        Http.Context context = new Http.Context(2L, header, request, flashData, flashData, argData);
        Http.Context.current.set(context);

        Result result = new Login(userServiceMock, authServiceMock).index();
        assertEquals(200, result.status());

        verify(userServiceMock).isValid(anyString());
        verify(userServiceMock, times(2)).findByToken(anyString());
        verify(request, times(2)).getHeader(anyString());
    }

    @Test
    public void testIndexUnauthorized() {
        IUserService userServiceMock = mock(IUserService.class);
        IAuthService authServiceMock = mock(IAuthService.class);
        when(userServiceMock.isValid(anyString())).thenReturn(false);
        Map<String, String> flashData = Collections.emptyMap();
        Map<String, Object> argData = Collections.emptyMap();
        play.api.mvc.RequestHeader header = mock(play.api.mvc.RequestHeader.class);
        Http.Request request = mock(Http.Request.class);
        when(request.getHeader("Token")).thenReturn("fake-token");
        Http.Context context = new Http.Context(2L, header, request, flashData, flashData, argData);
        Http.Context.current.set(context);

        Result result = new Login(userServiceMock, authServiceMock).index();
        assertEquals(401, result.status());

        verify(userServiceMock).isValid(anyString());
        verify(userServiceMock, times(1)).findByToken(anyString());
        verify(request, times(2)).getHeader(anyString());
    }

    @Test
    public void testIndexNoToken() {

        IUserService userServiceMock = mock(IUserService.class);
        IAuthService authServiceMock = mock(IAuthService.class);
        when(userServiceMock.isValid(anyString())).thenReturn(false);
        Map<String, String> flashData = Collections.emptyMap();
        Map<String, Object> argData = Collections.emptyMap();
        play.api.mvc.RequestHeader header = mock(play.api.mvc.RequestHeader.class);
        Http.Request request = mock(Http.Request.class);
        when(request.getHeader("Token")).thenReturn(null);
        Http.Context context = new Http.Context(2L, header, request, flashData, flashData, argData);
        Http.Context.current.set(context);

        Result result = new Login(userServiceMock, authServiceMock).index();
        assertEquals(401, result.status());

        verify(userServiceMock).isValid(anyString());
        verify(userServiceMock, times(1)).findByToken(anyString());
        verify(request, times(2)).getHeader(anyString());
    }

    @Test
    public void testIndexNullUser() {
        //set up mock user
        User user = null;

        IUserService userServiceMock = mock(IUserService.class);
        IAuthService authServiceMock = mock(IAuthService.class);
        when(userServiceMock.isValid(anyString())).thenReturn(true);
        when(userServiceMock.findByToken(anyString())).thenReturn(user);
        Map<String, String> flashData = Collections.emptyMap();
        Map<String, Object> argData = Collections.emptyMap();
        play.api.mvc.RequestHeader header = mock(play.api.mvc.RequestHeader.class);
        Http.Request request = mock(Http.Request.class);
        when(request.getHeader("Token")).thenReturn("fake-token");
        Http.Context context = new Http.Context(2L, header, request, flashData, flashData, argData);
        Http.Context.current.set(context);

        Result result = new Login(userServiceMock, authServiceMock).index();
        assertEquals(401, result.status());

        verify(userServiceMock).isValid(anyString());
        verify(userServiceMock, times(2)).findByToken(anyString());
        verify(request, times(2)).getHeader(anyString());
    }

    @Test
    public void testCreateSucess() {
        //set up mock user
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");

        IUserService userServiceMock = mock(IUserService.class);
        IAuthService authServiceMock = mock(IAuthService.class);
        try {
            when(authServiceMock.getUser(anyString(), anyString())).thenReturn(user);
        }catch(UnauthorizedException | InternalServerException e){
            fail(e.getLocalizedMessage());
        }

        Map<String, String> flashData = Collections.emptyMap();
        Map<String, Object> argData = Collections.emptyMap();
        play.api.mvc.RequestHeader header = mock(play.api.mvc.RequestHeader.class);
        Http.RequestBody requestBody = mock(Http.RequestBody.class);
        when(requestBody.asFormUrlEncoded()).thenReturn(new HashMap<String, String[]>(){{
            put("username", new String[] {"goodusername" });
            put("password", new String[] {"goodpassword" });
        } });
        Http.Request request = mock(Http.Request.class);
        when(request.body()).thenReturn(requestBody);
        Http.Context context = new Http.Context(2L, header, request, flashData, flashData, argData);
        Http.Context.current.set(context);

        Result result = new Login(userServiceMock, authServiceMock).create();
        assertEquals(200, result.status());
        assertEquals("application/json", result.contentType());
        assertTrue(contentAsString(result).
                equals("{\"id\":null," +
                        "\"username\":\"fake-user\"," +
                        "\"token\":\"fake-token\"," +
                        "\"tenant\":\"111\"," +
                        "\"userid\":\"1\"," +
                        "\"expireDate\":null}"));

        try{
            verify(authServiceMock).getUser(anyString(), anyString());
        }catch(UnauthorizedException | InternalServerException e){
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testCreateNoRequestBody() {
        IUserService userServiceMock = mock(IUserService.class);
        IAuthService authServiceMock = mock(IAuthService.class);

        Map<String, String> flashData = Collections.emptyMap();
        Map<String, Object> argData = Collections.emptyMap();
        play.api.mvc.RequestHeader header = mock(play.api.mvc.RequestHeader.class);
        Http.Request request = mock(Http.Request.class);
        Http.RequestBody requestBody = mock(Http.RequestBody.class);
        when(requestBody.asFormUrlEncoded()).thenReturn(new HashMap<String, String[]>());
        when(request.body()).thenReturn(requestBody);
        Http.Context context = new Http.Context(2L, header, request, flashData, flashData, argData);
        Http.Context.current.set(context);

        Result result = new Login(userServiceMock, authServiceMock).create();
        assertEquals(400, result.status());
        assertEquals("application/json", result.contentType());
        assertTrue(contentAsString(result).
                equals("{\"password\":[\"required\"]," +
                        "\"username\":[\"required\"]}"));
    }

    @Test
    public void testCreateNoUsername() {
        IUserService userServiceMock = mock(IUserService.class);
        IAuthService authServiceMock = mock(IAuthService.class);

        Map<String, String> flashData = Collections.emptyMap();
        Map<String, Object> argData = Collections.emptyMap();
        play.api.mvc.RequestHeader header = mock(play.api.mvc.RequestHeader.class);
        Http.Request request = mock(Http.Request.class);
        Http.RequestBody requestBody = mock(Http.RequestBody.class);
        when(requestBody.asFormUrlEncoded()).thenReturn(new HashMap<String, String[]>() {
            {
                put("password", new String[]{"good-password"});
            }
        });
        when(request.body()).thenReturn(requestBody);
        Http.Context context = new Http.Context(2L, header, request, flashData, flashData, argData);
        Http.Context.current.set(context);

        Result result = new Login(userServiceMock, authServiceMock).create();
        assertEquals(400, result.status());
        assertEquals("application/json", result.contentType());
        assertTrue(contentAsString(result).
                equals("{\"username\":[\"required\"]}"));
    }

    @Test
    public void testCreateUsernameInvalid() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {
            IUserService userServiceMock = mock(IUserService.class);
            IAuthService authServiceMock = mock(IAuthService.class);

            Map<String, String> flashData = Collections.emptyMap();
            Map<String, Object> argData = Collections.emptyMap();
            RequestHeader header = mock(RequestHeader.class);
            Http.Request request = mock(Http.Request.class);
            Http.RequestBody requestBody = mock(Http.RequestBody.class);
            when(requestBody.asFormUrlEncoded()).thenReturn(new HashMap<String, String[]>() {
                {
                    put("username", new String[]{"er"});
                    put("password", new String[]{"good-password"});
                }
            });
            when(request.body()).thenReturn(requestBody);
            Http.Context context = new Http.Context(2L, header, request, flashData, flashData, argData);
            Http.Context.current.set(context);

            Result result = new Login(userServiceMock, authServiceMock).create();
            assertEquals(400, result.status());
            assertEquals("application/json", result.contentType());
            assertTrue(contentAsString(result).
                    equals("{\"username\":[\"minLength->3\"]}"));
        });
    }

    @Test
    public void testCreateNoPassword() {

        IUserService userServiceMock = mock(IUserService.class);
        IAuthService authServiceMock = mock(IAuthService.class);

        Map<String, String> flashData = Collections.emptyMap();
        Map<String, Object> argData = Collections.emptyMap();
        play.api.mvc.RequestHeader header = mock(play.api.mvc.RequestHeader.class);
        Http.Request request = mock(Http.Request.class);
        Http.RequestBody requestBody = mock(Http.RequestBody.class);
        when(requestBody.asFormUrlEncoded()).thenReturn(new HashMap<String, String[]>() {
            {
                put("username", new String[]{"good-name"});
            }
        });
        when(request.body()).thenReturn(requestBody);
        Http.Context context = new Http.Context(2L, header, request, flashData, flashData, argData);
        Http.Context.current.set(context);

        Result result = new Login(userServiceMock, authServiceMock).create();
        assertEquals(400, result.status());
        assertEquals("application/json", result.contentType());
        assertTrue(contentAsString(result).
                equals("{\"password\":[\"required\"]}"));
    }

    @Test
    public void testCreatePasswordInvalid() {
        IUserService userServiceMock = mock(IUserService.class);
        IAuthService authServiceMock = mock(IAuthService.class);

        Map<String, String> flashData = Collections.emptyMap();
        Map<String, Object> argData = Collections.emptyMap();
        play.api.mvc.RequestHeader header = mock(play.api.mvc.RequestHeader.class);
        Http.Request request = mock(Http.Request.class);
        Http.RequestBody requestBody = mock(Http.RequestBody.class);
        when(requestBody.asFormUrlEncoded()).thenReturn(new HashMap<String, String[]>() {
            {
                put("username", new String[]{"name"});
                put("password", new String[]{"oop"});
            }
        });
        when(request.body()).thenReturn(requestBody);
        Http.Context context = new Http.Context(2L, header, request, flashData, flashData, argData);
        Http.Context.current.set(context);

        Result result = new Login(userServiceMock, authServiceMock).create();
        assertEquals(400, result.status());
        assertEquals("application/json", result.contentType());
        assertTrue(contentAsString(result).
                equals("{\"password\":[\"minLength->6\"]}"));

    }

    @Test
    public void testCreateUnauthorized() {
        IUserService userServiceMock = mock(IUserService.class);
        IAuthService authServiceMock = mock(IAuthService.class);
        try {
            when(authServiceMock.getUser(anyString(), anyString())).thenThrow(new UnauthorizedException("unauthorized"));
        }catch(UnauthorizedException | InternalServerException e){
            fail(e.getLocalizedMessage());
        }

        Map<String, String> flashData = Collections.emptyMap();
        Map<String, Object> argData = Collections.emptyMap();
        play.api.mvc.RequestHeader header = mock(play.api.mvc.RequestHeader.class);
        Http.RequestBody requestBody = mock(Http.RequestBody.class);
        when(requestBody.asFormUrlEncoded()).thenReturn(new HashMap<String, String[]>(){{
            put("username", new String[] {"goodusername" });
            put("password", new String[] {"goodpassword" });
        } });
        Http.Request request = mock(Http.Request.class);
        when(request.body()).thenReturn(requestBody);
        Http.Context context = new Http.Context(2L, header, request, flashData, flashData, argData);
        Http.Context.current.set(context);

        Result result = new Login(userServiceMock, authServiceMock).create();
        assertEquals(401, result.status());
        assertEquals("application/json", result.contentType());
        Logger.debug(contentAsString(result));
        assertTrue(contentAsString(result).
                equals("{\"message\":\"unauthorized\"}"));

        try{
            verify(authServiceMock).getUser(anyString(), anyString());
        }catch(UnauthorizedException | InternalServerException e){
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testCreateInternalServerError() {
        IUserService userServiceMock = mock(IUserService.class);
        IAuthService authServiceMock = mock(IAuthService.class);
        try {
            when(authServiceMock.getUser(anyString(), anyString())).
                    thenThrow(new InternalServerException("all the things broke"));
        }catch(UnauthorizedException | InternalServerException e){
            fail(e.getLocalizedMessage());
        }

        Map<String, String> flashData = Collections.emptyMap();
        Map<String, Object> argData = Collections.emptyMap();
        play.api.mvc.RequestHeader header = mock(play.api.mvc.RequestHeader.class);
        Http.RequestBody requestBody = mock(Http.RequestBody.class);
        when(requestBody.asFormUrlEncoded()).thenReturn(new HashMap<String, String[]>(){{
            put("username", new String[] {"goodusername" });
            put("password", new String[] {"goodpassword" });
        } });
        Http.Request request = mock(Http.Request.class);
        when(request.body()).thenReturn(requestBody);
        Http.Context context = new Http.Context(2L, header, request, flashData, flashData, argData);
        Http.Context.current.set(context);

        Result result = new Login(userServiceMock, authServiceMock).create();
        assertEquals(500, result.status());
        assertEquals("application/json", result.contentType());
        Logger.debug(contentAsString(result));
        assertTrue(contentAsString(result).
                equals("{\"message\":\"all the things broke\"}"));

        try{
            verify(authServiceMock).getUser(anyString(), anyString());
        }catch(UnauthorizedException | InternalServerException e){
            fail(e.getLocalizedMessage());
        }
    }

}
