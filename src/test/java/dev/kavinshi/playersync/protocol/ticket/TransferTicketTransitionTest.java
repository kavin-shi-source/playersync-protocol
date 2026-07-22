package dev.kavinshi.playersync.protocol.ticket;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static dev.kavinshi.playersync.protocol.ticket.TransferTicketState.*;
import static dev.kavinshi.playersync.protocol.ticket.TransferTicketTransition.TransitionResult.*;
import static org.assertj.core.api.Assertions.assertThat;

class TransferTicketTransitionTest {

    @ParameterizedTest
    @EnumSource(value = TransferTicketState.class, names = {"APPLIED", "ABORTED", "FAILED", "EXPIRED"})
    void terminalStatesDenyAllTransitions(TransferTicketState terminal) {
        for (TransferTicketState to : TransferTicketState.values()) {
            assertThat(TransferTicketTransition.validate(terminal, to))
                    .as("%s -> %s should be DENIED_ALREADY_TERMINAL", terminal, to)
                    .isEqualTo(DENIED_ALREADY_TERMINAL);
        }
    }

    @Test
    void happyPathTransitionsAreAllowed() {
        assertThat(TransferTicketTransition.validate(REQUESTED, PREPARING)).isEqualTo(ALLOWED);
        assertThat(TransferTicketTransition.validate(PREPARING, READY)).isEqualTo(ALLOWED);
        assertThat(TransferTicketTransition.validate(READY, CLAIMED)).isEqualTo(ALLOWED);
        assertThat(TransferTicketTransition.validate(CLAIMED, APPLIED)).isEqualTo(ALLOWED);
    }

    @Test
    void abortTransitionsAreAllowed() {
        assertThat(TransferTicketTransition.validate(REQUESTED, ABORT_REQUESTED)).isEqualTo(ALLOWED);
        assertThat(TransferTicketTransition.validate(PREPARING, ABORT_REQUESTED)).isEqualTo(ALLOWED);
        assertThat(TransferTicketTransition.validate(READY, ABORT_REQUESTED)).isEqualTo(ALLOWED);
        assertThat(TransferTicketTransition.validate(READY, ABORTED)).isEqualTo(ALLOWED);
        assertThat(TransferTicketTransition.validate(CLAIMED, ABORT_REQUESTED)).isEqualTo(ALLOWED);
        assertThat(TransferTicketTransition.validate(ABORT_REQUESTED, ABORTED)).isEqualTo(ALLOWED);
        assertThat(TransferTicketTransition.validate(ABORT_REQUESTED, FAILED)).isEqualTo(ALLOWED);
    }

    @Test
    void failureAndExpiryFromActiveStatesAreAllowed() {
        assertThat(TransferTicketTransition.validate(REQUESTED, FAILED)).isEqualTo(ALLOWED);
        assertThat(TransferTicketTransition.validate(REQUESTED, EXPIRED)).isEqualTo(ALLOWED);
        assertThat(TransferTicketTransition.validate(PREPARING, FAILED)).isEqualTo(ALLOWED);
        assertThat(TransferTicketTransition.validate(CLAIMED, FAILED)).isEqualTo(ALLOWED);
    }

    @Test
    void invalidTransitionsAreDenied() {
        // cannot go backwards
        assertThat(TransferTicketTransition.validate(PREPARING, REQUESTED)).isEqualTo(DENIED_INVALID_TRANSITION);
        assertThat(TransferTicketTransition.validate(READY, PREPARING)).isEqualTo(DENIED_INVALID_TRANSITION);
        assertThat(TransferTicketTransition.validate(CLAIMED, READY)).isEqualTo(DENIED_INVALID_TRANSITION);
        assertThat(TransferTicketTransition.validate(APPLIED, REQUESTED)).isEqualTo(DENIED_ALREADY_TERMINAL);
        // cannot skip states
        assertThat(TransferTicketTransition.validate(REQUESTED, CLAIMED)).isEqualTo(DENIED_INVALID_TRANSITION);
        assertThat(TransferTicketTransition.validate(REQUESTED, APPLIED)).isEqualTo(DENIED_INVALID_TRANSITION);
        // ABORT_REQUESTED cannot go back to active
        assertThat(TransferTicketTransition.validate(ABORT_REQUESTED, REQUESTED)).isEqualTo(DENIED_INVALID_TRANSITION);
        assertThat(TransferTicketTransition.validate(ABORT_REQUESTED, PREPARING)).isEqualTo(DENIED_INVALID_TRANSITION);
    }
}
