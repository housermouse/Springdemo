package com.apexsoft.springdemo;

import org.apache.commons.lang3.StringUtils;

public class ZookeeperUtils {

    private static String toRootPath(String namespace, String serviceId) {
        StringBuffer sb = new StringBuffer();
        sb.append("/ams");
        if (StringUtils.isNotBlank(namespace)) {
            sb.append("/").append(namespace);
        }
        sb.append("/").append(serviceId);
        return  sb.toString();
    }

    public static String toServerPath(String namespace, String serviceId) {
        String rootPath = toRootPath(namespace, serviceId);
        String path = rootPath + "/server";
        return path;
    }

    public static String toClientPath(String namespace, String serviceId) {
        String rootPath = toRootPath(namespace, serviceId);
        String path = rootPath + "/client";
        return path;
    }

    public static String toServicePath(String namespace) {
        StringBuffer sb = new StringBuffer();
        sb.append("/ams");
        if (StringUtils.isNotBlank(namespace)) {
            sb.append("/").append(namespace);
        }
        return sb.toString();
    }


}

