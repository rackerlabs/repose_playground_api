package controllers;

import com.google.common.collect.ImmutableMap;
import exceptions.InternalServerException;
import exceptions.UnauthorizedException;
import models.User;
import org.junit.Test;
import play.Logger;
import play.api.mvc.RequestHeader;
import play.mvc.Http;
import play.mvc.Result;
import play.test.FakeApplication;
import play.test.Helpers;
import play.test.WithApplication;
import services.IAuthService;
import services.IUserService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static play.test.Helpers.contentAsString;


public class LoginTest extends WithApplication {

    @Override
    protected FakeApplication provideFakeApplication() {
        return new FakeApplication(new java.io.File("."), Helpers.class.getClassLoader(),
                ImmutableMap.of("play.http.router", "router.Routes"), new ArrayList<String>(), null);
    }

    @Test
    public void testIndexSuccess() {
        //start application
        FakeApplication app = Helpers.fakeApplication();
        Helpers.start(app);

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

        Helpers.stop(app);
    }

    @Test
    public void testIndexUnauthorized() {
        //start application
        FakeApplication app = Helpers.fakeApplication();
        Helpers.start(app);

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

        Helpers.stop(app);
    }

    @Test
    public void testIndexNoToken() {
        //start application
        FakeApplication app = Helpers.fakeApplication();
        Helpers.start(app);

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

        Helpers.stop(app);
    }

    @Test
    public void testIndexNullUser() {
        //start application
        FakeApplication app = Helpers.fakeApplication();
        Helpers.start(app);

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

        Helpers.stop(app);
    }

    @Test
    public void testCreateSucess() {
        //start application
        FakeApplication app = Helpers.fakeApplication();
        Helpers.start(app);

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

        Helpers.stop(app);
    }

    @Test
    public void testCreateNoRequestBody() {
        //start application
        FakeApplication app = Helpers.fakeApplication();
        Helpers.start(app);

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

        Helpers.stop(app);
    }

    @Test
    public void testCreateNoUsername() {
        //start application
        FakeApplication app = Helpers.fakeApplication();
        Helpers.start(app);

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

        Helpers.stop(app);
    }

    @Test
    public void testCreateUsernameInvalid() {
        //start application
        FakeApplication app = Helpers.fakeApplication();
        Helpers.start(app);

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


        Helpers.stop(app);
    }

    @Test
    public void testCreateNoPassword() {
        //start application
        FakeApplication app = Helpers.fakeApplication();
        Helpers.start(app);

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

        Helpers.stop(app);
    }

    @Test
    public void testCreatePasswordInvalid() {
        //start application
        FakeApplication app = Helpers.fakeApplication();
        Helpers.start(app);

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

        Helpers.stop(app);
    }

    @Test
    public void testCreateUnauthorized() {
        //start application
        FakeApplication app = Helpers.fakeApplication();
        Helpers.start(app);

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

        Helpers.stop(app);
    }

    @Test
    public void testCreateInternalServerError() {
        //start application
        FakeApplication app = Helpers.fakeApplication();
        Helpers.start(app);

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

        Helpers.stop(app);
    }

}
