package factories;

import com.google.inject.ImplementedBy;

/**
 * Created by dimi5963 on 2/29/16.
 */
@ImplementedBy(ClusterFactory.class)
public interface IClusterFactory {

    String getClusterName();

    String getCarinaEndpoint();

    String getCarinaZipUrl(String username, String clusterName);

    String getCarinaUserUrl(String username);

    String getCarinaClusterUrl(String username, String clusterName);

}
