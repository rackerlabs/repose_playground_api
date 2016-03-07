package services;

import com.google.inject.ImplementedBy;
import exceptions.InternalServerException;
import models.Configuration;
import models.User;

import java.util.List;

/**
 * Created by dimi5963 on 3/2/16.
 */
@ImplementedBy(ConfigurationServiceImpl.class)
public interface ConfigurationService {

    List<Configuration> getConfigurationsForInstance(User user, String containerId) throws InternalServerException;
}
