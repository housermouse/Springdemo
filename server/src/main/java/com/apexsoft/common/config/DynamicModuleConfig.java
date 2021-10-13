package com.apexsoft.common.config;

import com.apexsoft.module.po.DynamicModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.List;

@PropertySource(value = "classpath:module.yml")
@Component
@ConfigurationProperties(prefix = "dynamic")
public class DynamicModuleConfig {

    private static final Logger log = LoggerFactory.getLogger(DynamicModuleConfig.class);

    private   List<DynamicModule> list;

    public List<DynamicModule> getList() {
        return list;
    }

    public void setList(List<DynamicModule> list) {
        this.list = list;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public DynamicModuleConfig() {
        log.info("获得的modules是：{}", list);
    }

    public static class Builder {
        List<DynamicModule> list;

        public DynamicModuleConfig build() {
            DynamicModuleConfig result = new DynamicModuleConfig();
            result.list = this.list;
            return result;
        }

        public Builder list(List<DynamicModule> list) {
            this.list = list;
            return this;
        }

    }
}

