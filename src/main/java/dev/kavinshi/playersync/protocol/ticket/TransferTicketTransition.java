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
     */
    public static TransitionResult validate(TransferTicketState from, TransferTicketState to) {
        if (from.isTerminal()) {
            return TransitionResult.DENIED_ALREADY_TERMINAL;
        }
        return TransitionResult.DENIED_INVALID_TRANSITION;
    }
}
