package models;

import com.google.common.collect.ImmutableList;

/**
 * Created by dimi5963 on 3/2/16.
 */
public class CpuUsage {
    private ImmutableList<Long> perCpu;
    private Long total;
    private Long inKernelMode;
    private Long inUserMode;

    public CpuUsage(ImmutableList<Long> perCpu, Long total, Long inKernelMode, Long inUserMode) {
        this.perCpu = perCpu;
        this.total = total;
        this.inKernelMode = inKernelMode;
        this.inUserMode = inUserMode;
    }

    public ImmutableList<Long> getPerCpu() {
        return perCpu;
    }

    public Long getTotal() {
        return total;
    }

    public Long getInKernelMode() {
        return inKernelMode;
    }

    public Long getInUserMode() {
        return inUserMode;
    }
}
