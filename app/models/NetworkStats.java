package models;

/**
 * Created by dimi5963 on 3/2/16.
 */
public class NetworkStats {
    private long rxBytes;
    private long rxPackets;
    private long rxPacketsDropped;
    private long rxPacketsErrored;

    private long txBytes;
    private long txPackets;
    private long txPacketsDropped;
    private long txPacketsErrored;

    public NetworkStats(long rxBytes, long rxPackets, long rxPacketsDropped,
                        long rxPacketsErrored, long txBytes, long txPackets,
                        long txPacketsDropped, long txPacketsErrored) {
        this.rxBytes = rxBytes;
        this.rxPackets = rxPackets;
        this.rxPacketsDropped = rxPacketsDropped;
        this.rxPacketsErrored = rxPacketsErrored;
        this.txBytes = txBytes;
        this.txPackets = txPackets;
        this.txPacketsDropped = txPacketsDropped;
        this.txPacketsErrored = txPacketsErrored;
    }

    public long getRxBytes() {
        return rxBytes;
    }

    public long getRxPackets() {
        return rxPackets;
    }

    public long getRxPacketsDropped() {
        return rxPacketsDropped;
    }

    public long getRxPacketsErrored() {
        return rxPacketsErrored;
    }

    public long getTxBytes() {
        return txBytes;
    }

    public long getTxPackets() {
        return txPackets;
    }

    public long getTxPacketsDropped() {
        return txPacketsDropped;
    }

    public long getTxPacketsErrored() {
        return txPacketsErrored;
    }
}
