package dev.kavinshi.playersync.protocol.ticket;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TransferTicketStateTest {

    @Test
    void allNineStatesExist() {
        assertThat(TransferTicketState.values()).hasSize(9);
    }

    @Test
    void terminalStatesAreRecognized() {
        assertThat(TransferTicketState.APPLIED.isTerminal()).isTrue();
        assertThat(TransferTicketState.ABORTED.isTerminal()).isTrue();
        assertThat(TransferTicketState.FAILED.isTerminal()).isTrue();
        assertThat(TransferTicketState.EXPIRED.isTerminal()).isTrue();
    }

    @Test
    void nonTerminalStatesAreNotTerminal() {
        assertThat(TransferTicketState.REQUESTED.isTerminal()).isFalse();
        assertThat(TransferTicketState.PREPARING.isTerminal()).isFalse();
        assertThat(TransferTicketState.READY.isTerminal()).isFalse();
        assertThat(TransferTicketState.CLAIMED.isTerminal()).isFalse();
        assertThat(TransferTicketState.ABORT_REQUESTED.isTerminal()).isFalse();
    }
}
