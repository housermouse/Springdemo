package com.apex.ams.config.server;

import com.apex.ams.config.server.env.AESEncryptor;
import com.apex.ams.config.server.env.EncryptProperties;
import com.apex.ams.config.server.env.EnvEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.security.crypto.encrypt.TextEncryptor;

@Configuration
@EnableConfigurationProperties(EncryptProperties.class)
public class AmsEncryptionBootstrapConfiguration implements Ordered {
    private int order = Ordered.HIGHEST_PRECEDENCE + 20;

    private static final Logger logger = LoggerFactory.getLogger(ConfigServerConfiguration.class);

    @Autowired
    private EncryptProperties properties;

    @Bean
    public TextEncryptor textEncryptor(EnvEncryptor encryptor) {
        return new TextEncryptor() {
            @Override
            public String encrypt(String text) {
                try {
                    return encryptor.encrypt(text);
                } catch (Exception e) {
                    logger.warn(e.getMessage());
                }
                return text;
            }

            @Override
            public String decrypt(String encryptedText) {
                try {
                    return encryptor.decrypt(encryptedText);
                } catch (Exception e) {
                    logger.warn(e.getMessage());
                }
                return encryptedText;
            }
        };
    }

    @Bean
    public EnvEncryptor encryptor() {
        return new AESEncryptor(properties);
    }


    @Override
    public int getOrder() {
        return this.order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
