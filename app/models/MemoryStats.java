package models;

/**
 * Created by dimi5963 on 3/2/16.
 */
public class MemoryStats {
    private Long failCount;
    private Long limit;
    private Long maxUsage;
    private Long currentUsage;

    public MemoryStats(Long currentUsage, Long maxUsage, Long limit, Long failCount) {
        this.currentUsage = currentUsage;
        this.maxUsage = maxUsage;
        this.limit = limit;
        this.failCount = failCount;
    }

    public Long getFailCount() {
        return failCount;
    }

    public Long getLimit() {
        return limit;
    }

    public Long getMaxUsage() {
        return maxUsage;
    }

    public Long getCurrentUsage() {
        return currentUsage;
    }
}
