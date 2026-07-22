package dev.kavinshi.playersync.protocol.time;

/**
 * 类型安全的 epoch millis 时间点包装，避免 long 与 duration 混用。
 */
public record ProtocolPointInTime(long millis) implements Comparable<ProtocolPointInTime> {

    @Override
    public int compareTo(ProtocolPointInTime o) {
        return Long.compare(millis, o.millis);
    }

    public boolean isBefore(ProtocolPointInTime o) {
        return millis < o.millis;
    }

    public boolean isAfter(ProtocolPointInTime o) {
        return millis > o.millis;
    }
}
