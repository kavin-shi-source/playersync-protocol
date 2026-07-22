package dev.kavinshi.playersync.protocol.ticket;

import java.util.Set;

/**
 * 校验某个 {@link TicketOperation} 在给定双表状态下是否合法的纯函数契约。
 *
 * <p>无副作用、无 IO，可跨端共享。调用方在执行状态转移前用本契约断言操作合法；
 * SQL 的 {@code WHERE} 条件仍是并发安全的最终防线，本契约在应用层负责捕获
 * "在错误状态下发起错误操作"的逻辑错误。
 *
 * <p>校验规则：
 * <ol>
 *   <li>当前请求状态必须属于操作的 {@link TicketOperation#allowedRequestStates()}，
 *       否则 {@link OperationResult#DENIED_REQUEST_STATE_NOT_ALLOWED}。</li>
 *   <li>若操作定义了非空 {@link TicketOperation#requiredPlayerStates()}，
 *       则传入的 player_data 状态必须非 null 且属于该集合，
 *       否则 {@link OperationResult#DENIED_PLAYER_STATE_NOT_ALLOWED}。</li>
 * </ol>
 *
 * <p>{@code playerState} 为 {@code null} 表示调用方未读取或该操作不约束 player_data；
 * 仅当操作定义了非空 requiredPlayerStates 时才校验。对于已用 {@code FOR UPDATE}
 * 锁定双行并确认状态的事务点，应使用 {@link #requireLegal} 强校验：若抛出
 * {@link IllegalStateException}，说明代码路径与协议定义存在漂移，需立即暴露。
 */
public final class TicketOperationContract {

    private TicketOperationContract() {
    }

    /** 契约校验结果。 */
    public enum OperationResult {
        /** 操作合法，可执行。 */
        ALLOWED,
        /** 当前请求状态不允许发起本操作。 */
        DENIED_REQUEST_STATE_NOT_ALLOWED,
        /** 操作要求 player_data 处于特定状态，但实际不符或缺失。 */
        DENIED_PLAYER_STATE_NOT_ALLOWED
    }

    /**
     * 校验操作在给定双表状态下是否合法。
     *
     * @param operation          待校验操作
     * @param currentRequestState 当前请求状态（不可为 null）
     * @param currentPlayerState 当前 player_data 状态，可为 null（表示未读取/不约束）
     * @return 校验结果
     */
    public static OperationResult validate(TicketOperation operation,
                                           TransferTicketState currentRequestState,
                                           PlayerDataState currentPlayerState) {
        if (!operation.allowedRequestStates().contains(currentRequestState)) {
            return OperationResult.DENIED_REQUEST_STATE_NOT_ALLOWED;
        }
        Set<PlayerDataState> required = operation.requiredPlayerStates();
        if (!required.isEmpty()
                && (currentPlayerState == null || !required.contains(currentPlayerState))) {
            return OperationResult.DENIED_PLAYER_STATE_NOT_ALLOWED;
        }
        return OperationResult.ALLOWED;
    }

    /**
     * 仅校验请求侧的简化重载，等价于 {@code validate(operation, currentRequestState, null)}。
     * 用于不约束 player_data 的操作，或调用方尚未读取 player_data 的请求侧前置校验。
     *
     * @param operation          待校验操作
     * @param currentRequestState 当前请求状态（不可为 null）
     * @return 校验结果
     */
    public static OperationResult validate(TicketOperation operation,
                                           TransferTicketState currentRequestState) {
        return validate(operation, currentRequestState, null);
    }

    /**
     * 断言操作合法，否则抛出 {@link IllegalStateException}。
     *
     * <p>用于在已锁定行、已确认状态的事务点强校验协议一致性。异常即代表代码路径
     * 与协议定义漂移，应立即暴露而非静默写入；SQL 的 {@code WHERE} 仍处理并发竞争，
     * 本断言只负责捕获确定性的逻辑错误。
     *
     * @param operation          待校验操作
     * @param currentRequestState 当前请求状态
     * @param currentPlayerState 当前 player_data 状态，可为 null
     * @throws IllegalStateException 操作在给定状态下不合法
     */
    public static void requireLegal(TicketOperation operation,
                                    TransferTicketState currentRequestState,
                                    PlayerDataState currentPlayerState) {
        OperationResult result = validate(operation, currentRequestState, currentPlayerState);
        if (result != OperationResult.ALLOWED) {
            throw new IllegalStateException(
                    "Illegal ticket operation " + operation
                            + ": requestState=" + currentRequestState
                            + ", playerState=" + currentPlayerState
                            + " -> " + result);
        }
    }

    /**
     * 仅请求侧的断言重载。
     *
     * @param operation          待校验操作
     * @param currentRequestState 当前请求状态
     * @throws IllegalStateException 操作在给定请求状态下不合法
     */
    public static void requireLegal(TicketOperation operation,
                                    TransferTicketState currentRequestState) {
        requireLegal(operation, currentRequestState, null);
    }
}
