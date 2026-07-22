package dev.kavinshi.playersync.protocol;

/**
 * 跨端协议版本常量。两端协议版本必须一致才能通信。
 */
public final class ProtocolVersion {

    /** 当前实现的协议版本。 */
    public static final int CURRENT = 1;

    private ProtocolVersion() {
    }

    public static boolean isSupported(int version) {
        return version == CURRENT;
    }
}
