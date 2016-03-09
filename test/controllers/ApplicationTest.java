package controllers;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import exceptions.InternalServerException;
import exceptions.NotFoundException;
import factories.ConfigurationFactory;
import models.*;
import models.Configuration;
import org.junit.Test;
import play.api.mvc.RequestHeader;
import play.mvc.Http;
import play.mvc.Result;
import services.FilterService;
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

/**
 * Created by dimi5963 on 3/8/16.
 */
public class ApplicationTest {

    //test versions

    @Test
    public void testVersionsSuccess() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {

            //set up mock user
            User user = new User();
            user.setTenant("111");
            user.setPassword("pass");
            user.setToken("fake-token");
            user.setUserid("1");
            user.setUsername("fake-user");

            List<String> versions = new ArrayList<String>(){
                {
                    add("1.0");
                    add("2.0");
                    add("3.0");
                }
            };

            IUserService userServiceMock = mock(IUserService.class);
            FilterService filterService = mock(FilterService.class);
            IReposeService reposeService = mock(IReposeService.class);
            ConfigurationFactory configurationFactory = mock(ConfigurationFactory.class);

            when(userServiceMock.isValid(anyString())).thenReturn(true);
            try {
                when(filterService.getVersions()).thenReturn(versions);
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

            Result result = new Application(configurationFactory, filterService,
                    userServiceMock, reposeService).versions();
            assertEquals(200, result.status());
            assertEquals("[\"1.0\",\"2.0\",\"3.0\"]", contentAsString(result));

            verify(userServiceMock).isValid(anyString());
            verify(request, times(1)).getHeader(anyString());
            try {
                verify(filterService).getVersions();
            }catch(InternalServerException ise){
                fail(ise.getLocalizedMessage());
            }
        });
    }

    @Test
    public void testVersionsUnauthorized() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {
            IUserService userServiceMock = mock(IUserService.class);
            FilterService filterService = mock(FilterService.class);
            IReposeService reposeService = mock(IReposeService.class);
            ConfigurationFactory configurationFactory = mock(ConfigurationFactory.class);

            when(userServiceMock.isValid(anyString())).thenReturn(false);

            Map<String, String> flashData = Collections.emptyMap();
            Map<String, Object> argData = Collections.emptyMap();
            RequestHeader header = mock(RequestHeader.class);
            Http.Request request = mock(Http.Request.class);
            when(request.getHeader("Token")).thenReturn("fake-token");
            Http.Context context = new Http.Context(2L, header, request, flashData, flashData, argData);
            Http.Context.current.set(context);

            Result result = new Application(configurationFactory, filterService,
                    userServiceMock, reposeService).versions();
            assertEquals(401, result.status());

            verify(userServiceMock).isValid(anyString());
            verify(userServiceMock, never()).findByToken(anyString());
            verify(request, times(1)).getHeader(anyString());

            try {
                verify(filterService, never()).getVersions();
            }catch(InternalServerException ise){
                fail(ise.getLocalizedMessage());
            }
        });
    }

    @Test
    public void testVersionsNull() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {

            //set up mock user
            User user = new User();
            user.setTenant("111");
            user.setPassword("pass");
            user.setToken("fake-token");
            user.setUserid("1");
            user.setUsername("fake-user");

            IUserService userServiceMock = mock(IUserService.class);
            FilterService filterService = mock(FilterService.class);
            IReposeService reposeService = mock(IReposeService.class);
            ConfigurationFactory configurationFactory = mock(ConfigurationFactory.class);

            when(userServiceMock.isValid(anyString())).thenReturn(true);
            try {
                when(filterService.getVersions()).thenReturn(null);
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

            Result result = new Application(configurationFactory, filterService,
                    userServiceMock, reposeService).versions();
            assertEquals(404, result.status());

            verify(userServiceMock).isValid(anyString());
            verify(request, times(1)).getHeader(anyString());
            try {
                verify(filterService).getVersions();
            }catch(InternalServerException ise){
                fail(ise.getLocalizedMessage());
            }
        });
    }

    @Test
    public void testVersionsInternalServerException() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {
            //set up mock user
            User user = new User();
            user.setTenant("111");
            user.setPassword("pass");
            user.setToken("fake-token");
            user.setUserid("1");
            user.setUsername("fake-user");

            IUserService userServiceMock = mock(IUserService.class);
            FilterService filterService = mock(FilterService.class);
            IReposeService reposeService = mock(IReposeService.class);
            ConfigurationFactory configurationFactory = mock(ConfigurationFactory.class);

            when(userServiceMock.isValid(anyString())).thenReturn(true);
            when(userServiceMock.findByToken(anyString())).thenReturn(user);
            try {
                when(filterService.getVersions())
                        .thenThrow(new InternalServerException("all the things!"));
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

            Result result = new Application(configurationFactory, filterService,
                    userServiceMock, reposeService).versions();
            assertEquals(500, result.status());
            assertEquals(contentAsString(result), "{\"message\":\"all the things!\"}");

            verify(userServiceMock).isValid(anyString());
            verify(request, times(1)).getHeader(anyString());
            try {
                verify(filterService).getVersions();
            }catch(InternalServerException ise){
                fail(ise.getLocalizedMessage());
            }
        });
    }

    //test components by version

    @Test
    public void testComponentsByVersionSuccess() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {

            //set up mock user
            User user = new User();
            user.setTenant("111");
            user.setPassword("pass");
            user.setToken("fake-token");
            user.setUserid("1");
            user.setUsername("fake-user");

            List<String> filters = new ArrayList<String>(){
                {
                    add("add-header");
                    add("ip-user");
                    add("compression");
                }
            };

            IUserService userServiceMock = mock(IUserService.class);
            FilterService filterService = mock(FilterService.class);
            IReposeService reposeService = mock(IReposeService.class);
            ConfigurationFactory configurationFactory = mock(ConfigurationFactory.class);

            when(userServiceMock.isValid(anyString())).thenReturn(true);
            try {
                when(filterService.getFiltersByVersion(anyString())).thenReturn(filters);
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

            Result result = new Application(configurationFactory, filterService,
                    userServiceMock, reposeService).componentsByVersion("1");
            assertEquals(200, result.status());
            assertEquals("[\"add-header\",\"ip-user\",\"compression\"]",
                    contentAsString(result));

            verify(userServiceMock).isValid(anyString());
            verify(request, times(1)).getHeader(anyString());
            try {
                verify(filterService).getFiltersByVersion(anyString());
            }catch(InternalServerException ise){
                fail(ise.getLocalizedMessage());
            }
        });
    }

    @Test
    public void testComponentsByVersionUnauthorized() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {
            IUserService userServiceMock = mock(IUserService.class);
            FilterService filterService = mock(FilterService.class);
            IReposeService reposeService = mock(IReposeService.class);
            ConfigurationFactory configurationFactory = mock(ConfigurationFactory.class);

            when(userServiceMock.isValid(anyString())).thenReturn(false);

            Map<String, String> flashData = Collections.emptyMap();
            Map<String, Object> argData = Collections.emptyMap();
            RequestHeader header = mock(RequestHeader.class);
            Http.Request request = mock(Http.Request.class);
            when(request.getHeader("Token")).thenReturn("fake-token");
            Http.Context context = new Http.Context(2L, header, request, flashData, flashData, argData);
            Http.Context.current.set(context);

            Result result = new Application(configurationFactory, filterService,
                    userServiceMock, reposeService).componentsByVersion("1");
            assertEquals(401, result.status());

            verify(userServiceMock).isValid(anyString());
            verify(userServiceMock, never()).findByToken(anyString());
            verify(request, times(1)).getHeader(anyString());

            try {
                verify(filterService, never()).getFiltersByVersion(anyString());
            }catch(InternalServerException ise){
                fail(ise.getLocalizedMessage());
            }
        });
    }

    @Test
    public void testComponentsByVersionNull() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {

            //set up mock user
            User user = new User();
            user.setTenant("111");
            user.setPassword("pass");
            user.setToken("fake-token");
            user.setUserid("1");
            user.setUsername("fake-user");

            IUserService userServiceMock = mock(IUserService.class);
            FilterService filterService = mock(FilterService.class);
            IReposeService reposeService = mock(IReposeService.class);
            ConfigurationFactory configurationFactory = mock(ConfigurationFactory.class);

            when(userServiceMock.isValid(anyString())).thenReturn(true);
            try {
                when(filterService.getFiltersByVersion(anyString())).thenReturn(null);
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

            Result result = new Application(configurationFactory, filterService,
                    userServiceMock, reposeService).componentsByVersion("1");
            assertEquals(404, result.status());

            verify(userServiceMock).isValid(anyString());
            verify(request, times(1)).getHeader(anyString());
            try {
                verify(filterService).getFiltersByVersion(anyString());
            }catch(InternalServerException ise){
                fail(ise.getLocalizedMessage());
            }
        });
    }

    @Test
    public void testComponentsByVersionInternalServerException() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {
            //set up mock user
            User user = new User();
            user.setTenant("111");
            user.setPassword("pass");
            user.setToken("fake-token");
            user.setUserid("1");
            user.setUsername("fake-user");

            IUserService userServiceMock = mock(IUserService.class);
            FilterService filterService = mock(FilterService.class);
            IReposeService reposeService = mock(IReposeService.class);
            ConfigurationFactory configurationFactory = mock(ConfigurationFactory.class);

            when(userServiceMock.isValid(anyString())).thenReturn(true);
            when(userServiceMock.findByToken(anyString())).thenReturn(user);
            try {
                when(filterService.getFiltersByVersion(anyString()))
                        .thenThrow(new InternalServerException("all the things!"));
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

            Result result = new Application(configurationFactory, filterService,
                    userServiceMock, reposeService).componentsByVersion("1");
            assertEquals(500, result.status());
            assertEquals(contentAsString(result), "{\"message\":\"all the things!\"}");

            verify(userServiceMock).isValid(anyString());
            verify(request, times(1)).getHeader(anyString());
            try {
                verify(filterService).getFiltersByVersion(anyString());
            }catch(InternalServerException ise){
                fail(ise.getLocalizedMessage());
            }
        });
    }

    @Test
    public void testComponent() throws Exception {

    }

    //test component

    @Test
    public void testComponentSuccess() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {

            //set up mock user
            User user = new User();
            user.setTenant("111");
            user.setPassword("pass");
            user.setToken("fake-token");
            user.setUserid("1");
            user.setUsername("fake-user");

            ObjectNode component = JsonNodeFactory.instance.objectNode();
            component.put("test", "data");

            IUserService userServiceMock = mock(IUserService.class);
            FilterService filterService = mock(FilterService.class);
            IReposeService reposeService = mock(IReposeService.class);
            ConfigurationFactory configurationFactory = mock(ConfigurationFactory.class);

            when(userServiceMock.isValid(anyString())).thenReturn(true);
            try {
                when(filterService.getComponentData(anyString(), anyString())).
                        thenReturn(component);
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

            Result result = new Application(configurationFactory, filterService,
                    userServiceMock, reposeService).component("1", "2");
            assertEquals(200, result.status());
            assertEquals("{\"test\":\"data\"}",
                    contentAsString(result));

            verify(userServiceMock).isValid(anyString());
            verify(request, times(1)).getHeader(anyString());
            try {
                verify(filterService).getComponentData(anyString(), anyString());
            }catch(InternalServerException ise){
                fail(ise.getLocalizedMessage());
            }
        });
    }

    @Test
    public void testComponentUnauthorized() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {
            IUserService userServiceMock = mock(IUserService.class);
            FilterService filterService = mock(FilterService.class);
            IReposeService reposeService = mock(IReposeService.class);
            ConfigurationFactory configurationFactory = mock(ConfigurationFactory.class);

            when(userServiceMock.isValid(anyString())).thenReturn(false);

            Map<String, String> flashData = Collections.emptyMap();
            Map<String, Object> argData = Collections.emptyMap();
            RequestHeader header = mock(RequestHeader.class);
            Http.Request request = mock(Http.Request.class);
            when(request.getHeader("Token")).thenReturn("fake-token");
            Http.Context context = new Http.Context(2L, header, request, flashData, flashData, argData);
            Http.Context.current.set(context);

            Result result = new Application(configurationFactory, filterService,
                    userServiceMock, reposeService).component("1", "3");
            assertEquals(401, result.status());

            verify(userServiceMock).isValid(anyString());
            verify(request, times(1)).getHeader(anyString());

            try {
                verify(filterService, never()).getComponentData(anyString(), anyString());
            }catch(InternalServerException ise){
                fail(ise.getLocalizedMessage());
            }
        });
    }

    @Test
    public void testComponentNull() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {

            //set up mock user
            User user = new User();
            user.setTenant("111");
            user.setPassword("pass");
            user.setToken("fake-token");
            user.setUserid("1");
            user.setUsername("fake-user");

            IUserService userServiceMock = mock(IUserService.class);
            FilterService filterService = mock(FilterService.class);
            IReposeService reposeService = mock(IReposeService.class);
            ConfigurationFactory configurationFactory = mock(ConfigurationFactory.class);

            when(userServiceMock.isValid(anyString())).thenReturn(true);
            try {
                when(filterService.getComponentData(anyString(), anyString())).
                        thenReturn(null);
            }catch (InternalServerException ise){
                fail(ise.getLocalizedMessage());
            }

            Map<String, String> flashData = Collections.emptyMap();
            Map<String, Object> argData = Collections.emptyMap();
            RequestHeader header = mock(RequestHeader.class);
            Http.Request request = mock(Http.Request.class);
            when(request.getHeader("Token")).thenReturn("fake-token");
            Http.Context context = new Http.Context(2L, header, request, flashData, flashData, argData);
            Http.Context.current.set(context);

            Result result = new Application(configurationFactory, filterService,
                    userServiceMock, reposeService).component("1", "2");
            assertEquals(404, result.status());

            verify(userServiceMock).isValid(anyString());
            verify(request, times(1)).getHeader(anyString());
            try {
                verify(filterService).getComponentData(anyString(), anyString());
            } catch (InternalServerException ise){
                fail(ise.getLocalizedMessage());
            }
        });
    }

    @Test
    public void testComponentInternalServerException() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {
            //set up mock user
            User user = new User();
            user.setTenant("111");
            user.setPassword("pass");
            user.setToken("fake-token");
            user.setUserid("1");
            user.setUsername("fake-user");

            IUserService userServiceMock = mock(IUserService.class);
            FilterService filterService = mock(FilterService.class);
            IReposeService reposeService = mock(IReposeService.class);
            ConfigurationFactory configurationFactory = mock(ConfigurationFactory.class);

            when(userServiceMock.isValid(anyString())).thenReturn(true);
            when(userServiceMock.findByToken(anyString())).thenReturn(user);
            try {
                when(filterService.getComponentData(anyString(), anyString()))
                        .thenThrow(new InternalServerException("all the things!"));
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

            Result result = new Application(configurationFactory, filterService,
                    userServiceMock, reposeService).component("1", "2");
            assertEquals(500, result.status());
            assertEquals(contentAsString(result), "{\"message\":\"all the things!\"}");

            verify(userServiceMock).isValid(anyString());
            verify(request, times(1)).getHeader(anyString());
            try {
                verify(filterService).getComponentData(anyString(), anyString());
            } catch (InternalServerException ise){
                fail(ise.getLocalizedMessage());
            }
        });
    }

    //test build

    @Test
    public void testBuildSuccess() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {

            //set up mock user
            User user = new User();
            user.setTenant("111");
            user.setPassword("pass");
            user.setToken("fake-token");
            user.setUserid("1");
            user.setUsername("fake-user");

            List<models.Configuration> configurationList = new ArrayList<Configuration>(){
                {
                    add(new Configuration("add-header", "lotsaxml"));
                    add(new Configuration("ip-user", "lotsaxml"));
                }
            };

            ObjectNode requestJson = JsonNodeFactory.instance.objectNode();
            requestJson.put("request", "yes");
            requestJson.put("message", "test");

            String reposeId = "1";

            IUserService userServiceMock = mock(IUserService.class);
            FilterService filterService = mock(FilterService.class);
            IReposeService reposeService = mock(IReposeService.class);
            ConfigurationFactory configurationFactory = mock(ConfigurationFactory.class);

            when(userServiceMock.isValid(anyString())).thenReturn(true);
            try {
                when(userServiceMock.findByToken(any())).thenReturn(user);
                when(reposeService.setUpReposeEnvironment(any(), any(),
                        anyString(), any())).
                        thenReturn(reposeId);
                when(configurationFactory.translateConfigurationsFromJson(any(), anyString(),
                        any())).thenReturn(configurationList);
            }catch(InternalServerException | NotFoundException ise){
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

            Result result = new Application(configurationFactory, filterService,
                    userServiceMock, reposeService).build("1");
            assertEquals(200, result.status());
            assertEquals("{\"message\":\"success\",\"id\":\"1\"}",
                    contentAsString(result));

            verify(userServiceMock).isValid(anyString());
            verify(request, times(1)).getHeader(anyString());
            try {
                verify(userServiceMock).findByToken(anyString());
                verify(reposeService).setUpReposeEnvironment(any(), any(), anyString(),
                        any());
                verify(configurationFactory).translateConfigurationsFromJson(any(),
                        any(), any());
            }catch(InternalServerException | NotFoundException ise){
                fail(ise.getLocalizedMessage());
            }
        });
    }

    @Test
    public void testBuildBodyNull() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {

            IUserService userServiceMock = mock(IUserService.class);
            FilterService filterService = mock(FilterService.class);
            IReposeService reposeService = mock(IReposeService.class);
            ConfigurationFactory configurationFactory = mock(ConfigurationFactory.class);

            Map<String, String> flashData = Collections.emptyMap();
            Map<String, Object> argData = Collections.emptyMap();
            RequestHeader header = mock(RequestHeader.class);
            Http.Request request = mock(Http.Request.class);
            when(request.getHeader("Token")).thenReturn("fake-token");
            Http.Context context = new Http.Context(2L, header, request, flashData, flashData, argData);
            Http.Context.current.set(context);

            Result result = new Application(configurationFactory, filterService,
                    userServiceMock, reposeService).build("1");
            assertEquals(400, result.status());

            verify(userServiceMock, never()).isValid(anyString());
            verify(request, never()).getHeader(anyString());
            try {
                verify(reposeService, never()).setUpReposeEnvironment(any(), any(), anyString(),
                        any());
                verify(configurationFactory, never()).translateConfigurationsFromJson(any(),
                        any(), any());
                verify(userServiceMock, never()).findByToken(any());
            }catch(InternalServerException | NotFoundException ise){
                fail(ise.getLocalizedMessage());
            }
        });
    }

    @Test
    public void testBuildUnauthorized() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {
            IUserService userServiceMock = mock(IUserService.class);
            FilterService filterService = mock(FilterService.class);
            IReposeService reposeService = mock(IReposeService.class);
            ConfigurationFactory configurationFactory = mock(ConfigurationFactory.class);

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

            Result result = new Application(configurationFactory, filterService,
                    userServiceMock, reposeService).build("1");
            assertEquals(401, result.status());

            verify(userServiceMock).isValid(anyString());
            verify(request).getHeader(anyString());
            try {
                verify(reposeService, never()).setUpReposeEnvironment(any(), any(), anyString(),
                        any());
                verify(configurationFactory, never()).translateConfigurationsFromJson(any(),
                        any(), any());
                verify(userServiceMock, never()).findByToken(any());
            }catch(InternalServerException | NotFoundException ise){
                fail(ise.getLocalizedMessage());
            }
        });
    }

    @Test
    public void testBuildUserNull() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {

            ObjectNode requestJson = JsonNodeFactory.instance.objectNode();
            requestJson.put("request", "yes");
            requestJson.put("message", "test");

            String reposeId = "1";

            IUserService userServiceMock = mock(IUserService.class);
            FilterService filterService = mock(FilterService.class);
            IReposeService reposeService = mock(IReposeService.class);
            ConfigurationFactory configurationFactory = mock(ConfigurationFactory.class);

            when(userServiceMock.isValid(anyString())).thenReturn(true);
            try {
                when(userServiceMock.findByToken(any())).thenReturn(null);
                when(reposeService.setUpReposeEnvironment(any(), any(),
                        anyString(), any())).
                        thenReturn(reposeId);
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

            Result result = new Application(configurationFactory, filterService,
                    userServiceMock, reposeService).build("1");
            assertEquals(401, result.status());

            verify(userServiceMock).isValid(anyString());
            verify(request, times(1)).getHeader(anyString());
            try {
                verify(userServiceMock).findByToken(anyString());
                verify(reposeService, never()).setUpReposeEnvironment(any(), any(), anyString(),
                        any());
                verify(configurationFactory, never()).translateConfigurationsFromJson(any(),
                        any(), any());
            }catch(InternalServerException | NotFoundException ise){
                fail(ise.getLocalizedMessage());
            }
        });
    }

    @Test
    public void testBuildNull() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {

            //set up mock user
            User user = new User();
            user.setTenant("111");
            user.setPassword("pass");
            user.setToken("fake-token");
            user.setUserid("1");
            user.setUsername("fake-user");

            List<models.Configuration> configurationList = new ArrayList<Configuration>(){
                {
                    add(new Configuration("add-header", "lotsaxml"));
                    add(new Configuration("ip-user", "lotsaxml"));
                }
            };

            ObjectNode requestJson = JsonNodeFactory.instance.objectNode();
            requestJson.put("request", "yes");
            requestJson.put("message", "test");

            IUserService userServiceMock = mock(IUserService.class);
            FilterService filterService = mock(FilterService.class);
            IReposeService reposeService = mock(IReposeService.class);
            ConfigurationFactory configurationFactory = mock(ConfigurationFactory.class);

            when(userServiceMock.isValid(anyString())).thenReturn(true);
            try {
                when(userServiceMock.findByToken(any())).thenReturn(user);
                when(reposeService.setUpReposeEnvironment(any(), any(),
                        anyString(), any())).
                        thenReturn(null);
                when(configurationFactory.translateConfigurationsFromJson(any(), anyString(),
                        any())).thenReturn(configurationList);
            }catch(InternalServerException | NotFoundException ise){
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

            Result result = new Application(configurationFactory, filterService,
                    userServiceMock, reposeService).build("1");
            assertEquals(500, result.status());
            assertEquals("{\"message\":\"unable to create repose environment.\"}",
                    contentAsString(result));

            verify(userServiceMock).isValid(anyString());
            verify(request, times(1)).getHeader(anyString());
            try {
                verify(userServiceMock).findByToken(anyString());
                verify(reposeService).setUpReposeEnvironment(any(), any(), anyString(),
                        any());
                verify(configurationFactory).translateConfigurationsFromJson(any(),
                        any(), any());
            }catch(InternalServerException | NotFoundException ise){
                fail(ise.getLocalizedMessage());
            }
        });
    }

    @Test
    public void testBuildNotFoundException() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {

            //set up mock user
            User user = new User();
            user.setTenant("111");
            user.setPassword("pass");
            user.setToken("fake-token");
            user.setUserid("1");
            user.setUsername("fake-user");

            List<models.Configuration> configurationList = new ArrayList<Configuration>(){
                {
                    add(new Configuration("add-header", "lotsaxml"));
                    add(new Configuration("ip-user", "lotsaxml"));
                }
            };

            ObjectNode requestJson = JsonNodeFactory.instance.objectNode();
            requestJson.put("request", "yes");
            requestJson.put("message", "test");

            String reposeId = "1";

            IUserService userServiceMock = mock(IUserService.class);
            FilterService filterService = mock(FilterService.class);
            IReposeService reposeService = mock(IReposeService.class);
            ConfigurationFactory configurationFactory = mock(ConfigurationFactory.class);

            when(userServiceMock.isValid(anyString())).thenReturn(true);
            try {
                when(userServiceMock.findByToken(any())).thenReturn(user);
                when(reposeService.setUpReposeEnvironment(any(), any(),
                        anyString(), any())).
                        thenReturn(reposeId);
                when(configurationFactory.translateConfigurationsFromJson(any(), anyString(),
                        any())).thenThrow(new NotFoundException("configs not found"));
            }catch(InternalServerException | NotFoundException ise){
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

            Result result = new Application(configurationFactory, filterService,
                    userServiceMock, reposeService).build("1");
            assertEquals(400, result.status());
            assertEquals("{\"message\":\"configs not found\"}",
                    contentAsString(result));

            verify(userServiceMock).isValid(anyString());
            verify(request, times(1)).getHeader(anyString());
            try {
                verify(userServiceMock).findByToken(anyString());
                verify(reposeService, never()).setUpReposeEnvironment(any(), any(), anyString(),
                        any());
                verify(configurationFactory).translateConfigurationsFromJson(any(),
                        any(), any());
            }catch(InternalServerException | NotFoundException ise){
                fail(ise.getLocalizedMessage());
            }
        });
    }

    @Test
    public void testBuildInternalServerException() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {

            //set up mock user
            User user = new User();
            user.setTenant("111");
            user.setPassword("pass");
            user.setToken("fake-token");
            user.setUserid("1");
            user.setUsername("fake-user");

            List<models.Configuration> configurationList = new ArrayList<Configuration>(){
                {
                    add(new Configuration("add-header", "lotsaxml"));
                    add(new Configuration("ip-user", "lotsaxml"));
                }
            };

            ObjectNode requestJson = JsonNodeFactory.instance.objectNode();
            requestJson.put("request", "yes");
            requestJson.put("message", "test");

            IUserService userServiceMock = mock(IUserService.class);
            FilterService filterService = mock(FilterService.class);
            IReposeService reposeService = mock(IReposeService.class);
            ConfigurationFactory configurationFactory = mock(ConfigurationFactory.class);

            when(userServiceMock.isValid(anyString())).thenReturn(true);
            try {
                when(userServiceMock.findByToken(any())).thenReturn(user);
                when(reposeService.setUpReposeEnvironment(any(), any(),
                        anyString(), any())).
                        thenThrow(new InternalServerException("failed all the things"));
                when(configurationFactory.translateConfigurationsFromJson(any(), anyString(),
                        any())).thenReturn(configurationList);
            }catch(InternalServerException | NotFoundException ise){
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

            Result result = new Application(configurationFactory, filterService,
                    userServiceMock, reposeService).build("1");
            assertEquals(500, result.status());
            assertEquals("{\"message\":\"failed all the things\"}",
                    contentAsString(result));

            verify(userServiceMock).isValid(anyString());
            verify(request, times(1)).getHeader(anyString());
            try {
                verify(userServiceMock).findByToken(anyString());
                verify(reposeService).setUpReposeEnvironment(any(), any(), anyString(),
                        any());
                verify(configurationFactory).translateConfigurationsFromJson(any(),
                        any(), any());
            }catch(InternalServerException | NotFoundException ise){
                fail(ise.getLocalizedMessage());
            }
        });
    }
}