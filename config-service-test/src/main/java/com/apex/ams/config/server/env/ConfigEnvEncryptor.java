package com.apex.ams.config.server.env;

import com.apex.ams.config.client.ConfigEnv;

public interface ConfigEnvEncryptor {
    ConfigEnv decrypt(ConfigEnv environment);
}
