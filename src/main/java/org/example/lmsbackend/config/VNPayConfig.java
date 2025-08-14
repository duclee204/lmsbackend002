package org.example.lmsbackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

@Configuration
public class VNPayConfig {
    
    // VNPay Configuration
    @Value("${vnpay.tmnCode:}")
    private String vnp_TmnCode;
    
    @Value("${vnpay.hashSecret:}")
    private String vnp_HashSecret;
    
    @Value("${vnpay.payUrl:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String vnp_PayUrl;
    
    @Value("${vnpay.returnUrl:https://lms-frontend001-d43a1c85c11e.herokuapp.com/payment-success}")
    private String vnp_ReturnUrl;
    
    @Value("${vnpay.ipnUrl:https://lms-backend001-110ad185d2b7.herokuapp.com/api/vnpay/ipn}")
    private String vnp_IpnUrl;
    
    @Value("${vnpay.apiUrl:https://sandbox.vnpayment.vn/merchant_webapi/api/transaction}")
    private String vnp_ApiUrl;
    
    @PostConstruct
    public void init() {
        // VNPay configuration loaded
    }
    
    // Getters
    public String getVnp_TmnCode() {
        return vnp_TmnCode;
    }
    
    public String getVnp_HashSecret() {
        return vnp_HashSecret;
    }
    
    public String getVnp_PayUrl() {
        return vnp_PayUrl;
    }
    
    public String getVnp_ReturnUrl() {
        return vnp_ReturnUrl;
    }
    
    public String getVnp_IpnUrl() {
        return vnp_IpnUrl;
    }
    
    public String getVnp_ApiUrl() {
        return vnp_ApiUrl;
    }
}
