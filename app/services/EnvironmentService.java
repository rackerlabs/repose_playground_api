package services;

import com.google.inject.ImplementedBy;
import exceptions.InternalServerException;
import models.Cluster;
import models.Configuration;
import models.User;

import java.util.List;

/**
 * Created by dimi5963 on 3/2/16.
 */
@ImplementedBy(EnvironmentServiceImpl.class)
public interface EnvironmentService {

    String generatedOriginEnvironment(Cluster cluster, String versionId, User user,
                                      List<Configuration> configurationList) throws InternalServerException;


}
