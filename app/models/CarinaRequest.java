package models;

/**
 * Created by dimi5963 on 8/29/15.
 */
public class CarinaRequest {
    private String cluster_name;
    private boolean autoscale;
    private String username;

    public boolean isAutoscale() {
        return autoscale;
    }

    public void setAutoscale(boolean autoScale) {
        this.autoscale = autoScale;
    }

    public String getUsername(){
        return username;
    }

    public void setUsername(String username){
        this.username = username;
    }


    public String getCluster_name() {
        return cluster_name;
    }

    public void setCluster_name(String clusterName) {
        this.cluster_name = clusterName;
    }

    public CarinaRequest(String clusterName, boolean autoScale, String username){
        this.cluster_name = clusterName;
        this.autoscale = autoScale;
        this.username = username;
    }
}
