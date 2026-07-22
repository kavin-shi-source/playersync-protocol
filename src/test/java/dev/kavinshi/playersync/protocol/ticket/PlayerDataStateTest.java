package dev.kavinshi.playersync.protocol.ticket;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlayerDataStateTest {

    @Test
    void allFiveTransferRelevantStatesExist() {
        assertThat(PlayerDataState.values())
                .containsExactly(PlayerDataState.ACTIVE, PlayerDataState.TRANSFER_READY,
                        PlayerDataState.LOADING, PlayerDataState.READY, PlayerDataState.ERROR);
    }

    @Test
    void fromDbValueParsesKnownStates() {
        assertThat(PlayerDataState.fromDbValue("ACTIVE")).isEqualTo(PlayerDataState.ACTIVE);
        assertThat(PlayerDataState.fromDbValue("TRANSFER_READY")).isEqualTo(PlayerDataState.TRANSFER_READY);
        assertThat(PlayerDataState.fromDbValue("LOADING")).isEqualTo(PlayerDataState.LOADING);
        assertThat(PlayerDataState.fromDbValue("READY")).isEqualTo(PlayerDataState.READY);
        assertThat(PlayerDataState.fromDbValue("ERROR")).isEqualTo(PlayerDataState.ERROR);
    }

    @Test
    void fromDbValueRejectsNull() {
        assertThatThrownBy(() -> PlayerDataState.fromDbValue(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fromDbValueRejectsNonTransferLifecycleStates() {
        // 非转服生命周期状态不属于协议契约处理范围
        assertThatThrownBy(() -> PlayerDataState.fromDbValue("INITIALIZING"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PlayerDataState.fromDbValue("ACTIVATING"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PlayerDataState.fromDbValue("GARBAGE"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
