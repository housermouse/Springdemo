package com.apex.ams.config.server;

import com.apex.ams.config.server.env.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class ConfigServerConfiguration {

    @Bean
    public ConfigEnvRepository jdbcEnvRepository(JdbcTemplate jdbc) {
        return new JdbcEnvRepository(jdbc);
    }

    @Bean
    @ConditionalOnProperty(value = "ams.config.server.encrypt.enabled", matchIfMissing = true)
    public ConfigEnvEncryptor configEnvEncryptor(EnvEncryptor encryptor) {
        return new CipherConfigEnvEncryptor(encryptor);
    }


}
