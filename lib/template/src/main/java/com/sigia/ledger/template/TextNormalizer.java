package io.slgl.template;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class TextNormalizer {

    public static final Pattern WHITESPACE_PATTERN = Pattern.compile("[\\p{Z}\\p{C}]+", Pattern.UNICODE_CHARACTER_CLASS);

    private TextNormalizer() {
    }

    public static String normalize(String text) {
        return normalizeUnicodeSymbolsToAscii(removeUnnecessaryWhitespaces(text.toLowerCase()));
    }

    private static String removeUnnecessaryWhitespaces(String text) {
        Matcher matcher = WHITESPACE_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            if (matcher.start() - 1 < 0 || matcher.end() > text.length() - 1) {
                matcher.appendReplacement(result, "");
                continue;
            }

            char prefix = text.charAt(matcher.start() - 1);
            char suffix = text.charAt(matcher.end());

            if (Character.isLetterOrDigit(prefix) && Character.isLetterOrDigit(suffix)) {
                matcher.appendReplacement(result, " ");
            } else {
                matcher.appendReplacement(result, "");
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private static String normalizeUnicodeSymbolsToAscii(String text) {
        StringBuilder result = new StringBuilder(text.length());

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            String replacement = UnicodeToASCIIUtil.escapeToAscii(c);
            if (replacement != null) {
                result.append(replacement);
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }
}
