package dev.kavinshi.playersync.protocol.hash;

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
     */
    public static boolean verifyHashMatches(byte[] payload, RevisionHash expected) {
        throw new UnsupportedOperationException("not yet implemented");
    }
}
