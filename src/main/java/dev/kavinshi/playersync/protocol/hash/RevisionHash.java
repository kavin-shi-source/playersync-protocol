package dev.kavinshi.playersync.protocol.hash;

import java.util.Arrays;
import java.util.Objects;

/**
 * 不可变 SHA-256 哈希值对象，包装 32 字节摘要，用于快照完整性校验。
 */
public final class RevisionHash {

    public static final int LENGTH = 32;

    private final byte[] bytes;

    private RevisionHash(byte[] bytes) {
        this.bytes = bytes.clone();
    }

    public static RevisionHash fromByteArray(byte[] raw) {
        Objects.requireNonNull(raw, "raw");
        if (raw.length != LENGTH) {
            throw new IllegalArgumentException("expected " + LENGTH + " bytes, got " + raw.length);
        }
        return new RevisionHash(raw);
    }

    public byte[] toByteArray() {
        return bytes.clone();
    }

    public String toHexString() {
        StringBuilder sb = new StringBuilder(LENGTH * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RevisionHash that && Arrays.equals(bytes, that.bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }
}
