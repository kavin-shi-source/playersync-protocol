package dev.kavinshi.playersync.protocol.ticket;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 票据状态转换的纯函数验证。无副作用，可跨端共享。
 *
 * <p>转换矩阵从现有 SQL 事务（{@code WHERE state = ...}）提取，
 * 代表转服票据的合法状态机。
 */
public final class TransferTicketTransition {

    private TransferTicketTransition() {
    }

    public enum TransitionResult {
        ALLOWED,
        DENIED_ALREADY_TERMINAL,
        DENIED_INVALID_TRANSITION
    }

    private static final Map<TransferTicketState, Set<TransferTicketState>> ALLOWED_TRANSITIONS = new EnumMap<>(TransferTicketState.class);

    static {
        // REQUESTED: source prepares snapshot, or abort before preparing
        ALLOWED_TRANSITIONS.put(TransferTicketState.REQUESTED, EnumSet.of(
                TransferTicketState.PREPARING,
                TransferTicketState.ABORT_REQUESTED,
                TransferTicketState.FAILED,
                TransferTicketState.EXPIRED
        ));

        // PREPARING: source commits snapshot → READY, or abort
        ALLOWED_TRANSITIONS.put(TransferTicketState.PREPARING, EnumSet.of(
                TransferTicketState.READY,
                TransferTicketState.ABORT_REQUESTED,
                TransferTicketState.FAILED,
                TransferTicketState.EXPIRED
        ));

        // READY: target claims, or abort
        ALLOWED_TRANSITIONS.put(TransferTicketState.READY, EnumSet.of(
                TransferTicketState.CLAIMED,
                TransferTicketState.ABORT_REQUESTED,
                TransferTicketState.ABORTED,
                TransferTicketState.FAILED,
                TransferTicketState.EXPIRED
        ));

        // CLAIMED: target applies, or fails
        ALLOWED_TRANSITIONS.put(TransferTicketState.CLAIMED, EnumSet.of(
                TransferTicketState.APPLIED,
                TransferTicketState.ABORT_REQUESTED,
                TransferTicketState.FAILED,
                TransferTicketState.EXPIRED
        ));

        // ABORT_REQUESTED: converges to ABORTED or FAILED
        ALLOWED_TRANSITIONS.put(TransferTicketState.ABORT_REQUESTED, EnumSet.of(
                TransferTicketState.ABORTED,
                TransferTicketState.FAILED
        ));
    }

    /**
     * 验证从 {@code from} 到 {@code to} 的状态转换是否合法。
     *
     * @param from 当前状态
     * @param to   目标状态
     * @return 转换结果
     */
    public static TransitionResult validate(TransferTicketState from, TransferTicketState to) {
        if (from.isTerminal()) {
            return TransitionResult.DENIED_ALREADY_TERMINAL;
        }
        Set<TransferTicketState> allowed = ALLOWED_TRANSITIONS.get(from);
        if (allowed != null && allowed.contains(to)) {
            return TransitionResult.ALLOWED;
        }
        return TransitionResult.DENIED_INVALID_TRANSITION;
    }
}
