package com.apexsoft.contoroller;

import com.apexsoft.common.config.DynamicModuleConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {
    @Autowired
    private DynamicModuleConfig dynamicModuleConfig;

    @RequestMapping("/sayhello")
    private String sayHello() {
        return "hello";
    }

    @RequestMapping("/dynamic")
    private String testDynamic() {
        return dynamicModuleConfig.getList().get(0).getLogourl();
    }
}
