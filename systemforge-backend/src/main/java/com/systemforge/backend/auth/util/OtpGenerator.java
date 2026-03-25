package com.systemforge.backend.auth.util;

import lombok.experimental.UtilityClass;

import java.security.SecureRandom;

/**
 * Cryptographically secure OTP generation utility.
 *
 * <p>Why {@link SecureRandom} instead of {@link java.util.Random}?
 * {@code Random} uses a linear congruential generator — an attacker who observes
 * a few OTPs can predict future ones. {@code SecureRandom} uses OS-level entropy
 * (e.g., {@code /dev/urandom} on Linux), making the output computationally unpredictable.
 *
 * <p>{@code SecureRandom} is thread-safe and expensive to instantiate — the instance
 * is stored statically and reused safely across threads.
 */
@UtilityClass
public class OtpGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int OTP_LENGTH = 6;
    private static final int OTP_UPPER_BOUND = 1_000_000; // exclusive → produces 000000–999999

    /**
     * Generates a 6-digit zero-padded OTP string.
     *
     * <p>Examples: "048291", "000023", "999999"
     *
     * @return 6-character numeric string with leading zeros preserved
     */
    public static String generate() {
        int raw = SECURE_RANDOM.nextInt(OTP_UPPER_BOUND);
        return String.format("%0" + OTP_LENGTH + "d", raw);
    }
}