package com.apexsoft.registry.zookeeper;


import com.apexsoft.springdemo.ZookeeperRegistryCenter;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.util.List;

@SpringBootTest
public class ZookeeperRegistryCenterTest {
    private static Logger log = LoggerFactory.getLogger(ZookeeperRegistryCenterTest.class);

    @Autowired
    private ZookeeperRegistryCenter registryCenter;


    @Autowired
    private ApplicationContext context;

    //检测单元测试是否搭建成功
    @Test
    public void test1() {
        log.info(context.toString());
    }

    @Test
    public void test2(){
        List<String> list = registryCenter.getServerList();
        List<String> list2 = registryCenter.getServerList2();
        return;
    }

}
