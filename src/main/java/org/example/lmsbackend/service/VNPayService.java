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

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

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

                //Build hash data (NO URL encoding for hash)
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(fieldValue);

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

        // Remove security hash fields before calculating signature
        fields.remove("vnp_SecureHashType");
        fields.remove("vnp_SecureHash");

        String signValue = VNPayUtil.hashAllFields(fields);
        String hashedSignValue = VNPayUtil.hmacSHA512(vnPayConfig.getVnp_HashSecret(), signValue);

        // Debug logging
        System.out.println("🔧 VNPay Debug - orderReturn:");
        System.out.println("Transaction Ref: " + vnp_TxnRef);
        System.out.println("Response Code: " + vnp_ResponseCode);
        System.out.println("Transaction Status: " + vnp_TransactionStatus);
        System.out.println("Received Hash: " + vnp_SecureHash);
        System.out.println("Hash Data: " + signValue);
        System.out.println("Calculated Hash: " + hashedSignValue);
        System.out.println("Hash Match: " + hashedSignValue.equals(vnp_SecureHash));
        System.out.println("Fields count: " + fields.size());
        System.out.println("All fields:");
        fields.forEach((key, value) -> System.out.println("  " + key + "=" + value));

        // 🧪 TEMPORARY: Skip signature validation for testing
        boolean signatureValid = hashedSignValue.equalsIgnoreCase(vnp_SecureHash);
        System.out.println("🧪 TESTING MODE: Signature validation bypassed");

        if (signatureValid) {
            if ("00".equals(vnp_TransactionStatus)) {
                System.out.println("✅ Payment SUCCESS");
                return 1; // Success
            } else {
                System.out.println("❌ Payment FAILED - Status: " + vnp_TransactionStatus);
                return 0; // Failed
            }
        } else {
            System.out.println("❌ INVALID SIGNATURE");
            System.out.println("Expected: " + hashedSignValue);
            System.out.println("Received: " + vnp_SecureHash);
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

        System.out.println("🔧 VNPay Sync TxnRef:");
        System.out.println("Database Transaction ID: " + txnRef);
        System.out.println("VNPay TxnRef: " + vnp_TxnRef);
        System.out.println("TMN Code: " + vnp_TmnCode);

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

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        // Build query string và hash
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // Build hash data string
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));

                // Build query string
                query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));

                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        String queryUrl = query.toString();
        String vnp_SecureHash = VNPayUtil.hmacSHA512(vnPayConfig.getVnp_HashSecret(), hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        String paymentUrl = vnPayConfig.getVnp_PayUrl() + "?" + queryUrl;

        System.out.println("🔗 Final VNPay URL: " + paymentUrl.substring(0, Math.min(paymentUrl.length(), 150)) + "...");

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
}
