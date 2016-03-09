package factories;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.ImplementedBy;
import exceptions.InternalServerException;
import exceptions.NotFoundException;
import models.Configuration;
import models.User;
import play.mvc.Http;

import java.util.List;
import java.util.Set;

/**
 * Created by dimi5963 on 3/2/16.
 */
@ImplementedBy(ConfigurationFactoryImpl.class)
public interface ConfigurationFactory {

    List<Configuration> translateConfigurationsFromUpload(User user, String reposeVersion, Http.MultipartFormData body)
            throws NotFoundException, InternalServerException;

    List<Configuration> translateConfigurationsFromJson(User user, String reposeVersion, JsonNode body)
            throws NotFoundException, InternalServerException;

    String updateSystemModelXml(User user, String versionId, String systemModelContent)
            throws InternalServerException;

    String generateContainerXml(int majorVersion)
            throws InternalServerException;

    String generateLoggingXml(int majorVersion)
            throws InternalServerException;

    String generateSystemModelXml(Set<String> filterNames, int majorVersion, User user, String versionId)
            throws InternalServerException;
}
