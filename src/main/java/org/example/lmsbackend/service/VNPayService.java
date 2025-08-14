package org.example.lmsbackend.service;

import org.example.lmsbackend.config.VNPayConfig;
import org.example.lmsbackend.utils.VNPayUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class VNPayService {

    @Autowired
    private VNPayConfig vnPayConfig;

    public String createOrder(HttpServletRequest request, int amount, String orderInfor, String urlReturn) {
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String vnp_TxnRef = VNPayUtil.getRandomNumber(8);
        String vnp_IpAddr = VNPayUtil.getIpAddress(request);
        String vnp_TmnCode = vnPayConfig.getVnp_TmnCode();
        String orderType = "other";

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount)); // Amount đã được nhân 100 ở PaymentService
        vnp_Params.put("vnp_CurrCode", "VND");

        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", orderInfor);
        vnp_Params.put("vnp_OrderType", orderType);

        String locate = "vn";
        vnp_Params.put("vnp_Locale", locate);

        // Sử dụng returnUrl từ parameter, fallback về config nếu null
        String finalReturnUrl = (urlReturn != null && !urlReturn.isEmpty()) ? urlReturn : vnPayConfig.getVnp_ReturnUrl();
        vnp_Params.put("vnp_ReturnUrl", finalReturnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        // 🌏 FIX: Sử dụng múi giờ Việt Nam (UTC+7) cho VNPay
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        // 🕐 FIX: Tăng thời gian timeout lên 30 phút để tránh timeout
        cld.add(Calendar.MINUTE, 30);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate); // REQUIRED theo VNPay docs

        // 🔒 CRITICAL: Remove vnp_SecureHash from params if it exists
        vnp_Params.remove("vnp_SecureHash");

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        boolean first = true;
        for (String fieldName : fieldNames) {
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                // Add separator if not first
                if (!first) {
                    hashData.append('&');
                    query.append('&');
                }

                //Build hash data (WITH URL encoding để match VNPay expectation)
                try {
                    hashData.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString()));
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                //Build query (WITH URL encoding for query)
                try {
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                first = false;
            }
        }
        String queryUrl = query.toString();
        String vnp_SecureHash = VNPayUtil.hmacSHA512(vnPayConfig.getVnp_HashSecret(), hashData.toString());
        
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        String paymentUrl = vnPayConfig.getVnp_PayUrl() + "?" + queryUrl;
        return paymentUrl;
    }

    public int orderReturn(HttpServletRequest request) {
        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements();) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                fields.put(fieldName, fieldValue);
            }
        }

        String vnp_SecureHash = request.getParameter("vnp_SecureHash");
        String vnp_ResponseCode = request.getParameter("vnp_ResponseCode");
        String vnp_TransactionStatus = request.getParameter("vnp_TransactionStatus");
        String vnp_TxnRef = request.getParameter("vnp_TxnRef");

        // 🚨 TEMP DEBUG - Xem tất cả params để hiểu VNPay trả về gì
        System.out.println("=== VNPay Return Debug ===");
        System.out.println("vnp_ResponseCode: " + vnp_ResponseCode);
        System.out.println("vnp_TransactionStatus: " + vnp_TransactionStatus);
        System.out.println("vnp_TxnRef: " + vnp_TxnRef);
        System.out.println("All params:");
        fields.forEach((key, value) -> {
            if (key.startsWith("vnp_")) {
                System.out.println("  " + key + ": " + value);
            }
        });

        // Remove security hash fields before calculating signature
        fields.remove("vnp_SecureHashType");
        fields.remove("vnp_SecureHash");

        String signValue = VNPayUtil.hashAllFields(fields);
        String hashedSignValue = VNPayUtil.hmacSHA512(vnPayConfig.getVnp_HashSecret(), signValue);

        // Validate signature properly
        boolean signatureValid = hashedSignValue.equalsIgnoreCase(vnp_SecureHash);
        System.out.println("Signature valid: " + signatureValid);

        if (signatureValid) {
            // Theo VNPay docs: 
            // vnp_ResponseCode: "00" = success, khác "00" = failed
            // vnp_TransactionStatus: "00" = success, khác "00" = failed
            
            // Kiểm tra cả ResponseCode và TransactionStatus
            boolean isSuccessResponse = "00".equals(vnp_ResponseCode);
            boolean isSuccessTransaction = "00".equals(vnp_TransactionStatus);
            
            if (isSuccessResponse && isSuccessTransaction) {
                System.out.println("→ RETURN: SUCCESS (1)");
                return 1; // Success
            } else {
                // Log chi tiết các mã lỗi để debug
                System.out.println("→ RETURN: FAILED (0)");
                System.out.println("  ResponseCode: " + vnp_ResponseCode + " (00=success)");
                System.out.println("  TransactionStatus: " + vnp_TransactionStatus + " (00=success)");
                
                // Giải thích mã lỗi phổ biến
                if ("24".equals(vnp_ResponseCode)) {
                    System.out.println("  → User cancelled transaction");
                } else if ("11".equals(vnp_ResponseCode)) {
                    System.out.println("  → Payment timeout");
                } else if ("51".equals(vnp_ResponseCode)) {
                    System.out.println("  → Insufficient balance");
                }
                
                return 0; // Failed
            }
        } else {
            System.out.println("→ RETURN: INVALID SIGNATURE (-1)");
            return -1; // Invalid signature
        }
    }

    /**
     * Tạo VNPay payment URL với Transaction ID được chỉ định trước
     * để đồng bộ hóa với database transaction_id
     */
    public String createOrderWithTxnRef(HttpServletRequest request, int amount, String orderInfor, String urlReturn, String txnRef) {
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String vnp_TxnRef = txnRef; // Sử dụng txnRef từ parameter thay vì tự sinh
        String vnp_IpAddr = VNPayUtil.getIpAddress(request);
        String vnp_TmnCode = vnPayConfig.getVnp_TmnCode();
        String orderType = "other";

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", orderInfor);
        vnp_Params.put("vnp_OrderType", orderType);
        vnp_Params.put("vnp_Amount", String.valueOf(amount)); // Amount đã được nhân 100 ở PaymentService
        vnp_Params.put("vnp_Locale", "vn");

        // Sử dụng returnUrl từ parameter, fallback về config nếu null
        String finalReturnUrl = (urlReturn != null && !urlReturn.isEmpty()) ? urlReturn : vnPayConfig.getVnp_ReturnUrl();
        vnp_Params.put("vnp_ReturnUrl", finalReturnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        // 🌏 FIX: Sử dụng múi giờ Việt Nam (UTC+7) cho VNPay
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        // 🕐 FIX: Tăng thời gian timeout lên 30 phút để tránh timeout
        cld.add(Calendar.MINUTE, 30);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate); // REQUIRED theo VNPay docs

        // 🔒 CRITICAL: Remove vnp_SecureHash from params if it exists
        vnp_Params.remove("vnp_SecureHash");

        // Build query string và hash - FIX theo chuẩn VNPay
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        boolean first = true;
        for (String fieldName : fieldNames) {
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                if (!first) {
                    hashData.append('&');
                    query.append('&');
                }
                
                // Build hash data string (WITH URL encoding để match VNPay)
                try {
                    hashData.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString()));
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Build query string (WITH URL encoding for query)
                try {
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                first = false;
            }
        }

        String queryUrl = query.toString();
        String vnp_SecureHash = VNPayUtil.hmacSHA512(vnPayConfig.getVnp_HashSecret(), hashData.toString());
        
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        String paymentUrl = vnPayConfig.getVnp_PayUrl() + "?" + queryUrl;

        return paymentUrl;
    }

    // Getter methods for debugging
    public String getTmnCode() {
        return vnPayConfig.getVnp_TmnCode();
    }

    public String getHashSecret() {
        return vnPayConfig.getVnp_HashSecret();
    }

    public String getPayUrl() {
        return vnPayConfig.getVnp_PayUrl();
    }

    public String getReturnUrl() {
        return vnPayConfig.getVnp_ReturnUrl();
    }

    /**
     * Validate VNPay configuration
     */
    public boolean isConfigValid() {
        return vnPayConfig.getVnp_TmnCode() != null && !vnPayConfig.getVnp_TmnCode().isEmpty() &&
               vnPayConfig.getVnp_HashSecret() != null && !vnPayConfig.getVnp_HashSecret().isEmpty() &&
               vnPayConfig.getVnp_PayUrl() != null && !vnPayConfig.getVnp_PayUrl().isEmpty() &&
               vnPayConfig.getVnp_ReturnUrl() != null && !vnPayConfig.getVnp_ReturnUrl().isEmpty();
    }

    /**
     * Get configuration status for debugging
     */
    public Map<String, Object> getConfigStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("tmnCodeExists", vnPayConfig.getVnp_TmnCode() != null && !vnPayConfig.getVnp_TmnCode().isEmpty());
        status.put("hashSecretExists", vnPayConfig.getVnp_HashSecret() != null && !vnPayConfig.getVnp_HashSecret().isEmpty());
        status.put("payUrlExists", vnPayConfig.getVnp_PayUrl() != null && !vnPayConfig.getVnp_PayUrl().isEmpty());
        status.put("returnUrlExists", vnPayConfig.getVnp_ReturnUrl() != null && !vnPayConfig.getVnp_ReturnUrl().isEmpty());
        status.put("isValid", isConfigValid());
        status.put("tmnCode", vnPayConfig.getVnp_TmnCode());
        status.put("payUrl", vnPayConfig.getVnp_PayUrl());
        status.put("returnUrl", vnPayConfig.getVnp_ReturnUrl());
        return status;
    }
}
