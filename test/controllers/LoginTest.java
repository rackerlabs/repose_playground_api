package controllers;

import com.google.common.collect.ImmutableMap;
import models.User;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;
import play.test.FakeApplication;
import play.test.Helpers;
import play.test.WithApplication;
import services.IUserService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;


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

        IUserService serviceMock = mock(IUserService.class);
        when(serviceMock.isValid(anyString())).thenReturn(true);
        when(serviceMock.findByToken(anyString())).thenReturn(user);
        Map<String, String> flashData = Collections.emptyMap();
        Map<String, Object> argData = Collections.emptyMap();
        play.api.mvc.RequestHeader header = mock(play.api.mvc.RequestHeader.class);
        Http.Request request = mock(Http.Request.class);
        when(request.getHeader("Token")).thenReturn("fake-token");
        Http.Context context = new Http.Context(2L, header, request, flashData, flashData, argData);
        Http.Context.current.set(context);

        Result result = new Login(serviceMock).index();
        assertEquals(200, result.status());

        verify(serviceMock).isValid(anyString());
        verify(serviceMock, times(2)).findByToken(anyString());
        verify(request, times(2)).getHeader(anyString());

        Helpers.stop(app);
    }

    @Test
    public void testIndexUnauthorized() {
        //start application
        FakeApplication app = Helpers.fakeApplication();
        Helpers.start(app);

        IUserService serviceMock = mock(IUserService.class);
        when(serviceMock.isValid(anyString())).thenReturn(false);
        Map<String, String> flashData = Collections.emptyMap();
        Map<String, Object> argData = Collections.emptyMap();
        play.api.mvc.RequestHeader header = mock(play.api.mvc.RequestHeader.class);
        Http.Request request = mock(Http.Request.class);
        when(request.getHeader("Token")).thenReturn("fake-token");
        Http.Context context = new Http.Context(2L, header, request, flashData, flashData, argData);
        Http.Context.current.set(context);

        Result result = new Login(serviceMock).index();
        assertEquals(401, result.status());

        verify(serviceMock).isValid(anyString());
        verify(serviceMock, times(1)).findByToken(anyString());
        verify(request, times(2)).getHeader(anyString());

        Helpers.stop(app);
    }

    @Test
    public void testIndexNoToken() {
        //start application
        FakeApplication app = Helpers.fakeApplication();
        Helpers.start(app);

        IUserService serviceMock = mock(IUserService.class);
        when(serviceMock.isValid(anyString())).thenReturn(false);
        Map<String, String> flashData = Collections.emptyMap();
        Map<String, Object> argData = Collections.emptyMap();
        play.api.mvc.RequestHeader header = mock(play.api.mvc.RequestHeader.class);
        Http.Request request = mock(Http.Request.class);
        when(request.getHeader("Token")).thenReturn(null);
        Http.Context context = new Http.Context(2L, header, request, flashData, flashData, argData);
        Http.Context.current.set(context);

        Result result = new Login(serviceMock).index();
        assertEquals(401, result.status());

        verify(serviceMock).isValid(anyString());
        verify(serviceMock, times(1)).findByToken(anyString());
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

        IUserService serviceMock = mock(IUserService.class);
        when(serviceMock.isValid(anyString())).thenReturn(true);
        when(serviceMock.findByToken(anyString())).thenReturn(user);
        Map<String, String> flashData = Collections.emptyMap();
        Map<String, Object> argData = Collections.emptyMap();
        play.api.mvc.RequestHeader header = mock(play.api.mvc.RequestHeader.class);
        Http.Request request = mock(Http.Request.class);
        when(request.getHeader("Token")).thenReturn("fake-token");
        Http.Context context = new Http.Context(2L, header, request, flashData, flashData, argData);
        Http.Context.current.set(context);

        Result result = new Login(serviceMock).index();
        assertEquals(401, result.status());

        verify(serviceMock).isValid(anyString());
        verify(serviceMock, times(2)).findByToken(anyString());
        verify(request, times(2)).getHeader(anyString());

        Helpers.stop(app);
    }


}
