package org.example.lmsbackend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.lmsbackend.config.ZaloPayConfig;
import org.example.lmsbackend.utils.ZaloPayUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class ZaloPayService {

    @Autowired
    private ZaloPayConfig zaloPayConfig;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Tạo đơn hàng ZaloPay
     */
    public Map<String, Object> createOrder(long amount, String description, String orderInfo, String courseId) {
        try {
            String appTransId = ZaloPayUtil.createAppTransId();
            
            // Tạo order data
            Map<String, Object> order = new HashMap<>();
            order.put("app_id", Integer.parseInt(zaloPayConfig.getAppId()));
            order.put("app_user", "user_" + System.currentTimeMillis());
            order.put("app_time", ZaloPayUtil.getTimeStamp());
            order.put("amount", amount);
            order.put("app_trans_id", appTransId);
            order.put("embed_data", "{\"courseId\":\"" + courseId + "\"}");
            order.put("item", "[{\"itemid\":\"course_" + courseId + "\",\"itemname\":\"" + orderInfo + "\",\"itemprice\":" + amount + ",\"itemquantity\":1}]");
            order.put("description", description);
            order.put("bank_code", "");
            order.put("callback_url", zaloPayConfig.getCallbackUrl());

            // Tạo MAC data để tính MAC
            String data = order.get("app_id") + "|" + order.get("app_trans_id") + "|" + order.get("app_user") + "|" + 
                         order.get("amount") + "|" + order.get("app_time") + "|" + order.get("embed_data") + "|" + order.get("item");
            
            order.put("mac", ZaloPayUtil.hmacSHA256(zaloPayConfig.getKey1(), data));

            // Gọi API ZaloPay
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            // Convert order to form data
            StringBuilder formData = new StringBuilder();
            for (Map.Entry<String, Object> entry : order.entrySet()) {
                if (formData.length() > 0) {
                    formData.append("&");
                }
                formData.append(entry.getKey()).append("=").append(entry.getValue());
            }

            HttpEntity<String> request = new HttpEntity<>(formData.toString(), headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                zaloPayConfig.getEndpoint(), request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), 
                    new TypeReference<Map<String, Object>>() {});
                
                // Thêm thông tin bổ sung vào response
                responseMap.put("app_trans_id", appTransId);
                responseMap.put("course_id", courseId);
                responseMap.put("amount", amount);
                
                return responseMap;
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("return_code", -1);
                errorResponse.put("return_message", "Error calling ZaloPay API");
                return errorResponse;
            }

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("return_code", -1);
            errorResponse.put("return_message", "Internal server error: " + e.getMessage());
            return errorResponse;
        }
    }

    /**
     * Xác thực callback từ ZaloPay
     */
    public boolean verifyCallback(Map<String, String> callbackData) {
        try {
            String mac = callbackData.get("mac");
            String dataStr = callbackData.get("data");
            
            String computedMac = ZaloPayUtil.hmacSHA256(zaloPayConfig.getKey2(), dataStr);
            
            return computedMac.equals(mac);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Query trạng thái đơn hàng ZaloPay
     */
    public Map<String, Object> queryOrder(String appTransId) {
        try {
            String data = zaloPayConfig.getAppId() + "|" + appTransId + "|" + zaloPayConfig.getKey1();
            String mac = ZaloPayUtil.hmacSHA256(zaloPayConfig.getKey1(), data);

            Map<String, Object> queryData = new HashMap<>();
            queryData.put("app_id", Integer.parseInt(zaloPayConfig.getAppId()));
            queryData.put("app_trans_id", appTransId);
            queryData.put("mac", mac);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            // Convert to form data
            StringBuilder formData = new StringBuilder();
            for (Map.Entry<String, Object> entry : queryData.entrySet()) {
                if (formData.length() > 0) {
                    formData.append("&");
                }
                formData.append(entry.getKey()).append("=").append(entry.getValue());
            }

            HttpEntity<String> request = new HttpEntity<>(formData.toString(), headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                zaloPayConfig.getQueryUrl(), request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return objectMapper.readValue(response.getBody(), 
                    new TypeReference<Map<String, Object>>() {});
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("return_code", -1);
                errorResponse.put("return_message", "Error querying ZaloPay order");
                return errorResponse;
            }

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("return_code", -1);
            errorResponse.put("return_message", "Internal server error: " + e.getMessage());
            return errorResponse;
        }
    }

    /**
     * Lấy config ZaloPay (cho testing)
     */
    public Map<String, Object> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("app_id", zaloPayConfig.getAppId());
        config.put("endpoint", zaloPayConfig.getEndpoint());
        config.put("callback_url", zaloPayConfig.getCallbackUrl());
        config.put("redirect_url", zaloPayConfig.getRedirectUrl());
        return config;
    }
}
