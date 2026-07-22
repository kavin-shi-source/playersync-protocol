package dev.kavinshi.playersync.protocol.ticket;

/**
 * 跨端共享的玩家数据行状态，映射 {@code player_data.sync_state} 列中与转服相关的子集。
 *
 * <p>本枚举是协议层对"转服操作涉及的 player_data 状态"的权威定义。Forge 端的
 * {@code vip.fubuki.playersync.sync.SyncState} 是完整的玩家生命周期状态超集
 * （还包含 {@code INITIALIZING}/{@code ACTIVATING} 等非转服状态），本枚举仅覆盖
 * 转服双表事务会读写或校验的状态值，使协议能对 {@link TicketOperation} 的
 * player_data 前置条件做出无歧义的类型安全断言。
 *
 * <p>枚举名与数据库列值严格一致，两端通过 {@link #fromDbValue(String)} 统一解析，
 * 避免在 SQL 字符串与协议类型之间出现拼写漂移。
 */
public enum PlayerDataState {
    /** 源服持有玩家所有权，可发起 prepare 提交快照。 */
    ACTIVE,
    /** 快照已提交，仅指定目标服可 claim。 */
    TRANSFER_READY,
    /** 目标服已 claim，正在加载/应用快照。 */
    LOADING,
    /** 无所有权，可被任意服务器普通 claim（abort 源会话消失后的归宿）。 */
    READY,
    /** 数据或协议状态不确定，禁止自动 claim，需人工介入。 */
    ERROR;

    /**
     * 将数据库列值解析为枚举。
     *
     * <p>仅识别转服相关状态；若列值属于非转服生命周期状态（如 {@code INITIALIZING}），
     * 视为转服契约无法处理的状态并抛出，由调用方决定 fail-closed 策略。
     *
     * @param value {@code player_data.sync_state} 列值
     * @return 对应枚举
     * @throws IllegalArgumentException 值为 null 或不属于转服相关状态
     */
    public static PlayerDataState fromDbValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("player_data.sync_state is null");
        }
        return PlayerDataState.valueOf(value);
    }
}
