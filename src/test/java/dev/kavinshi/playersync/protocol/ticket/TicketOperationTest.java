package dev.kavinshi.playersync.protocol.ticket;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static dev.kavinshi.playersync.protocol.ticket.TicketOperationContract.OperationResult.*;
import static dev.kavinshi.playersync.protocol.ticket.TicketOperation.*;
import static dev.kavinshi.playersync.protocol.ticket.TransferTicketState.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketOperationTest {

    @Test
    void allNineOperationsExist() {
        assertThat(TicketOperation.values()).hasSize(9);
    }

    @Test
    void targetStatesAreCorrect() {
        assertThat(SOURCE_BEGIN_PREPARE.targetRequestState()).isEqualTo(PREPARING);
        assertThat(SOURCE_COMMIT_READY.targetRequestState()).isEqualTo(READY);
        assertThat(TARGET_CLAIM.targetRequestState()).isEqualTo(CLAIMED);
        assertThat(TARGET_APPLY.targetRequestState()).isEqualTo(APPLIED);
        assertThat(REQUEST_ABORT.targetRequestState()).isEqualTo(ABORT_REQUESTED);
        assertThat(SOURCE_SETTLE_ABORT.targetRequestState()).isEqualTo(ABORTED);
        assertThat(RECOVER_EXPIRED_PREPARE.targetRequestState()).isEqualTo(FAILED);
        assertThat(RECOVER_EXPIRED_CLAIM.targetRequestState()).isEqualTo(FAILED);
        assertThat(MARK_FAILED.targetRequestState()).isEqualTo(FAILED);
    }

    @Test
    void terminalFlagFollowsTargetState() {
        // 仅目标为终态的操作是 terminal
        assertThat(SOURCE_BEGIN_PREPARE.isTerminal()).isFalse();
        assertThat(SOURCE_COMMIT_READY.isTerminal()).isFalse();
        assertThat(TARGET_CLAIM.isTerminal()).isFalse();
        assertThat(REQUEST_ABORT.isTerminal()).isFalse();
        assertThat(TARGET_APPLY.isTerminal()).isTrue();
        assertThat(SOURCE_SETTLE_ABORT.isTerminal()).isTrue();
        assertThat(RECOVER_EXPIRED_PREPARE.isTerminal()).isTrue();
        assertThat(RECOVER_EXPIRED_CLAIM.isTerminal()).isTrue();
        assertThat(MARK_FAILED.isTerminal()).isTrue();
    }

    @Test
    void requestOnlyOperationsDoNotConstrainPlayerState() {
        assertThat(SOURCE_BEGIN_PREPARE.requiredPlayerStates()).isEmpty();
        assertThat(REQUEST_ABORT.requiredPlayerStates()).isEmpty();
        assertThat(MARK_FAILED.requiredPlayerStates()).isEmpty();
    }

    @Test
    void dualTableOperationsRequirePlayerState() {
        assertThat(SOURCE_COMMIT_READY.requiredPlayerStates()).containsExactly(PlayerDataState.ACTIVE);
        assertThat(TARGET_CLAIM.requiredPlayerStates()).containsExactly(PlayerDataState.TRANSFER_READY);
        assertThat(TARGET_APPLY.requiredPlayerStates()).containsExactly(PlayerDataState.LOADING);
        assertThat(RECOVER_EXPIRED_CLAIM.requiredPlayerStates()).containsExactly(PlayerDataState.LOADING);
    }

    @Test
    void multiPlayerStateOperationsAcceptEither() {
        assertThat(SOURCE_SETTLE_ABORT.requiredPlayerStates())
                .containsExactlyInAnyOrder(PlayerDataState.ACTIVE, PlayerDataState.TRANSFER_READY);
        assertThat(RECOVER_EXPIRED_PREPARE.requiredPlayerStates())
                .containsExactlyInAnyOrder(PlayerDataState.ACTIVE, PlayerDataState.TRANSFER_READY);
    }

    @Test
    void contractAllowsHappyPathTransitions() {
        assertThat(TicketOperationContract.validate(SOURCE_BEGIN_PREPARE, REQUESTED))
                .isEqualTo(ALLOWED);
        assertThat(TicketOperationContract.validate(SOURCE_COMMIT_READY, PREPARING, PlayerDataState.ACTIVE))
                .isEqualTo(ALLOWED);
        assertThat(TicketOperationContract.validate(TARGET_CLAIM, READY, PlayerDataState.TRANSFER_READY))
                .isEqualTo(ALLOWED);
        assertThat(TicketOperationContract.validate(TARGET_APPLY, CLAIMED, PlayerDataState.LOADING))
                .isEqualTo(ALLOWED);
        assertThat(TicketOperationContract.validate(SOURCE_SETTLE_ABORT, ABORT_REQUESTED, PlayerDataState.TRANSFER_READY))
                .isEqualTo(ALLOWED);
        assertThat(TicketOperationContract.validate(SOURCE_SETTLE_ABORT, ABORT_REQUESTED, PlayerDataState.ACTIVE))
                .isEqualTo(ALLOWED);
        assertThat(TicketOperationContract.validate(RECOVER_EXPIRED_CLAIM, CLAIMED, PlayerDataState.LOADING))
                .isEqualTo(ALLOWED);
    }

    @ParameterizedTest
    @EnumSource(value = TransferTicketState.class, names = {"REQUESTED", "PREPARING", "READY", "CLAIMED"})
    void requestAbortIsLegalFromAllActiveStates(TransferTicketState state) {
        assertThat(TicketOperationContract.validate(REQUEST_ABORT, state)).isEqualTo(ALLOWED);
    }

    @Test
    void contractDeniesWrongRequestState() {
        assertThat(TicketOperationContract.validate(SOURCE_BEGIN_PREPARE, PREPARING))
                .isEqualTo(DENIED_REQUEST_STATE_NOT_ALLOWED);
        assertThat(TicketOperationContract.validate(SOURCE_COMMIT_READY, REQUESTED, PlayerDataState.ACTIVE))
                .isEqualTo(DENIED_REQUEST_STATE_NOT_ALLOWED);
        assertThat(TicketOperationContract.validate(TARGET_CLAIM, CLAIMED, PlayerDataState.TRANSFER_READY))
                .isEqualTo(DENIED_REQUEST_STATE_NOT_ALLOWED);
        // 终态不可发起任何活跃操作
        assertThat(TicketOperationContract.validate(SOURCE_BEGIN_PREPARE, APPLIED))
                .isEqualTo(DENIED_REQUEST_STATE_NOT_ALLOWED);
        assertThat(TicketOperationContract.validate(REQUEST_ABORT, ABORTED))
                .isEqualTo(DENIED_REQUEST_STATE_NOT_ALLOWED);
    }

    @Test
    void contractDeniesWrongPlayerState() {
        assertThat(TicketOperationContract.validate(SOURCE_COMMIT_READY, PREPARING, PlayerDataState.TRANSFER_READY))
                .isEqualTo(DENIED_PLAYER_STATE_NOT_ALLOWED);
        assertThat(TicketOperationContract.validate(TARGET_CLAIM, READY, PlayerDataState.LOADING))
                .isEqualTo(DENIED_PLAYER_STATE_NOT_ALLOWED);
        assertThat(TicketOperationContract.validate(TARGET_APPLY, CLAIMED, PlayerDataState.ACTIVE))
                .isEqualTo(DENIED_PLAYER_STATE_NOT_ALLOWED);
        assertThat(TicketOperationContract.validate(RECOVER_EXPIRED_CLAIM, CLAIMED, PlayerDataState.TRANSFER_READY))
                .isEqualTo(DENIED_PLAYER_STATE_NOT_ALLOWED);
    }

    @Test
    void contractDeniesNullPlayerStateWhenRequired() {
        // 操作要求 player_data 状态但调用方未提供 -> 拒绝
        assertThat(TicketOperationContract.validate(SOURCE_COMMIT_READY, PREPARING, null))
                .isEqualTo(DENIED_PLAYER_STATE_NOT_ALLOWED);
        assertThat(TicketOperationContract.validate(TARGET_CLAIM, READY, null))
                .isEqualTo(DENIED_PLAYER_STATE_NOT_ALLOWED);
    }

    @Test
    void contractAllowsNullPlayerStateWhenNotRequired() {
        // 请求侧操作不约束 player_data，null 应放行
        assertThat(TicketOperationContract.validate(SOURCE_BEGIN_PREPARE, REQUESTED, null))
                .isEqualTo(ALLOWED);
        assertThat(TicketOperationContract.validate(REQUEST_ABORT, READY, null))
                .isEqualTo(ALLOWED);
    }

    @Test
    void requireLegalPassesOnValidOperation() {
        // 不抛异常即通过
        TicketOperationContract.requireLegal(SOURCE_COMMIT_READY, PREPARING, PlayerDataState.ACTIVE);
        TicketOperationContract.requireLegal(TARGET_APPLY, CLAIMED, PlayerDataState.LOADING);
        TicketOperationContract.requireLegal(REQUEST_ABORT, READY);
    }

    @Test
    void requireLegalThrowsOnIllegalOperation() {
        assertThatThrownBy(() -> TicketOperationContract.requireLegal(TARGET_CLAIM, CLAIMED, PlayerDataState.TRANSFER_READY))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TARGET_CLAIM")
                .hasMessageContaining("DENIED_REQUEST_STATE_NOT_ALLOWED");
        assertThatThrownBy(() -> TicketOperationContract.requireLegal(SOURCE_COMMIT_READY, PREPARING, PlayerDataState.TRANSFER_READY))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DENIED_PLAYER_STATE_NOT_ALLOWED");
    }

    @Test
    void isLegalFromChecksRequestStateOnly() {
        assertThat(SOURCE_BEGIN_PREPARE.isLegalFrom(REQUESTED)).isTrue();
        assertThat(SOURCE_BEGIN_PREPARE.isLegalFrom(PREPARING)).isFalse();
        assertThat(REQUEST_ABORT.isLegalFrom(CLAIMED)).isTrue();
        assertThat(REQUEST_ABORT.isLegalFrom(ABORTED)).isFalse();
    }

    @Test
    void preconditionSetsAreImmutable() {
        // 操作的前置条件集合不可被外部修改，保证协议定义的权威性
        java.util.Set<TransferTicketState> states = SOURCE_COMMIT_READY.allowedRequestStates();
        assertThatThrownBy(() -> states.add(REQUESTED))
                .isInstanceOf(UnsupportedOperationException.class);
        java.util.Set<PlayerDataState> playerStates = TARGET_CLAIM.requiredPlayerStates();
        assertThatThrownBy(() -> playerStates.add(PlayerDataState.ACTIVE))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
