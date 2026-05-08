import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Json {
    private Json() {
    }

    public static Object parse(String text) {
        return new Parser(text == null ? "" : text).parse();
    }

    public static String stringify(Object value) {
        StringBuilder out = new StringBuilder();
        write(value, out);
        return out.toString();
    }

    @SuppressWarnings("unchecked")
    private static void write(Object value, StringBuilder out) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof String text) {
            writeString(text, out);
        } else if (value instanceof Number || value instanceof Boolean) {
            out.append(value);
        } else if (value instanceof Map<?, ?> map) {
            out.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    out.append(',');
                }
                first = false;
                writeString(String.valueOf(entry.getKey()), out);
                out.append(':');
                write(entry.getValue(), out);
            }
            out.append('}');
        } else if (value instanceof Iterable<?> list) {
            out.append('[');
            boolean first = true;
            for (Object item : list) {
                if (!first) {
                    out.append(',');
                }
                first = false;
                write(item, out);
            }
            out.append(']');
        } else {
            writeString(String.valueOf(value), out);
        }
    }

    private static void writeString(String value, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 32) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }

    private static final class Parser {
        private final String text;
        private int index;

        Parser(String text) {
            this.text = text;
        }

        Object parse() {
            skip();
            Object value = parseValue();
            skip();
            return value;
        }

        private Object parseValue() {
            skip();
            if (index >= text.length()) {
                return null;
            }

            char c = text.charAt(index);
            if (c == '{') {
                return parseObject();
            }
            if (c == '[') {
                return parseArray();
            }
            if (c == '"') {
                return parseString();
            }
            if (starts("true")) {
                index += 4;
                return true;
            }
            if (starts("false")) {
                index += 5;
                return false;
            }
            if (starts("null")) {
                index += 4;
                return null;
            }
            return parseNumber();
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            index++;
            skip();
            if (peek('}')) {
                index++;
                return map;
            }
            while (index < text.length()) {
                String key = parseString();
                skip();
                expect(':');
                Object value = parseValue();
                map.put(key, value);
                skip();
                if (peek('}')) {
                    index++;
                    break;
                }
                expect(',');
            }
            return map;
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            index++;
            skip();
            if (peek(']')) {
                index++;
                return list;
            }
            while (index < text.length()) {
                list.add(parseValue());
                skip();
                if (peek(']')) {
                    index++;
                    break;
                }
                expect(',');
            }
            return list;
        }

        private String parseString() {
            expect('"');
            StringBuilder out = new StringBuilder();
            while (index < text.length()) {
                char c = text.charAt(index++);
                if (c == '"') {
                    break;
                }
                if (c == '\\' && index < text.length()) {
                    char escaped = text.charAt(index++);
                    switch (escaped) {
                        case '"' -> out.append('"');
                        case '\\' -> out.append('\\');
                        case '/' -> out.append('/');
                        case 'b' -> out.append('\b');
                        case 'f' -> out.append('\f');
                        case 'n' -> out.append('\n');
                        case 'r' -> out.append('\r');
                        case 't' -> out.append('\t');
                        case 'u' -> {
                            String hex = text.substring(index, Math.min(index + 4, text.length()));
                            out.append((char) Integer.parseInt(hex, 16));
                            index += 4;
                        }
                        default -> out.append(escaped);
                    }
                } else {
                    out.append(c);
                }
            }
            return out.toString();
        }

        private Number parseNumber() {
            int start = index;
            while (index < text.length()) {
                char c = text.charAt(index);
                if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') {
                    index++;
                } else {
                    break;
                }
            }
            String value = text.substring(start, index);
            if (value.contains(".") || value.contains("e") || value.contains("E")) {
                return new BigDecimal(value);
            }
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
                return Long.parseLong(value);
            }
        }

        private boolean starts(String value) {
            return text.startsWith(value, index);
        }

        private boolean peek(char expected) {
            return index < text.length() && text.charAt(index) == expected;
        }

        private void expect(char expected) {
            skip();
            if (index >= text.length() || text.charAt(index) != expected) {
                throw new IllegalArgumentException("JSON invalido");
            }
            index++;
        }

        private void skip() {
            while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
                index++;
            }
        }
    }
}
