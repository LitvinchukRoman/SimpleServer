package practice2.util;

public final class ByteFormatter {

    private static final long BYTES_IN_MB = 1024L * 1024L;

    private ByteFormatter() {
    }

    public static String formatBytes(long bytes) {
        if (bytes < 0) {
            throw new IllegalArgumentException("bytes must be >= 0, got: " + bytes);
        }
        return (bytes / BYTES_IN_MB) + " MB";
    }
}
