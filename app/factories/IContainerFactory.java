package factories;

import com.google.inject.ImplementedBy;
import models.Container;
import models.User;

import java.util.List;

/**
 * Created by dimi5963 on 2/29/16.
 */
@ImplementedBy(SpotifyContainerFactory.class)
public interface IContainerFactory<E> {

    /***
     * Translate containers from Docker containers to Container models
     * @param dockerContainerList docker containers
     * @param user User model
     * @return List of Container models
     */
    List<Container> translateContainers(List<E> dockerContainerList, User user);

}
