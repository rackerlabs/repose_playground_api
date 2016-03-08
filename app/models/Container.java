package models;

/**
 * Created by dimi5963 on 2/10/16.
 */
public class Container {

    private String name;
    private ContainerStatus containerStatus;
    private String message;
    private String version;
    private String id;
    private ContainerStats containerStats;


    public Container(String name, boolean isStarted, String message, String version, String id) {
        this.name = name;
        this.containerStatus = isStarted ? ContainerStatus.STARTED : ContainerStatus.STOPPED;
        this.message = message;
        this.version = version;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public ContainerStatus getContainerStatus() {
        return containerStatus;
    }

    public String getMessage() {
        return message;
    }

    public String getVersion() {
        return version;
    }

    public String getId() {
        return id;
    }

    public ContainerStats getContainerStats() {
        return containerStats;
    }

    public void setContainerStats(ContainerStats containerStats) {
        this.containerStats = containerStats;
    }
}
