package com.profect.tickle.global.util;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * PostgreSQL Binary COPY 포맷 쓰기 유틸.
 * - 헤더/트레일러
 * - BIGINT, TEXT(UTF-8), TIMESTAMPTZ
 * - NULL 처리 규약(length = -1)
 */
public final class PgCopyBinaryUtils {

    private PgCopyBinaryUtils() {}

    /** Postgres binary timestamptz 기준(2000-01-01 00:00:00 UTC)의 epoch 초 */
    public static final long POSTGRES_EPOCH_SECONDS = 946684800L;

    /** Binary COPY header 쓰기 */
    public static void writeHeader(DataOutputStream out) throws IOException {
        // Signature: "PGCOPY\n\377\r\n\0" (11 bytes)
        byte[] signature = new byte[]{
                0x50, 0x47, 0x43, 0x4F, 0x50, 0x59, 0x0A,  // "PGCOPY\n"
                (byte) 0xFF,                               // 0xFF
                0x0D, 0x0A, 0x00                          // "\r\n\0"
        };
        out.write(signature);
        out.writeInt(0); // flags
        out.writeInt(0); // header extension length
    }

    /** Binary COPY trailer(-1) 쓰기 */
    public static void writeTrailer(DataOutputStream out) throws IOException {
        out.writeShort(-1); // int16 = -1
    }

    /** BIGINT 컬럼 쓰기. NULL -> length = -1 */
    public static void writeInt8(DataOutputStream out, Long v) throws IOException {
        if (v == null) {
            out.writeInt(-1);
            return;
        }
        out.writeInt(8);      // length
        out.writeLong(v);     // value (DataOutputStream은 big-endian)
    }

    /** TEXT(UTF-8) 컬럼 쓰기. NULL -> length = -1 */
    public static void writeText(DataOutputStream out, String s) throws IOException {
        if (s == null) {
            out.writeInt(-1);
            return;
        }
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    /** TIMESTAMPTZ 컬럼 쓰기. NULL -> length = -1 */
    public static void writeTimestamptz(DataOutputStream out, Instant instant) throws IOException {
        if (instant == null) {
            out.writeInt(-1);
            return;
        }
        long micros = (instant.getEpochSecond() - POSTGRES_EPOCH_SECONDS) * 1_000_000L
                + (instant.getNano() / 1_000);
        out.writeInt(8);   // length
        out.writeLong(micros);
    }

    /** 안전한 Long 변환 보조 */
    public static Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Long l) return l;
        return Long.valueOf(v.toString());
    }
}
