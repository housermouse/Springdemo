package com.apexsoft.common.config;

import com.apexsoft.common.factory.YamlPropertyLoaderFactory;
import com.apexsoft.module.po.DynamicModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.List;

@PropertySource(value = "classpath:module.yml", encoding = "UTF-8", factory = YamlPropertyLoaderFactory.class)
@Component
@ConfigurationProperties(prefix = "dynamic")
public class DynamicModuleConfig {

    private static final Logger log = LoggerFactory.getLogger(DynamicModuleConfig.class);

    private List<DynamicModule> modules;

    public List<DynamicModule> getModules() {
        return modules;
    }

    public void setModules(List<DynamicModule> modules) {
        this.modules = modules;
    }
}

