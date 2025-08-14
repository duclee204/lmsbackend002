package org.example.lmsbackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ZaloPayConfig {

    @Value("${zalopay.app.id:2553}")
    private String appId;

    @Value("${zalopay.key1:PcY4iZIKFCIdgZvA6ueMcMHHUbRLYjPL}")
    private String key1;

    @Value("${zalopay.key2:kLtgPl8HHhfvMuDHPwKfgfsY4Ydm9eIz}")
    private String key2;

    @Value("${zalopay.endpoint:https://sb-openapi.zalopay.vn/v2/create}")
    private String endpoint;

    @Value("${zalopay.callback.url:http://localhost:8080/api/payments/zalopay-callback}")
    private String callbackUrl;

    @Value("${zalopay.redirect.url:http://localhost:4200/zalopay-callback}")
    private String redirectUrl;

    @Value("${zalopay.query.url:https://sb-openapi.zalopay.vn/v2/query}")
    private String queryUrl;

    // Getters
    public String getAppId() {
        return appId;
    }

    public String getKey1() {
        return key1;
    }

    public String getKey2() {
        return key2;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public String getQueryUrl() {
        return queryUrl;
    }
}
