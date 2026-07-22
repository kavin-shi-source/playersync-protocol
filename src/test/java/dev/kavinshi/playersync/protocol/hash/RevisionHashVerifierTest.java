package dev.kavinshi.playersync.protocol.hash;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.assertj.core.api.Assertions.assertThat;

class RevisionHashVerifierTest {

    @Test
    void verifyMonotonicStrictlyIncreasing() {
        assertThat(RevisionHashVerifier.verifyMonotonic(0, 1)).isTrue();
        assertThat(RevisionHashVerifier.verifyMonotonic(5, 6)).isTrue();
        assertThat(RevisionHashVerifier.verifyMonotonic(5, 5)).isFalse();
        assertThat(RevisionHashVerifier.verifyMonotonic(6, 5)).isFalse();
    }

    @Test
    void verifyHashMatchesCorrectPayload() throws NoSuchAlgorithmException {
        byte[] payload = "hello world".getBytes(StandardCharsets.UTF_8);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        RevisionHash expected = RevisionHash.fromByteArray(digest.digest(payload));

        assertThat(RevisionHashVerifier.verifyHashMatches(payload, expected)).isTrue();
    }

    @Test
    void verifyHashMatchesRejectsTamperedPayload() throws NoSuchAlgorithmException {
        byte[] original = "hello world".getBytes(StandardCharsets.UTF_8);
        byte[] tampered = "hello warld".getBytes(StandardCharsets.UTF_8);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        RevisionHash expected = RevisionHash.fromByteArray(digest.digest(original));

        assertThat(RevisionHashVerifier.verifyHashMatches(tampered, expected)).isFalse();
    }

    @Test
    void verifyHashMatchesRejectsWrongHash() {
        byte[] payload = "test".getBytes(StandardCharsets.UTF_8);
        byte[] wrongHash = new byte[32]; // all zeros

        assertThat(RevisionHashVerifier.verifyHashMatches(payload, RevisionHash.fromByteArray(wrongHash))).isFalse();
    }
}
