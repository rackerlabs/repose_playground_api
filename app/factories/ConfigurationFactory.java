package factories;

import com.google.inject.ImplementedBy;
import exceptions.NotFoundException;
import models.Configuration;
import models.User;
import play.mvc.Http;

import java.util.List;

/**
 * Created by dimi5963 on 3/2/16.
 */
@ImplementedBy(ConfigurationFactoryImpl.class)
public interface ConfigurationFactory {

    List<Configuration> translateConfigurations(User user, String reposeVersion, Http.MultipartFormData body)
            throws NotFoundException;

    String updateSystemModelXml(User user, String versionId, String systemModelContent);

    String generateContainerXml(int majorVersion);

    String generateLoggingXml(int majorVersion);
}
