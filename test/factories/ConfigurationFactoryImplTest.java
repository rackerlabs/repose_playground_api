package factories;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import exceptions.InternalServerException;
import exceptions.NotFoundException;
import models.Configuration;
import models.Filter;
import models.User;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import play.libs.Json;
import play.mvc.Http;
import repositories.FilterRepository;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by dimi5963 on 3/5/16.
 */
public class ConfigurationFactoryImplTest {

    private final FilterRepository filterRepository = mock(FilterRepository.class);

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void testTranslateConfigurationsFromUploadSuccess() throws Exception {
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");

        Http.MultipartFormData multipartFormData = new Http.MultipartFormData() {
            @Override
            public Map<String, String[]> asFormUrlEncoded() {
                return null;
            }

            @Override
            public List<FilePart> getFiles() {
                return new ArrayList<FilePart>(){
                    {
                        add(new Http.MultipartFormData.FilePart("repose","repose.zip",
                                "application/gzip",new File("test/test_data/repose.zip")));
                    }
                };
            }
        };

        List<Configuration> configurationList =
                new ConfigurationFactoryImpl(new XmlFactoryImpl(), filterRepository).
                        translateConfigurationsFromUpload(user, "1", multipartFormData);

        //includes mac osx zipped files.
        assertEquals(14, configurationList.size());
        assertNotNull(configurationList.stream().filter(t -> "system-model.cfg.xml".equals(t.getName())));
    }

    @Test
    public void testTranslateConfigurationsFromUploadSuccessVersion7() throws Exception {
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");

        Http.MultipartFormData multipartFormData = new Http.MultipartFormData() {
            @Override
            public Map<String, String[]> asFormUrlEncoded() {
                return null;
            }

            @Override
            public List<FilePart> getFiles() {
                return new ArrayList<FilePart>(){
                    {
                        add(new Http.MultipartFormData.FilePart("repose","repose.zip",
                                "application/gzip",new File("test/test_data/repose.zip")));
                    }
                };
            }
        };

        List<Configuration> configurationList =
                new ConfigurationFactoryImpl(new XmlFactoryImpl(), filterRepository).
                        translateConfigurationsFromUpload(user, "7", multipartFormData);

        //includes mac osx zipped files.
        assertEquals(14, configurationList.size());
        assertNotNull(configurationList.stream().filter(t -> "system-model.cfg.xml".equals(t.getName())));
    }

    @Test
    public void testTranslateConfigurationsFromUploadInvalidVersion() throws Exception {
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");

        Http.MultipartFormData multipartFormData = new Http.MultipartFormData() {
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
        };

        exception.expect(InternalServerException.class);
        exception.expectMessage("Invalid version specified.");
        new ConfigurationFactoryImpl(new XmlFactoryImpl(), filterRepository).
                translateConfigurationsFromUpload(user, "xxx", multipartFormData);

    }

    @Test
    public void testTranslateConfigurationsFromUploadNoBody() throws Exception {
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");

        Http.MultipartFormData multipartFormData = new Http.MultipartFormData() {
            @Override
            public Map<String, String[]> asFormUrlEncoded() {
                return null;
            }

            @Override
            public List<FilePart> getFiles() {
                return null;
            }
        };

        exception.expect(NotFoundException.class);
        exception.expectMessage("No zip files");
        new ConfigurationFactoryImpl(new XmlFactoryImpl(), filterRepository).
                translateConfigurationsFromUpload(user, "7", multipartFormData);
    }

    @Test
    public void testTranslateConfigurationsFromJsonSuccess() throws Exception {
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");

        ArrayNode requestNode = Json.newArray();
        ObjectNode filterNode = Json.newObject();
        filterNode.put("filter","add-header");
        filterNode.put("name","add-headers.response.header.quality[0]");
        filterNode.put("value","0.9");
        filterNode.put("type","attribute");
        requestNode.add(filterNode);
        filterNode = Json.newObject();
        filterNode.put("filter","ip-user");
        filterNode.put("name","ip-user.response.cidr-ip.quality[0]");
        filterNode.put("value","0.9");
        filterNode.put("type","attribute");
        requestNode.add(filterNode);

        when(filterRepository.findByName(anyString()))
                .thenReturn(new Filter("add-header"))
                .thenReturn(new Filter("ip-user"));


        List<Configuration> configurationList =
                new ConfigurationFactoryImpl(new XmlFactoryImpl(), filterRepository).
                        translateConfigurationsFromJson(user, "1.2.3", requestNode);

        assertEquals(5, configurationList.size());
        assertNotNull(configurationList.stream().filter(t -> "system-model.cfg.xml".equals(t.getName())));
        String systemModelXml = configurationList.stream().
                filter(t -> "system-model.cfg.xml".equals(t.getName())).findFirst().get().getXml();
        assertTrue(systemModelXml.contains("add-header"));
        assertTrue(systemModelXml.contains("ip-user"));
    }

    @Test
    public void testTranslateConfigurationsFromJsonSuccessVersion7() throws Exception {
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");

        ArrayNode requestNode = Json.newArray();
        ObjectNode filterNode = Json.newObject();
        filterNode.put("filter","add-header");
        filterNode.put("name","add-headers.response.header.quality[0]");
        filterNode.put("value","0.9");
        filterNode.put("type","attribute");
        requestNode.add(filterNode);
        filterNode = Json.newObject();
        filterNode.put("filter","ip-user");
        filterNode.put("name","ip-user.response.cidr-ip.quality[0]");
        filterNode.put("value","0.9");
        filterNode.put("type","attribute");
        requestNode.add(filterNode);

        when(filterRepository.findByName(anyString()))
                .thenReturn(new Filter("add-header"))
                .thenReturn(new Filter("ip-user"));


        List<Configuration> configurationList =
                new ConfigurationFactoryImpl(new XmlFactoryImpl(), filterRepository).
                        translateConfigurationsFromJson(user, "7.2.3", requestNode);

        assertEquals(5, configurationList.size());
        assertNotNull(configurationList.stream().filter(t -> "system-model.cfg.xml".equals(t.getName())));
        String systemModelXml = configurationList.stream().
                filter(t -> "system-model.cfg.xml".equals(t.getName())).findFirst().get().getXml();
        assertTrue(systemModelXml.contains("add-header"));
        assertTrue(systemModelXml.contains("ip-user"));
    }

    @Test
    public void testTranslateConfigurationsFromJsonInvalidVersion() throws Exception {
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");

        ArrayNode requestNode = Json.newArray();
        ObjectNode filterNode = Json.newObject();
        filterNode.put("filter","add-header");
        filterNode.put("name","add-headers.response.header.quality[0]");
        filterNode.put("value","0.9");
        filterNode.put("type","attribute");
        requestNode.add(filterNode);
        filterNode = Json.newObject();
        filterNode.put("filter","ip-user");
        filterNode.put("name","ip-user.response.cidr-ip.quality[0]");
        filterNode.put("value","0.9");
        filterNode.put("type","attribute");
        requestNode.add(filterNode);

        when(filterRepository.findByName(anyString()))
                .thenReturn(new Filter("add-header"))
                .thenReturn(new Filter("ip-user"));



        exception.expect(InternalServerException.class);
        exception.expectMessage("Invalid version specified.");
        new ConfigurationFactoryImpl(new XmlFactoryImpl(), filterRepository).
                translateConfigurationsFromJson(user, "xxxx", requestNode);

    }

    @Test
    public void testTranslateConfigurationsFromJsonNoBody() throws Exception {
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");

        when(filterRepository.findByName(anyString()))
                .thenReturn(new Filter("add-header"))
                .thenReturn(new Filter("ip-user"));

        List<Configuration> configurationList =
                new ConfigurationFactoryImpl(new XmlFactoryImpl(), filterRepository).
                        translateConfigurationsFromJson(user, "1.2.3", null);

        assertEquals(3, configurationList.size());
        assertNotNull(configurationList.stream().filter(t -> "system-model.cfg.xml".equals(t.getName())));
        String systemModelXml = configurationList.stream().
                filter(t -> "system-model.cfg.xml".equals(t.getName())).findFirst().get().getXml();
        assertFalse(systemModelXml.contains("add-header"));
    }

    @Test
    public void testTranslateConfigurationsFromJsonNoFilters() throws Exception {
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");

        when(filterRepository.findByName(anyString()))
                .thenReturn(new Filter("add-header"))
                .thenReturn(new Filter("ip-user"));


        List<Configuration> configurationList =
                new ConfigurationFactoryImpl(new XmlFactoryImpl(), filterRepository).
                        translateConfigurationsFromJson(user, "1.2.3", Json.newArray());

        assertEquals(3, configurationList.size());
        assertNotNull(configurationList.stream().filter(t -> "system-model.cfg.xml".equals(t.getName())));
        String systemModelXml = configurationList.stream().
                filter(t -> "system-model.cfg.xml".equals(t.getName())).findFirst().get().getXml();
        assertFalse(systemModelXml.contains("add-header"));
    }
}