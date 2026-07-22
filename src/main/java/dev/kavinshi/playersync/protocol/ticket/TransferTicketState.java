package dev.kavinshi.playersync.protocol.ticket;

/**
 * 跨端共享的转服票据状态，映射 {@code playersync_transfer_request.state} 列。
 *
 * <p>与 Forge 端 {@code SyncState}（player_data 行级状态）正交：本枚举仅描述票据状态。
 */
public enum TransferTicketState {
    REQUESTED,
    PREPARING,
    READY,
    CLAIMED,
    APPLIED,
    ABORT_REQUESTED,
    ABORTED,
    FAILED,
    EXPIRED;

    /** 终态：不再发生状态转移。 */
    public boolean isTerminal() {
        return this == APPLIED || this == ABORTED || this == FAILED || this == EXPIRED;
    }
}
