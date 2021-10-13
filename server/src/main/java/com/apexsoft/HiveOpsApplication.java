package com.apexsoft;

import com.apex.ams.eventbus.annotation.EnableAmsEventBusBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.scheduling.annotation.EnableAsync;


@SpringBootApplication
@EnableAmsEventBusBroker
@EnableConfigurationProperties
@EnableAsync
@ComponentScan(basePackages = {"com.apexsoft","com.apex.ams"})
public class HiveOpsApplication {
    private static Logger log = LoggerFactory.getLogger(HiveOpsApplication.class);

    public static void main(String... args) {

        SpringApplication.run(HiveOpsApplication.class, args);

    }
}