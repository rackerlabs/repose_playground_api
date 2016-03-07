package factories;

import exceptions.InternalServerException;
import exceptions.NotFoundException;
import models.Configuration;
import models.User;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import play.mvc.Http;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by dimi5963 on 3/5/16.
 */
public class ConfigurationFactoryImplTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void testTranslateConfigurationsSuccess() throws Exception {
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
                new ConfigurationFactoryImpl(new XmlFactoryImpl()).
                        translateConfigurations(user, "1", multipartFormData);

        //includes mac osx zipped files.
        assertEquals(14, configurationList.size());
        assertNotNull(configurationList.stream().filter(t -> "system-model.cfg.xml".equals(t.getName())));
    }

    @Test
    public void testTranslateConfigurationsSuccessVersion7() throws Exception {
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
                new ConfigurationFactoryImpl(new XmlFactoryImpl()).
                        translateConfigurations(user, "7", multipartFormData);

        //includes mac osx zipped files.
        assertEquals(14, configurationList.size());
        assertNotNull(configurationList.stream().filter(t -> "system-model.cfg.xml".equals(t.getName())));
    }

    @Test
    public void testTranslateConfigurationsInvalidVersion() throws Exception {
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
        new ConfigurationFactoryImpl(new XmlFactoryImpl()).
                translateConfigurations(user, "xxx", multipartFormData);

    }

    @Test
    public void testTranslateConfigurationsNoBody() throws Exception {
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
        new ConfigurationFactoryImpl(new XmlFactoryImpl()).
                translateConfigurations(user, "7", multipartFormData);
    }
}