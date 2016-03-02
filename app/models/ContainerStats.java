package models;

/**
 * Created by dimi5963 on 3/2/16.
 */
public class ContainerStats {

    private CpuStats cpuStats;
    private MemoryStats memoryStats;
    private NetworkStats networkStats;
    private CpuStats preCpuStats;

    public ContainerStats(CpuStats cpuStats, MemoryStats memoryStats, NetworkStats networkStats, CpuStats preCpuStats) {
        this.cpuStats = cpuStats;
        this.memoryStats = memoryStats;
        this.networkStats = networkStats;
        this.preCpuStats = preCpuStats;
    }

    public CpuStats getCpuStats() {
        return cpuStats;
    }

    public MemoryStats getMemoryStats() {
        return memoryStats;
    }

    public NetworkStats getNetworkStats() {
        return networkStats;
    }

    public CpuStats getPreCpuStats() {
        return preCpuStats;
    }
}
