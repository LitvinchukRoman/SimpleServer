package practice2.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("ByteFormatter")
class ByteFormatterTest {

    @Test
    @Tag("fast")
    void formatsOneMegabyte() {
        assertEquals("1 MB", ByteFormatter.formatBytes(1024L * 1024L));
    }

    @Test
    @Tag("fast")
    void negativeBytesThrow() {
        assertThrows(IllegalArgumentException.class, () -> ByteFormatter.formatBytes(-1));
    }

    @ParameterizedTest(name = "{0} bytes -> \"{1}\"")
    @Tag("fast")
    @CsvSource({
            "0,           0 MB",
            "1048576,     1 MB",
            "5242880,     5 MB",
            "1073741824,  1024 MB"
    })
    void formatsVariousSizes(long bytes, String expected) {
        assertEquals(expected, ByteFormatter.formatBytes(bytes));
    }

    @Test
    @Tag("slow")
    void formatsRealJvmMaxMemory() {
        long maxMemory = Runtime.getRuntime().maxMemory();
        assumeTrue(maxMemory != Long.MAX_VALUE, "JVM has no memory limit (-Xmx) - skipped");
        assertTrue(ByteFormatter.formatBytes(maxMemory).endsWith(" MB"));
    }
}
