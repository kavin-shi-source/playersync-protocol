package dev.kavinshi.playersync.protocol.error;

/**
 * 跨端共享的错误码常量。两端必须使用相同 String 值，存入 {@code transfer_request.error_code}。
 */
public final class SharedErrorCode {

    private SharedErrorCode() {
    }

    public static final String INVALID_READY_SNAPSHOT = "INVALID_READY_SNAPSHOT";
    public static final String SOURCE_SHUTTING_DOWN = "SOURCE_SHUTTING_DOWN";
    public static final String TARGET_KICKED = "TARGET_KICKED";
    public static final String TRANSFER_ALREADY_RUNNING = "TRANSFER_ALREADY_RUNNING";
    public static final String DATABASE_ERROR = "DATABASE_ERROR";
    public static final String COMMIT_RESULT_UNKNOWN = "COMMIT_RESULT_UNKNOWN";
    public static final String ABORT_ALREADY_RESOLVED = "ABORT_ALREADY_RESOLVED";
}
