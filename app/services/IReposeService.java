package services;

import com.google.inject.ImplementedBy;
import exceptions.InternalServerException;
import models.Configuration;
import models.Container;
import models.ContainerStats;
import models.ReposeEnvironmentType;
import models.User;

import java.util.List;

/**
 * Created by dimi5963 on 2/29/16.
 */
@ImplementedBy(ReposeService.class)
public interface IReposeService {

    List<Container> getReposeList(User user) throws InternalServerException;

    boolean stopReposeInstance(User user, String containerId) throws InternalServerException;

    boolean startReposeInstance(User user, String containerId) throws InternalServerException;

    ContainerStats getInstanceStats(User user, String containerId) throws InternalServerException;

    //boolean deleteReposeInstance(User user, String containerId) throws InternalServerException;

    //boolean createReposeInstance(User user, String containerId) throws InternalServerException;

    String setUpReposeEnvironment(ReposeEnvironmentType reposeEnvironmentType,
                                  User user, String versionId, List<Configuration> configs)
            throws InternalServerException;

}
