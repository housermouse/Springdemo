package com.apexsoft;

import com.apex.livegql.autoconfigure.WebSocketAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(exclude = {WebSocketAutoConfiguration.class})
//@EnableRedisRepositories
public class HiveOpsApplication {
    private static Logger log = LoggerFactory.getLogger(HiveOpsApplication.class);




    public static void main(String... args) {


        SpringApplication.run(HiveOpsApplication.class, args);

    }


}