package vn.edu.policethief.shared.protocol;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Bounded UTF-8 lines, verb + tab-separated Base64 fields. No network deserialization. */
public final class Packet {
    public static final int MAX_LINE = 16384;

    private Packet() {}

    public static String encode(String verb, Object... fields) {
        StringBuilder b = new StringBuilder(verb);
        for (Object f : fields)
            b.append('\t')
                    .append(
                            Base64.getEncoder()
                                    .encodeToString(
                                            String.valueOf(f).getBytes(StandardCharsets.UTF_8)));
        return b.toString();
    }

    public static String[] decode(String line) {
        String[] p = line.split("\\t", -1);
        if (p.length > 32 || !p[0].matches("[A-Z_]+"))
            throw new IllegalArgumentException("Invalid message");
        for (int i = 1; i < p.length; i++)
            p[i] = new String(Base64.getDecoder().decode(p[i]), StandardCharsets.UTF_8);
        return p;
    }

    public static String read(BufferedReader in) throws IOException {
        StringBuilder b = new StringBuilder();
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\n') return b.toString();
            if (c != '\r') b.append((char) c);
            if (b.length() > MAX_LINE) throw new IOException("Message too large");
        }
        return b.isEmpty() ? null : b.toString();
    }
}
