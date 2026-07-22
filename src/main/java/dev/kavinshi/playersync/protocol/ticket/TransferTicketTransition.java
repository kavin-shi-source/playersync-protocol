package dev.kavinshi.playersync.protocol.ticket;

/**
 * 票据状态转换的纯函数验证。无副作用，可跨端共享。
 */
public final class TransferTicketTransition {

    private TransferTicketTransition() {
    }

    public enum TransitionResult {
        ALLOWED,
        DENIED_ALREADY_TERMINAL,
        DENIED_INVALID_TRANSITION
    }

    /**
     * 验证从 {@code from} 到 {@code to} 的状态转换是否合法。
     *
     * <p>TODO 批次 B/C：补全完整转换表。
     */
    public static TransitionResult validate(TransferTicketState from, TransferTicketState to) {
        if (from.isTerminal()) {
            return TransitionResult.DENIED_ALREADY_TERMINAL;
        }
        // TODO 批次 B/C：补全完整转换表
        return TransitionResult.DENIED_INVALID_TRANSITION;
    }
}
