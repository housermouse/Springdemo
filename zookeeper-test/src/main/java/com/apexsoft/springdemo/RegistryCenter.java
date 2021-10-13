package com.apexsoft.springdemo;


import java.util.List;

public interface RegistryCenter {



    /**
     *  获取蜂巢监控节点信息
     * @return
     */
    List<String> getServerList();

    /**
     *  获取蜂巢监控节点信息
     * @return
     */
    List<String> getServerList2();

    /**
     * 根据路径获得路径下的节点信息
     * @param path
     * @return
     */
    List<String> getChildren(final String path);

    /**
     * 判断路径是否存在
     * @param path
     * @return
     */
    public boolean exists(final String path);

    /**
     * 注册中心状态信息
     * @return
     */
    public RegistryCenterStatus status();

    public List<String> getServerList(String namespace, String serviceId);

    public List<String> getClientList(String namespace, String serviceId);

    public List<String> getServiceList(String namespace);

    public String getNode(String path);

}
