package hu.laci.cms.backend.config.database.migration;

/**
 * Produces the stable text representation used by migration checksums.
 *
 * <p>Line endings, trailing horizontal whitespace outside SQL literals, and
 * terminal blank lines are formatting-only differences. Text inside single
 * quotes, double quotes, and PostgreSQL dollar-quoted blocks is preserved.
 * This class deliberately does not parse or rewrite SQL semantics.</p>
 */
final class MigrationChecksumCanonicalizer {

    private MigrationChecksumCanonicalizer() {
    }

    /**
     * Converts a migration source file to its checksum representation.
     *
     * @param source UTF-8 migration source text
     * @return canonical source text suitable for hashing
     */
    static String canonicalize(String source) {
        StringBuilder canonical = new StringBuilder();
        StringBuilder line = new StringBuilder();
        QuoteState quoteState = QuoteState.NORMAL;
        String dollarQuoteDelimiter = null;

        for (int index = 0; index < source.length(); index++) {
            char character = source.charAt(index);
            if (character == '\r' || character == '\n') {
                if (character == '\r' && index + 1 < source.length() && source.charAt(index + 1) == '\n') {
                    index++;
                }
                appendLine(canonical, line, quoteState == QuoteState.NORMAL);
                line.setLength(0);
                canonical.append('\n');
                continue;
            }

            line.append(character);
            if (quoteState == QuoteState.SINGLE_QUOTE) {
                if (character == '\'' && (index + 1 == source.length() || source.charAt(index + 1) != '\'')) {
                    quoteState = QuoteState.NORMAL;
                } else if (character == '\'' && index + 1 < source.length() && source.charAt(index + 1) == '\'') {
                    line.append(source.charAt(++index));
                }
            } else if (quoteState == QuoteState.DOUBLE_QUOTE) {
                if (character == '"' && (index + 1 == source.length() || source.charAt(index + 1) != '"')) {
                    quoteState = QuoteState.NORMAL;
                } else if (character == '"' && index + 1 < source.length() && source.charAt(index + 1) == '"') {
                    line.append(source.charAt(++index));
                }
            } else if (quoteState == QuoteState.DOLLAR_QUOTE) {
                if (source.startsWith(dollarQuoteDelimiter, index)) {
                    for (int delimiterIndex = 1; delimiterIndex < dollarQuoteDelimiter.length(); delimiterIndex++) {
                        line.append(source.charAt(++index));
                    }
                    quoteState = QuoteState.NORMAL;
                    dollarQuoteDelimiter = null;
                }
            } else if (character == '\'') {
                quoteState = QuoteState.SINGLE_QUOTE;
            } else if (character == '"') {
                quoteState = QuoteState.DOUBLE_QUOTE;
            } else if (character == '$') {
                String delimiter = findDollarQuoteDelimiter(source, index);
                if (delimiter != null) {
                    for (int delimiterIndex = 1; delimiterIndex < delimiter.length(); delimiterIndex++) {
                        line.append(source.charAt(++index));
                    }
                    quoteState = QuoteState.DOLLAR_QUOTE;
                    dollarQuoteDelimiter = delimiter;
                }
            }
        }

        appendLine(canonical, line, quoteState == QuoteState.NORMAL);
        int end = canonical.length();
        while (end > 0 && canonical.charAt(end - 1) == '\n') {
            end--;
        }
        return canonical.substring(0, end);
    }

    private static void appendLine(StringBuilder target, StringBuilder line, boolean trimTrailingWhitespace) {
        int end = line.length();
        if (trimTrailingWhitespace) {
            while (end > 0 && (line.charAt(end - 1) == ' ' || line.charAt(end - 1) == '\t')) {
                end--;
            }
        }
        target.append(line, 0, end);
    }

    private static String findDollarQuoteDelimiter(String source, int start) {
        int closing = source.indexOf('$', start + 1);
        if (closing < 0) {
            return null;
        }
        for (int index = start + 1; index < closing; index++) {
            char character = source.charAt(index);
            if (!Character.isLetterOrDigit(character) && character != '_') {
                return null;
            }
        }
        return source.substring(start, closing + 1);
    }

    private enum QuoteState {
        NORMAL,
        SINGLE_QUOTE,
        DOUBLE_QUOTE,
        DOLLAR_QUOTE
    }
}
