package com.homeauto.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

// Turns a password into a SHA-256 hash so we never store the plain text
public class PasswordUtil {

    public static String hash(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Could not hash password", e);
        }
    }
}
