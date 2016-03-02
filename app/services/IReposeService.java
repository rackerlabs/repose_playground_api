package services;

import com.google.inject.ImplementedBy;
import exceptions.InternalServerException;
import models.Container;
import models.ContainerStats;
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

}
