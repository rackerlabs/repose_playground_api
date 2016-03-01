package factories;

/**
 * Created by dimi5963 on 2/29/16.
 */
public class ClusterFactory implements IClusterFactory {

    @Override
    public String getClusterName() {
        return play.Play.application().configuration().getString("user.cluster.name");
    }

    @Override
    public String getCarinaEndpoint() {
        return play.Play.application().configuration().getString("carina.endpoint");
    }

    @Override
    public String getCarinaZipUrl(String username, String clusterName) {
        return getCarinaEndpoint() + username + "/" + clusterName + "/zip";
    }

    @Override
    public String getCarinaUserUrl(String username) {
        return getCarinaEndpoint() + username;
    }

    @Override
    public String getCarinaClusterUrl(String username, String clusterName) {
        return getCarinaEndpoint() + username + "/" + clusterName;
    }
}
