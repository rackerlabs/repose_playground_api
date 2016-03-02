package models;

/**
 * Created by dimi5963 on 2/29/16.
 */
public enum ContainerStatus {
    STARTED("started"),
    STOPPED("stopped");

    private String status;

    private ContainerStatus(String status) {
        this.status = status;
    }

}

