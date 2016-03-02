package models;

/**
 * Created by dimi5963 on 3/2/16.
 */
public class CpuStats {

    private CpuUsage cpuUsage;
    private Long systemUsage;

    public CpuStats(CpuUsage cpuUsage, Long systemUsage) {
        this.cpuUsage = cpuUsage;
        this.systemUsage = systemUsage;
    }

    public CpuUsage getCpuUsage() {
        return cpuUsage;
    }

    public Long getSystemUsage() {
        return systemUsage;
    }
}
