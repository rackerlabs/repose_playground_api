package clients;

import com.google.inject.ImplementedBy;
import models.Cluster;
import models.Container;
import models.User;

import java.util.List;

/**
 * Created by dimi5963 on 2/29/16.
 */
@ImplementedBy(SpotifyDockerClient.class)
public interface IDockerClient {

    List<Container> getReposeContainers(Cluster cluster, User user);

}
