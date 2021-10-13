package com.apex.ams.config.server.env;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(EncryptProperties.PREFIX)
public class EncryptProperties {
    public static final String PREFIX = "ams.config.encrypt";

    private String key = "1eb3bea9-da46-42fb-ae0a-70a338918af9";

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
