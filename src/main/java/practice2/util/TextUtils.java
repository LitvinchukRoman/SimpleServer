package practice2.util;

public final class TextUtils {

    private TextUtils() {
    }

    public static String reverse(String s) {
        if (s == null) {
            throw new IllegalArgumentException("s must not be null");
        }
        return new StringBuilder(s).reverse().toString();
    }

    public static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public static boolean isPalindrome(String s) {
        if (s == null) {
            throw new IllegalArgumentException("s must not be null");
        }
        String normalized = s.toLowerCase().replaceAll("[^\\p{L}\\p{Nd}]", "");
        return normalized.contentEquals(new StringBuilder(normalized).reverse());
    }
}
