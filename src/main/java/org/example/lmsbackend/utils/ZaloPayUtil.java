package org.example.lmsbackend.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class ZaloPayUtil {

    public static String getCurrentTimeString(String pattern) {
        return new SimpleDateFormat(pattern).format(new Date());
    }

    public static String getRandomNumber(int len) {
        Random rnd = new Random();
        String chars = "0123456789";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public static String hmacSHA256(String key, String data) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] hash = sha256_HMAC.doFinal(data.toString().getBytes(StandardCharsets.UTF_8));
            
            StringBuilder result = new StringBuilder();
            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error generating HMAC-SHA256", e);
        }
    }

    public static long getTimeStamp() {
        return System.currentTimeMillis();
    }

    public static String createAppTransId() {
        return getCurrentTimeString("yyMMdd") + "_" + getRandomNumber(6);
    }
}
