package dev.kavinshi.playersync.protocol.ticket;

import java.util.EnumSet;
import java.util.Set;

/**
 * 转服票据的命名操作。每个操作绑定一组前置条件（合法请求状态 + 必要 player_data 状态）
 * 与目标请求状态，是跨端共享的转服状态机权威定义。
 *
 * <p>与 {@link TransferTicketTransition} 的区别：后者只校验"请求状态 A → 请求状态 B"
 * 这对单表状态是否合法；本枚举描述的是"执行某个命名操作需满足的双表前置条件"，
 * 使协议成为双表事务的真正权威——而不仅校验单表状态对。
 *
 * <p>操作语义从 Forge/Velocity 现有 SQL 事务的 {@code WHERE state = ...} 条件与
 * 双表配对校验中提取，代表转服票据的合法操作集合。SQL 的 {@code WHERE} 仍是
 * 并发安全的最终防线；本枚举在应用层负责捕获"在错误状态下发起错误操作"的逻辑错误。
 *
 * <p>操作到状态的映射：
 * <pre>
 *   SOURCE_BEGIN_PREPARE    : REQUESTED            → PREPARING          (仅 request 表)
 *   SOURCE_COMMIT_READY     : PREPARING            → READY              (player ACTIVE → TRANSFER_READY)
 *   TARGET_CLAIM            : READY                → CLAIMED            (player TRANSFER_READY → LOADING)
 *   TARGET_APPLY            : CLAIMED              → APPLIED            (player LOADING → ACTIVE)
 *   REQUEST_ABORT           : REQUESTED/PREPARING/ → ABORT_REQUESTED    (仅 request 表，由源服 poller 收敛)
 *                            READY/CLAIMED
 *   SOURCE_SETTLE_ABORT     : ABORT_REQUESTED      → ABORTED            (player ACTIVE/TRANSFER_READY → 恢复)
 *   RECOVER_EXPIRED_PREPARE : PREPARING            → FAILED             (player ACTIVE/TRANSFER_READY → 恢复)
 *   RECOVER_EXPIRED_CLAIM   : CLAIMED              → FAILED             (player LOADING → ERROR)
 *   MARK_FAILED             : REQUESTED/PREPARING/ → FAILED             (player_data 处理由具体路径决定)
 *                            READY/CLAIMED/
 *                            ABORT_REQUESTED
 * </pre>
 */
public enum TicketOperation {
    /** 源服原子 claim 请求：REQUESTED → PREPARING。仅更新 request 行，player_data 在此不校验。 */
    SOURCE_BEGIN_PREPARE(
            EnumSet.of(TransferTicketState.REQUESTED),
            EnumSet.noneOf(PlayerDataState.class),
            TransferTicketState.PREPARING),

    /** 源服提交快照：PREPARING → READY，player_data ACTIVE → TRANSFER_READY（双表事务）。 */
    SOURCE_COMMIT_READY(
            EnumSet.of(TransferTicketState.PREPARING),
            EnumSet.of(PlayerDataState.ACTIVE),
            TransferTicketState.READY),

    /** 目标服认领票据：READY → CLAIMED，player_data TRANSFER_READY → LOADING（双表事务）。 */
    TARGET_CLAIM(
            EnumSet.of(TransferTicketState.READY),
            EnumSet.of(PlayerDataState.TRANSFER_READY),
            TransferTicketState.CLAIMED),

    /** 目标服应用快照完成：CLAIMED → APPLIED，player_data LOADING → ACTIVE（双表事务，终态）。 */
    TARGET_APPLY(
            EnumSet.of(TransferTicketState.CLAIMED),
            EnumSet.of(PlayerDataState.LOADING),
            TransferTicketState.APPLIED),

    /**
     * 请求中止：活跃状态 → ABORT_REQUESTED。仅更新 request 行，player_data 不约束
     * （由源服 poller 在 {@link #SOURCE_SETTLE_ABORT} 中恢复）。
     */
    REQUEST_ABORT(
            EnumSet.of(TransferTicketState.REQUESTED, TransferTicketState.PREPARING,
                    TransferTicketState.READY, TransferTicketState.CLAIMED),
            EnumSet.noneOf(PlayerDataState.class),
            TransferTicketState.ABORT_REQUESTED),

    /**
     * 源服收敛中止：ABORT_REQUESTED → ABORTED。player_data 可能是 ACTIVE（提交前未触碰）
     * 或 TRANSFER_READY（提交后需恢复为 ACTIVE/READY）。
     */
    SOURCE_SETTLE_ABORT(
            EnumSet.of(TransferTicketState.ABORT_REQUESTED),
            EnumSet.of(PlayerDataState.ACTIVE, PlayerDataState.TRANSFER_READY),
            TransferTicketState.ABORTED),

    /**
     * 恢复过期 PREPARING：PREPARING → FAILED。player_data 可能是 ACTIVE（快照未提交）
     * 或 TRANSFER_READY（快照已提交，需恢复），由具体恢复路径决定。
     */
    RECOVER_EXPIRED_PREPARE(
            EnumSet.of(TransferTicketState.PREPARING),
            EnumSet.of(PlayerDataState.ACTIVE, PlayerDataState.TRANSFER_READY),
            TransferTicketState.FAILED),

    /** 恢复过期 CLAIMED：CLAIMED → FAILED，player_data LOADING → ERROR。 */
    RECOVER_EXPIRED_CLAIM(
            EnumSet.of(TransferTicketState.CLAIMED),
            EnumSet.of(PlayerDataState.LOADING),
            TransferTicketState.FAILED),

    /**
     * 通用失败化：活跃/中止请求状态 → FAILED。player_data 不约束，因各失败路径对
     * player_data 的处理各异（REQUESTED 不触碰、PREPARING 保持 ACTIVE、CLAIMED 设 ERROR）。
     */
    MARK_FAILED(
            EnumSet.of(TransferTicketState.REQUESTED, TransferTicketState.PREPARING,
                    TransferTicketState.READY, TransferTicketState.CLAIMED,
                    TransferTicketState.ABORT_REQUESTED),
            EnumSet.noneOf(PlayerDataState.class),
            TransferTicketState.FAILED);

    private final Set<TransferTicketState> allowedRequestStates;
    private final Set<PlayerDataState> requiredPlayerStates;
    private final TransferTicketState targetRequestState;

    TicketOperation(Set<TransferTicketState> allowedRequestStates,
                    Set<PlayerDataState> requiredPlayerStates,
                    TransferTicketState targetRequestState) {
        // 不可变快照：防止外部修改操作的前置条件定义
        this.allowedRequestStates = Set.copyOf(allowedRequestStates);
        this.requiredPlayerStates = Set.copyOf(requiredPlayerStates);
        this.targetRequestState = targetRequestState;
    }

    /** 允许发起本操作的请求状态集合。 */
    public Set<TransferTicketState> allowedRequestStates() {
        return allowedRequestStates;
    }

    /** 本操作要求的 player_data 状态集合；空集表示不约束 player_data。 */
    public Set<PlayerDataState> requiredPlayerStates() {
        return requiredPlayerStates;
    }

    /** 操作完成后的目标请求状态。 */
    public TransferTicketState targetRequestState() {
        return targetRequestState;
    }

    /** 操作是否终结票据（目标状态为终态）。 */
    public boolean isTerminal() {
        return targetRequestState.isTerminal();
    }

    /** 当前请求状态是否允许发起本操作（仅校验请求侧，不校验 player_data）。 */
    public boolean isLegalFrom(TransferTicketState requestState) {
        return allowedRequestStates.contains(requestState);
    }
}
