package com.rest.util;

public class AppConstant {

    public static final String[] PUBLIC_URLS = {
            "/auth/**",
            "/ws/**",
            "/api/orderbook/**",
            "/api/marketdata/**",
            "/api/trades/**",
            "/api/orders/**",
            "/api/customers/**",
            "/"
    };

    public static final String ADMIN_ROLE = "ADMIN";
    public static final String GUEST_ROLE = "GUEST";
}