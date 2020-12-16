package me.redteapot.rebot;

import org.slf4j.helpers.MessageFormatter;

public class Strings {
    public static String format(String fmt, Object... args) {
        return MessageFormatter.format(fmt, args).getMessage();
    }

    public static String comment(String source, String comment, int position, int gaps) {
        int lineStart = findLineStart(source, position);
        int lineEnd = findLineEnd(source, position);

        int displayStart = Math.max(lineStart, position - gaps);
        int displayEnd = Math.min(lineEnd, position + gaps + 1);

        int pointerOffset = position - displayStart;

        return new StringBuilder()
            .append(source, displayStart, displayEnd).append('\n')
            .append(" ".repeat(pointerOffset)).append("^\n")
            .append(" ".repeat(pointerOffset)).append(comment).append("\n")
            .toString();
    }

    public static String comment(String source, String comment, int start, int end, int gaps) {
        int lineStart = findLineStart(source, start);
        int lineEnd = findLineEnd(source, end);

        int displayStart = Math.max(lineStart, start - gaps);
        int displayEnd = Math.min(lineEnd, end + gaps + 1);

        StringBuilder result = new StringBuilder();
        int currentLineStart = displayStart;
        int currentLineEnd = source.indexOf('\n', currentLineStart + 1);
        if (currentLineEnd < 0) {
            currentLineEnd = source.length();
        }
        int highlightLastLineOffset = 0;
        while (currentLineStart >= 0 && currentLineStart < displayEnd) {
            int highlightStart = Math.max(start, currentLineStart);
            int highlightEnd = Math.min(end, currentLineEnd);

            highlightLastLineOffset = Math.max(0, highlightStart - currentLineStart);

            result.append(source, currentLineStart, currentLineEnd).append('\n');
            result.append(" ".repeat(highlightLastLineOffset));
            result.append("^".repeat(Math.min(currentLineEnd - highlightStart, highlightEnd - currentLineStart)));
            result.append('\n');

            currentLineStart = currentLineEnd + 1;
            currentLineEnd = source.indexOf('\n', currentLineStart + 1);
            if (currentLineEnd < 0) {
                currentLineEnd = source.length();
            }
        }

        result.append(" ".repeat(highlightLastLineOffset));
        result.append(comment).append('\n');

        return result.toString();
    }

    private static int findLineStart(String source, int position) {
        for (int i = position; i >= 0; i--) {
            if (i >= source.length()) {
                continue;
            }
            if (source.charAt(i) == '\n') {
                return i + 1;
            }
        }

        return 0;
    }

    private static int findLineEnd(String source, int position) {
        int lineEnd = source.indexOf('\n', position);

        if (lineEnd == -1) {
            return source.length();
        }

        return lineEnd;
    }
}
