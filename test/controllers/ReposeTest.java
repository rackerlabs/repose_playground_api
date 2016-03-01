package controllers;

import exceptions.InternalServerException;
import models.Container;
import models.User;
import org.junit.Test;
import play.api.mvc.RequestHeader;
import play.mvc.Http;
import play.mvc.Result;
import play.test.WithApplication;
import services.IReposeService;
import services.IUserService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static play.test.Helpers.*;


public class ReposeTest extends WithApplication {

    @Test
    public void testListSuccess() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {
            //set up mock container list
            List<Container> containerList = new ArrayList<Container>(){
                {
                    add(new Container("fake-name", true, "started 1 sec ago", "1.0", "1"));
                    add(new Container("fake-name2", false, "exited 1 sec ago", "1.1", "2"));
                }
            };

            //set up mock user
            User user = new User();
            user.setTenant("111");
            user.setPassword("pass");
            user.setToken("fake-token");
            user.setUserid("1");
            user.setUsername("fake-user");


            IUserService userServiceMock = mock(IUserService.class);
            IReposeService reposeServiceMock = mock(IReposeService.class);

            when(userServiceMock.isValid(anyString())).thenReturn(true);
            when(userServiceMock.findByToken(anyString())).thenReturn(user);
            try {
                when(reposeServiceMock.getReposeList(any())).thenReturn(containerList);
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

            Result result = new Repose(userServiceMock, reposeServiceMock).list();
            assertEquals(200, result.status());

            verify(userServiceMock).isValid(anyString());
            verify(userServiceMock, times(1)).findByToken(anyString());
            verify(request, times(1)).getHeader(anyString());
            try {
                verify(reposeServiceMock).getReposeList(any());
            }catch(InternalServerException ise){
                fail(ise.getLocalizedMessage());
            }
        });
    }

    @Test
    public void testListUnauthorized() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {
            IUserService userServiceMock = mock(IUserService.class);
            IReposeService reposeServiceMock = mock(IReposeService.class);

            when(userServiceMock.isValid(anyString())).thenReturn(false);

            Map<String, String> flashData = Collections.emptyMap();
            Map<String, Object> argData = Collections.emptyMap();
            RequestHeader header = mock(RequestHeader.class);
            Http.Request request = mock(Http.Request.class);
            when(request.getHeader("Token")).thenReturn("fake-token");
            Http.Context context = new Http.Context(2L, header, request, flashData, flashData, argData);
            Http.Context.current.set(context);

            Result result = new Repose(userServiceMock, reposeServiceMock).list();
            assertEquals(401, result.status());

            verify(userServiceMock).isValid(anyString());
            verify(userServiceMock, never()).findByToken(anyString());
            verify(request, times(1)).getHeader(anyString());

            try {
                verify(reposeServiceMock, never()).getReposeList(any());
            }catch(InternalServerException ise){
                fail(ise.getLocalizedMessage());
            }
        });
    }

    @Test
    public void testListNoToken() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {
            IUserService userServiceMock = mock(IUserService.class);
            IReposeService reposeServiceMock = mock(IReposeService.class);

            when(userServiceMock.isValid(anyString())).thenReturn(false);

            Map<String, String> flashData = Collections.emptyMap();
            Map<String, Object> argData = Collections.emptyMap();
            RequestHeader header = mock(RequestHeader.class);
            Http.Request request = mock(Http.Request.class);
            when(request.getHeader("Token")).thenReturn(null);
            Http.Context context = new Http.Context(2L, header, request, flashData, flashData, argData);
            Http.Context.current.set(context);

            Result result = new Repose(userServiceMock, reposeServiceMock).list();
            assertEquals(401, result.status());

            verify(userServiceMock).isValid(anyString());
            verify(userServiceMock, never()).findByToken(anyString());
            verify(request, times(1)).getHeader(anyString());

            try {
                verify(reposeServiceMock, never()).getReposeList(any());
            }catch(InternalServerException ise){
                fail(ise.getLocalizedMessage());
            }
        });

    }

    @Test
    public void testListNullUser() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {
            //set up mock user
            User user = null;

            IUserService userServiceMock = mock(IUserService.class);
            IReposeService reposeServiceMock = mock(IReposeService.class);

            when(userServiceMock.isValid(anyString())).thenReturn(true);
            when(userServiceMock.findByToken(anyString())).thenReturn(user);

            Map<String, String> flashData = Collections.emptyMap();
            Map<String, Object> argData = Collections.emptyMap();
            RequestHeader header = mock(RequestHeader.class);
            Http.Request request = mock(Http.Request.class);
            when(request.getHeader("Token")).thenReturn("fake-token");
            Http.Context context = new Http.Context(2L, header, request, flashData, flashData, argData);
            Http.Context.current.set(context);

            Result result = new Repose(userServiceMock, reposeServiceMock).list();
            assertEquals(401, result.status());

            verify(userServiceMock).isValid(anyString());
            verify(userServiceMock, times(1)).findByToken(anyString());
            verify(request, times(1)).getHeader(anyString());

            try {
                verify(reposeServiceMock, never()).getReposeList(any());
            }catch(InternalServerException ise){
                fail(ise.getLocalizedMessage());
            }
        });
    }

}
