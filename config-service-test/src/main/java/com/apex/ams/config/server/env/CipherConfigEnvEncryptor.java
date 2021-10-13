package com.apex.ams.config.server.env;

import com.apex.ams.config.client.ConfigEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

public class CipherConfigEnvEncryptor implements ConfigEnvEncryptor {
    private static Logger logger = LoggerFactory.getLogger(CipherConfigEnvEncryptor.class);

    private EnvEncryptor encryptor;

    public  CipherConfigEnvEncryptor(EnvEncryptor encryptor){
        this.encryptor = encryptor;
    }

    @Override
    public ConfigEnv decrypt(ConfigEnv cfgEnv) {
        ConfigEnv result = new ConfigEnv(cfgEnv);
         for (PropertySource<Map<String, Object>> source : cfgEnv.getPropertySources()) {
            Map<String, Object> map = new LinkedHashMap<>(source.getSource());
            for (Map.Entry<String, Object> entry : new LinkedHashSet<>(map.entrySet())) {
                Object key = entry.getKey();
                String name = key.toString();
                if (StringUtils.isEmpty(entry.getValue())) {
                    break;
                }
                String value = entry.getValue().toString();
                if (value.startsWith("{cipher}")) {
                    map.remove(key);
                    try {
                        value = value.substring("{cipher}".length());
                        value = encryptor.decrypt(value);
                    }
                    catch (Exception e) {
                        value = "<n/a>";
                        name = "invalid." + name;
                        String message = "Cannot decrypt key: " + key + " (" + e.getClass()
                                + ": " + e.getMessage() + ")";
                        if (logger.isDebugEnabled()) {
                            logger.debug(message, e);
                        } else if (logger.isWarnEnabled()) {
                            logger.warn(message);
                        }
                    }
                    map.put(name, value);
                }
            }
            result.add(new MapPropertySource(source.getName(), map));
        }
        return result;
    }
}
