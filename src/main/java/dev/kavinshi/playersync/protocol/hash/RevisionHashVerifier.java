package dev.kavinshi.playersync.protocol.hash;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * 快照哈希与版本单调性纯函数校验。
 */
public final class RevisionHashVerifier {

    private RevisionHashVerifier() {
    }

    /** revision 必须严格单调递增。 */
    public static boolean verifyMonotonic(long oldRevision, long newRevision) {
        return newRevision > oldRevision;
    }

    /**
     * 校验 payload 的 SHA-256 是否与期望哈希匹配。
     *
     * @param payload  原始字节数组
     * @param expected 期望的 SHA-256 哈希值
     * @return true 表示哈希匹配
     * @throws NullPointerException 如果参数为 null
     */
    public static boolean verifyHashMatches(byte[] payload, RevisionHash expected) {
        Objects.requireNonNull(payload, "payload");
        Objects.requireNonNull(expected, "expected");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] actual = digest.digest(payload);
            return MessageDigest.isEqual(actual, expected.toByteArray());
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated by the JVM specification; should never happen
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
