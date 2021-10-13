package com.apex.ams.config.server.env;


import com.apex.ams.config.client.ConfigEnv;

public interface ConfigEnvRepository {

    ConfigEnv findOne(String application, String profile, String label);

    ConfigEnv refresh(String application, String profile, String label);

}
