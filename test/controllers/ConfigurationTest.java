package controllers;

import exceptions.InternalServerException;
import exceptions.NotFoundException;
import factories.ConfigurationFactory;
import models.User;
import org.apache.http.HttpEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.junit.Test;
import play.api.mvc.RequestHeader;
import play.mvc.Http;
import play.mvc.Result;
import play.test.WithApplication;
import services.ConfigurationService;
import services.IReposeService;
import services.IUserService;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static play.test.Helpers.*;

/**
 * Created by dimi5963 on 3/5/16.
 */
public class ConfigurationTest extends WithApplication {

    //test configurations

    @Test
    public void testConfigurationsSuccess() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {

            //set up mock user
            User user = new User();
            user.setTenant("111");
            user.setPassword("pass");
            user.setToken("fake-token");
            user.setUserid("1");
            user.setUsername("fake-user");

            //mock list
            List<models.Configuration> configurationList = new ArrayList<models.Configuration>(){
                {
                    add(new models.Configuration("filter-name", "filter-xml"));
                    add(new models.Configuration("filter-name2", "filter-xml2"));
                    add(new models.Configuration("filter-name3", "filter-xml3"));
                }
            };


            IUserService userServiceMock = mock(IUserService.class);
            IReposeService reposeServiceMock = mock(IReposeService.class);
            ConfigurationService configurationServiceMock = mock(ConfigurationService.class);
            ConfigurationFactory configurationFactoryMock = mock(ConfigurationFactory.class);

            when(userServiceMock.isValid(anyString())).thenReturn(true);
            when(userServiceMock.findByToken(anyString())).thenReturn(user);
            try {
                when(configurationServiceMock.getConfigurationsForInstance(any(), anyString())).
                        thenReturn(configurationList);
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

            Result result = new Configuration(userServiceMock, reposeServiceMock,
                    configurationServiceMock, configurationFactoryMock).configurations("1");
            assertEquals(200, result.status());
            assertEquals("[{\"name\":\"filter-name\",\"xml\":\"filter-xml\"}," +
                    "{\"name\":\"filter-name2\",\"xml\":\"filter-xml2\"}," +
                    "{\"name\":\"filter-name3\",\"xml\":\"filter-xml3\"}]", contentAsString(result));

            verify(userServiceMock).isValid(anyString());
            verify(userServiceMock, times(1)).findByToken(anyString());
            verify(request, times(1)).getHeader(anyString());
            try {
                verify(configurationServiceMock).getConfigurationsForInstance(any(), anyString());
            }catch(InternalServerException ise){
                fail(ise.getLocalizedMessage());
            }
        });
    }

    @Test
    public void testConfigurationsUnauthorized() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {

            IUserService userServiceMock = mock(IUserService.class);
            IReposeService reposeServiceMock = mock(IReposeService.class);
            ConfigurationService configurationServiceMock = mock(ConfigurationService.class);
            ConfigurationFactory configurationFactoryMock = mock(ConfigurationFactory.class);

            when(userServiceMock.isValid(anyString())).thenReturn(false);

            Map<String, String> flashData = Collections.emptyMap();
            Map<String, Object> argData = Collections.emptyMap();
            RequestHeader header = mock(RequestHeader.class);
            Http.Request request = mock(Http.Request.class);
            when(request.getHeader("Token")).thenReturn("fake-token");
            Http.Context context = new Http.Context(2L, header, request, flashData, flashData, argData);
            Http.Context.current.set(context);

            Result result = new Configuration(userServiceMock, reposeServiceMock,
                    configurationServiceMock, configurationFactoryMock).configurations("1");
            assertEquals(401, result.status());

            verify(userServiceMock).isValid(anyString());
            verify(userServiceMock, never()).findByToken(anyString());
            verify(request, times(1)).getHeader(anyString());

            try {
                verify(configurationServiceMock, never()).getConfigurationsForInstance(any(), anyString());
            }catch(InternalServerException ise){
                fail(ise.getLocalizedMessage());
            }
        });
    }

    @Test
    public void testConfigurationsNoToken() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {

            IUserService userServiceMock = mock(IUserService.class);
            IReposeService reposeServiceMock = mock(IReposeService.class);
            ConfigurationService configurationServiceMock = mock(ConfigurationService.class);
            ConfigurationFactory configurationFactoryMock = mock(ConfigurationFactory.class);

            when(userServiceMock.isValid(anyString())).thenReturn(false);

            Map<String, String> flashData = Collections.emptyMap();
            Map<String, Object> argData = Collections.emptyMap();
            RequestHeader header = mock(RequestHeader.class);
            Http.Request request = mock(Http.Request.class);
            when(request.getHeader("Token")).thenReturn(null);
            Http.Context context = new Http.Context(2L, header, request, flashData, flashData, argData);
            Http.Context.current.set(context);

            Result result = new Configuration(userServiceMock, reposeServiceMock,
                    configurationServiceMock, configurationFactoryMock).configurations("1");
            assertEquals(401, result.status());

            verify(userServiceMock).isValid(anyString());
            verify(userServiceMock, never()).findByToken(anyString());
            verify(request, times(1)).getHeader(anyString());

            try {
                verify(configurationServiceMock, never()).getConfigurationsForInstance(any(), anyString());
            }catch(InternalServerException ise){
                fail(ise.getLocalizedMessage());
            }
        });

    }

    @Test
    public void testConfigurationsNullUser() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {
            //set up mock user
            User user = null;

            IUserService userServiceMock = mock(IUserService.class);
            IReposeService reposeServiceMock = mock(IReposeService.class);
            ConfigurationService configurationServiceMock = mock(ConfigurationService.class);
            ConfigurationFactory configurationFactoryMock = mock(ConfigurationFactory.class);

            when(userServiceMock.isValid(anyString())).thenReturn(true);
            when(userServiceMock.findByToken(anyString())).thenReturn(user);

            Map<String, String> flashData = Collections.emptyMap();
            Map<String, Object> argData = Collections.emptyMap();
            RequestHeader header = mock(RequestHeader.class);
            Http.Request request = mock(Http.Request.class);
            when(request.getHeader("Token")).thenReturn("fake-token");
            Http.Context context = new Http.Context(2L, header, request, flashData, flashData, argData);
            Http.Context.current.set(context);

            Result result = new Configuration(userServiceMock, reposeServiceMock,
                    configurationServiceMock, configurationFactoryMock).configurations("1");
            assertEquals(401, result.status());

            verify(userServiceMock).isValid(anyString());
            verify(userServiceMock, times(1)).findByToken(anyString());
            verify(request, times(1)).getHeader(anyString());

            try {
                verify(configurationServiceMock, never()).getConfigurationsForInstance(any(), anyString());
            }catch(InternalServerException ise){
                fail(ise.getLocalizedMessage());
            }
        });
    }

    @Test
    public void testConfigurationsEmpty() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {

            //set up mock user
            User user = new User();
            user.setTenant("111");
            user.setPassword("pass");
            user.setToken("fake-token");
            user.setUserid("1");
            user.setUsername("fake-user");

            IUserService userServiceMock = mock(IUserService.class);
            IReposeService reposeServiceMock = mock(IReposeService.class);
            ConfigurationService configurationServiceMock = mock(ConfigurationService.class);
            ConfigurationFactory configurationFactoryMock = mock(ConfigurationFactory.class);

            when(userServiceMock.isValid(anyString())).thenReturn(true);
            when(userServiceMock.findByToken(anyString())).thenReturn(user);
            try {
                when(configurationServiceMock.
                        getConfigurationsForInstance(any(), anyString())).
                        thenReturn(new ArrayList<models.Configuration>());
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

            Result result = new Configuration(userServiceMock, reposeServiceMock,
                    configurationServiceMock, configurationFactoryMock).configurations("1");
            assertEquals(200, result.status());
            assertEquals("[]", contentAsString(result));

            verify(userServiceMock).isValid(anyString());
            verify(userServiceMock, times(1)).findByToken(anyString());
            verify(request, times(1)).getHeader(anyString());
            try {
                verify(configurationServiceMock).getConfigurationsForInstance(any(), any());
            }catch(InternalServerException ise){
                fail(ise.getLocalizedMessage());
            }
        });
    }

    @Test
    public void testConfigurationsNull() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {

            //set up mock user
            User user = new User();
            user.setTenant("111");
            user.setPassword("pass");
            user.setToken("fake-token");
            user.setUserid("1");
            user.setUsername("fake-user");

            IUserService userServiceMock = mock(IUserService.class);
            IReposeService reposeServiceMock = mock(IReposeService.class);
            ConfigurationService configurationServiceMock = mock(ConfigurationService.class);
            ConfigurationFactory configurationFactoryMock = mock(ConfigurationFactory.class);

            when(userServiceMock.isValid(anyString())).thenReturn(true);
            when(userServiceMock.findByToken(anyString())).thenReturn(user);
            try {
                when(configurationServiceMock.
                        getConfigurationsForInstance(any(), anyString())).thenReturn(null);
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

            Result result = new Configuration(userServiceMock, reposeServiceMock,
                    configurationServiceMock, configurationFactoryMock).configurations("1");
            assertEquals(200, result.status());
            assertEquals("[]", contentAsString(result));

            verify(userServiceMock).isValid(anyString());
            verify(userServiceMock, times(1)).findByToken(anyString());
            verify(request, times(1)).getHeader(anyString());
            try {
                verify(configurationServiceMock).getConfigurationsForInstance(any(), any());
            }catch(InternalServerException ise){
                fail(ise.getLocalizedMessage());
            }
        });
    }

    @Test
    public void testConfigurationsInternalServerException() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {
            //set up mock user
            User user = new User();
            user.setTenant("111");
            user.setPassword("pass");
            user.setToken("fake-token");
            user.setUserid("1");
            user.setUsername("fake-user");

            IUserService userServiceMock = mock(IUserService.class);
            IReposeService reposeServiceMock = mock(IReposeService.class);
            ConfigurationService configurationServiceMock = mock(ConfigurationService.class);
            ConfigurationFactory configurationFactoryMock = mock(ConfigurationFactory.class);

            when(userServiceMock.isValid(anyString())).thenReturn(true);
            when(userServiceMock.findByToken(anyString())).thenReturn(user);
            try {
                when(configurationServiceMock.getConfigurationsForInstance(any(), anyString()))
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

            Result result = new Configuration(userServiceMock, reposeServiceMock,
                    configurationServiceMock, configurationFactoryMock).configurations("1");
            assertEquals(500, result.status());
            assertEquals(contentAsString(result), "{\"message\":\"all the things!\"}");

            verify(userServiceMock).isValid(anyString());
            verify(userServiceMock, times(1)).findByToken(anyString());
            verify(request, times(1)).getHeader(anyString());
            try {
                verify(configurationServiceMock).getConfigurationsForInstance(any(), anyString());
            }catch(InternalServerException ise){
                fail(ise.getLocalizedMessage());
            }
        });
    }


    //test upload configurations

    @Test
    public void testUploadConfigurationsSuccess() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {

            //set up mock user
            User user = new User();
            user.setTenant("111");
            user.setPassword("pass");
            user.setToken("fake-token");
            user.setUserid("1");
            user.setUsername("fake-user");

            //mock list
            List<models.Configuration> configurationList = new ArrayList<models.Configuration>(){
                {
                    add(new models.Configuration("filter-name", "filter-xml"));
                    add(new models.Configuration("filter-name2", "filter-xml2"));
                    add(new models.Configuration("filter-name3", "filter-xml3"));
                }
            };


            String reposeId = "1";

            IUserService userServiceMock = mock(IUserService.class);
            IReposeService reposeServiceMock = mock(IReposeService.class);
            ConfigurationService configurationServiceMock = mock(ConfigurationService.class);
            ConfigurationFactory configurationFactoryMock = mock(ConfigurationFactory.class);

            when(userServiceMock.isValid(anyString())).thenReturn(true);
            when(userServiceMock.findByToken(anyString())).thenReturn(user);
            try {
                when(reposeServiceMock.setUpReposeEnvironment(any(), any(), anyString(), anyList())).
                        thenReturn(reposeId);
                when(configurationFactoryMock.translateConfigurationsFromUpload(any(), anyString(), any())).
                        thenReturn(configurationList);
            }catch(InternalServerException | NotFoundException ise){
                fail(ise.getLocalizedMessage());
            }

            Http.MultipartFormData.FilePart part =
                    new Http.MultipartFormData.FilePart("picture","test-image.jpg",
                            "image/jpeg",new File("test-image.jpg"));



            MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
            HttpEntity httpEntity = multipartEntityBuilder.addTextBody("test", "test").build();

            Map<String, String> flashData = Collections.emptyMap();
            Map<String, Object> argData = Collections.emptyMap();
            RequestHeader header = mock(RequestHeader.class);
            Http.Request request = mock(Http.Request.class);
            when(request.getHeader("Token")).thenReturn("fake-token");
            Http.RequestBody requestBody = mock(Http.RequestBody.class);
            when(requestBody.asMultipartFormData()).thenReturn(new Http.MultipartFormData() {
                @Override
                public Map<String, String[]> asFormUrlEncoded() {
                    return null;
                }

                @Override
                public List<FilePart> getFiles() {
                    return new ArrayList<FilePart>(){
                        {
                            add(new Http.MultipartFormData.FilePart("picture","test-image.jpg",
                                    "image/jpeg",new File("test-image.jpg")));
                        }
                    };
                }
            });
            when(request.body()).thenReturn(requestBody);

            Http.Context context = new Http.Context(2L, header, request, flashData, flashData, argData);
            Http.Context.current.set(context);

            Result result = new Configuration(userServiceMock, reposeServiceMock,
                    configurationServiceMock, configurationFactoryMock).uploadReposeConfigs("1");
            assertEquals(200, result.status());
            assertEquals("{\"message\":\"success\",\"id\":\"1\"}", contentAsString(result));

            verify(userServiceMock).isValid(anyString());
            verify(userServiceMock, times(1)).findByToken(anyString());
            verify(request, times(1)).getHeader(anyString());
            try {
                verify(reposeServiceMock).setUpReposeEnvironment(any(), any(), anyString(), anyList());
                verify(configurationFactoryMock).translateConfigurationsFromUpload(any(), anyString(), any());
            }catch(InternalServerException | NotFoundException ise){
                fail(ise.getLocalizedMessage());
            }
        });
    }

    @Test
    public void testUploadConfigurationsUnauthorized() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {

            IUserService userServiceMock = mock(IUserService.class);
            IReposeService reposeServiceMock = mock(IReposeService.class);
            ConfigurationService configurationServiceMock = mock(ConfigurationService.class);
            ConfigurationFactory configurationFactoryMock = mock(ConfigurationFactory.class);

            when(userServiceMock.isValid(anyString())).thenReturn(false);

            Map<String, String> flashData = Collections.emptyMap();
            Map<String, Object> argData = Collections.emptyMap();
            RequestHeader header = mock(RequestHeader.class);
            Http.Request request = mock(Http.Request.class);
            when(request.getHeader("Token")).thenReturn("fake-token");
            Http.RequestBody requestBody = mock(Http.RequestBody.class);
            when(requestBody.asMultipartFormData()).thenReturn(new Http.MultipartFormData() {
                @Override
                public Map<String, String[]> asFormUrlEncoded() {
                    return null;
                }

                @Override
                public List<FilePart> getFiles() {
                    return new ArrayList<FilePart>(){
                        {
                            add(new Http.MultipartFormData.FilePart("picture","test-image.jpg",
                                    "image/jpeg",new File("test-image.jpg")));
                        }
                    };
                }
            });
            when(request.body()).thenReturn(requestBody);
            Http.Context context = new Http.Context(2L, header, request, flashData, flashData, argData);
            Http.Context.current.set(context);

            Result result = new Configuration(userServiceMock, reposeServiceMock,
                    configurationServiceMock, configurationFactoryMock).uploadReposeConfigs("1");
            assertEquals(401, result.status());

            verify(userServiceMock).isValid(anyString());
            verify(userServiceMock, never()).findByToken(anyString());
            verify(request, times(1)).getHeader(anyString());

            try {
                verify(reposeServiceMock, never()).setUpReposeEnvironment(any(), any(), anyString(), anyList());
                verify(configurationFactoryMock, never()).translateConfigurationsFromUpload(any(), anyString(), any());
            }catch(InternalServerException | NotFoundException ise){
                fail(ise.getLocalizedMessage());
            }
        });
    }

    @Test
    public void testUploadConfigurationsNoToken() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {

            IUserService userServiceMock = mock(IUserService.class);
            IReposeService reposeServiceMock = mock(IReposeService.class);
            ConfigurationService configurationServiceMock = mock(ConfigurationService.class);
            ConfigurationFactory configurationFactoryMock = mock(ConfigurationFactory.class);

            when(userServiceMock.isValid(anyString())).thenReturn(false);

            Map<String, String> flashData = Collections.emptyMap();
            Map<String, Object> argData = Collections.emptyMap();
            RequestHeader header = mock(RequestHeader.class);
            Http.Request request = mock(Http.Request.class);
            when(request.getHeader("Token")).thenReturn(null);
            Http.RequestBody requestBody = mock(Http.RequestBody.class);
            when(requestBody.asMultipartFormData()).thenReturn(new Http.MultipartFormData() {
                @Override
                public Map<String, String[]> asFormUrlEncoded() {
                    return null;
                }

                @Override
                public List<FilePart> getFiles() {
                    return new ArrayList<FilePart>(){
                        {
                            add(new Http.MultipartFormData.FilePart("picture","test-image.jpg",
                                    "image/jpeg",new File("test-image.jpg")));
                        }
                    };
                }
            });
            when(request.body()).thenReturn(requestBody);
            Http.Context context = new Http.Context(2L, header, request, flashData, flashData, argData);
            Http.Context.current.set(context);

            Result result = new Configuration(userServiceMock, reposeServiceMock,
                    configurationServiceMock, configurationFactoryMock).uploadReposeConfigs("1");
            assertEquals(401, result.status());

            verify(userServiceMock).isValid(anyString());
            verify(userServiceMock, never()).findByToken(anyString());
            verify(request, times(1)).getHeader(anyString());

            try {
                verify(reposeServiceMock, never()).setUpReposeEnvironment(any(), any(), anyString(), anyList());
                verify(configurationFactoryMock, never()).translateConfigurationsFromUpload(any(), anyString(), any());
            }catch(InternalServerException | NotFoundException ise){
                fail(ise.getLocalizedMessage());
            }
        });

    }

    @Test
    public void testUploadConfigurationsNullUser() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {
            //set up mock user
            User user = null;

            IUserService userServiceMock = mock(IUserService.class);
            IReposeService reposeServiceMock = mock(IReposeService.class);
            ConfigurationService configurationServiceMock = mock(ConfigurationService.class);
            ConfigurationFactory configurationFactoryMock = mock(ConfigurationFactory.class);

            when(userServiceMock.isValid(anyString())).thenReturn(true);
            when(userServiceMock.findByToken(anyString())).thenReturn(user);

            Map<String, String> flashData = Collections.emptyMap();
            Map<String, Object> argData = Collections.emptyMap();
            RequestHeader header = mock(RequestHeader.class);
            Http.Request request = mock(Http.Request.class);
            when(request.getHeader("Token")).thenReturn("fake-token");
            Http.RequestBody requestBody = mock(Http.RequestBody.class);
            when(requestBody.asMultipartFormData()).thenReturn(new Http.MultipartFormData() {
                @Override
                public Map<String, String[]> asFormUrlEncoded() {
                    return null;
                }

                @Override
                public List<FilePart> getFiles() {
                    return new ArrayList<FilePart>(){
                        {
                            add(new Http.MultipartFormData.FilePart("picture","test-image.jpg",
                                    "image/jpeg",new File("test-image.jpg")));
                        }
                    };
                }
            });
            when(request.body()).thenReturn(requestBody);
            Http.Context context = new Http.Context(2L, header, request, flashData, flashData, argData);
            Http.Context.current.set(context);

            Result result = new Configuration(userServiceMock, reposeServiceMock,
                    configurationServiceMock, configurationFactoryMock).uploadReposeConfigs("1");
            assertEquals(401, result.status());

            verify(userServiceMock).isValid(anyString());
            verify(userServiceMock, times(1)).findByToken(anyString());
            verify(request, times(1)).getHeader(anyString());

            try {
                verify(reposeServiceMock, never()).setUpReposeEnvironment(any(), any(), anyString(), anyList());
                verify(configurationFactoryMock, never()).translateConfigurationsFromUpload(any(), anyString(), any());
            }catch(InternalServerException | NotFoundException ise){
                fail(ise.getLocalizedMessage());
            }
        });
    }

    @Test
    public void testUploadConfigurationsNotFoundException() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {

            //set up mock user
            User user = new User();
            user.setTenant("111");
            user.setPassword("pass");
            user.setToken("fake-token");
            user.setUserid("1");
            user.setUsername("fake-user");

            IUserService userServiceMock = mock(IUserService.class);
            IReposeService reposeServiceMock = mock(IReposeService.class);
            ConfigurationService configurationServiceMock = mock(ConfigurationService.class);
            ConfigurationFactory configurationFactoryMock = mock(ConfigurationFactory.class);

            when(userServiceMock.isValid(anyString())).thenReturn(true);
            when(userServiceMock.findByToken(anyString())).thenReturn(user);
            try {
                when(configurationFactoryMock.translateConfigurationsFromUpload(any(), anyString(), any())).
                        thenThrow(new NotFoundException("Configurations Not found"));
            }catch(NotFoundException | InternalServerException ise){
                fail(ise.getLocalizedMessage());
            }

            Map<String, String> flashData = Collections.emptyMap();
            Map<String, Object> argData = Collections.emptyMap();
            RequestHeader header = mock(RequestHeader.class);
            Http.Request request = mock(Http.Request.class);
            when(request.getHeader("Token")).thenReturn("fake-token");
            Http.RequestBody requestBody = mock(Http.RequestBody.class);
            when(requestBody.asMultipartFormData()).thenReturn(new Http.MultipartFormData() {
                @Override
                public Map<String, String[]> asFormUrlEncoded() {
                    return null;
                }

                @Override
                public List<FilePart> getFiles() {
                    return new ArrayList<FilePart>(){
                        {
                            add(new Http.MultipartFormData.FilePart("picture","test-image.jpg",
                                    "image/jpeg",new File("test-image.jpg")));
                        }
                    };
                }
            });
            when(request.body()).thenReturn(requestBody);
            Http.Context context = new Http.Context(2L, header, request, flashData, flashData, argData);
            Http.Context.current.set(context);

            Result result = new Configuration(userServiceMock, reposeServiceMock,
                    configurationServiceMock, configurationFactoryMock).uploadReposeConfigs("1");
            assertEquals(400, result.status());
            assertEquals("{\"message\":\"Configurations Not found\"}", contentAsString(result));

            verify(userServiceMock).isValid(anyString());
            verify(userServiceMock, times(1)).findByToken(anyString());
            verify(request, times(1)).getHeader(anyString());
            try {
                verify(reposeServiceMock, never()).setUpReposeEnvironment(any(), any(), anyString(), anyList());
                verify(configurationFactoryMock).translateConfigurationsFromUpload(any(), anyString(), any());
            }catch(InternalServerException | NotFoundException ise){
                fail(ise.getLocalizedMessage());
            }
        });
    }

    @Test
    public void testUploadConfigurationsNull() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {

            //set up mock user
            User user = new User();
            user.setTenant("111");
            user.setPassword("pass");
            user.setToken("fake-token");
            user.setUserid("1");
            user.setUsername("fake-user");

            IUserService userServiceMock = mock(IUserService.class);
            IReposeService reposeServiceMock = mock(IReposeService.class);
            ConfigurationService configurationServiceMock = mock(ConfigurationService.class);
            ConfigurationFactory configurationFactoryMock = mock(ConfigurationFactory.class);

            when(userServiceMock.isValid(anyString())).thenReturn(true);
            when(userServiceMock.findByToken(anyString())).thenReturn(user);
            try {
                when(reposeServiceMock.setUpReposeEnvironment(any(), any(), anyString(), anyList())).
                        thenReturn(null);
                when(configurationFactoryMock.translateConfigurationsFromUpload(any(), anyString(), any())).
                        thenReturn(new ArrayList<models.Configuration>());
            }catch(InternalServerException | NotFoundException ise){
                fail(ise.getLocalizedMessage());
            }

            Map<String, String> flashData = Collections.emptyMap();
            Map<String, Object> argData = Collections.emptyMap();
            RequestHeader header = mock(RequestHeader.class);
            Http.Request request = mock(Http.Request.class);
            when(request.getHeader("Token")).thenReturn("fake-token");
            Http.RequestBody requestBody = mock(Http.RequestBody.class);
            when(requestBody.asMultipartFormData()).thenReturn(new Http.MultipartFormData() {
                @Override
                public Map<String, String[]> asFormUrlEncoded() {
                    return null;
                }

                @Override
                public List<FilePart> getFiles() {
                    return new ArrayList<FilePart>(){
                        {
                            add(new Http.MultipartFormData.FilePart("picture","test-image.jpg",
                                    "image/jpeg",new File("test-image.jpg")));
                        }
                    };
                }
            });
            when(request.body()).thenReturn(requestBody);
            Http.Context context = new Http.Context(2L, header, request, flashData, flashData, argData);
            Http.Context.current.set(context);

            Result result = new Configuration(userServiceMock, reposeServiceMock,
                    configurationServiceMock, configurationFactoryMock).uploadReposeConfigs("1");
            assertEquals(500, result.status());
            assertEquals("{\"message\":\"unable to create repose environment.\"}", contentAsString(result));

            verify(userServiceMock).isValid(anyString());
            verify(userServiceMock, times(1)).findByToken(anyString());
            verify(request, times(1)).getHeader(anyString());
            try {
                verify(reposeServiceMock).setUpReposeEnvironment(any(), any(), anyString(), anyList());
                verify(configurationFactoryMock).translateConfigurationsFromUpload(any(), anyString(), any());
            }catch(InternalServerException | NotFoundException ise){
                fail(ise.getLocalizedMessage());
            }
        });
    }

    @Test
    public void testUploadConfigurationsInternalServerException() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {
            //set up mock user
            User user = new User();
            user.setTenant("111");
            user.setPassword("pass");
            user.setToken("fake-token");
            user.setUserid("1");
            user.setUsername("fake-user");

            IUserService userServiceMock = mock(IUserService.class);
            IReposeService reposeServiceMock = mock(IReposeService.class);
            ConfigurationService configurationServiceMock = mock(ConfigurationService.class);
            ConfigurationFactory configurationFactoryMock = mock(ConfigurationFactory.class);

            when(userServiceMock.isValid(anyString())).thenReturn(true);
            when(userServiceMock.findByToken(anyString())).thenReturn(user);
            try {
                when(configurationFactoryMock.translateConfigurationsFromUpload(any(), anyString(), any())).
                        thenReturn(new ArrayList<models.Configuration>());
                when(reposeServiceMock.setUpReposeEnvironment(any(), any(), anyString(), anyList()))
                        .thenThrow(new InternalServerException("all the things!"));
            }catch(NotFoundException | InternalServerException ise){
                fail(ise.getLocalizedMessage());
            }

            Map<String, String> flashData = Collections.emptyMap();
            Map<String, Object> argData = Collections.emptyMap();
            RequestHeader header = mock(RequestHeader.class);
            Http.Request request = mock(Http.Request.class);
            when(request.getHeader("Token")).thenReturn("fake-token");
            Http.RequestBody requestBody = mock(Http.RequestBody.class);
            when(requestBody.asMultipartFormData()).thenReturn(new Http.MultipartFormData() {
                @Override
                public Map<String, String[]> asFormUrlEncoded() {
                    return null;
                }

                @Override
                public List<FilePart> getFiles() {
                    return new ArrayList<FilePart>(){
                        {
                            add(new Http.MultipartFormData.FilePart("picture","test-image.jpg",
                                    "image/jpeg",new File("test-image.jpg")));
                        }
                    };
                }
            });
            when(request.body()).thenReturn(requestBody);
            Http.Context context = new Http.Context(2L, header, request, flashData, flashData, argData);
            Http.Context.current.set(context);

            Result result = new Configuration(userServiceMock, reposeServiceMock,
                    configurationServiceMock, configurationFactoryMock).uploadReposeConfigs("1");
            assertEquals(500, result.status());
            assertEquals(contentAsString(result), "{\"message\":\"all the things!\"}");

            verify(userServiceMock).isValid(anyString());
            verify(userServiceMock, times(1)).findByToken(anyString());
            verify(request, times(1)).getHeader(anyString());
            try {
                verify(reposeServiceMock).setUpReposeEnvironment(any(), any(), anyString(), anyList());
                verify(configurationFactoryMock).translateConfigurationsFromUpload(any(), anyString(), any());
            }catch(NotFoundException | InternalServerException ise){
                fail(ise.getLocalizedMessage());
            }
        });
    }
}