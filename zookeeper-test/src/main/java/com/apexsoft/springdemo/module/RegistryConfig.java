package com.apexsoft.springdemo.module;

import com.apex.ams.registry.zookeeper.ZookeeperRegistry;
import org.I0Itec.zkclient.IZkStateListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.serialize.SerializableSerializer;
import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Created by Logan on 2018/2/11.
 */
@Configuration("fcRegistryConfig")
@ConfigurationProperties(prefix = "ams.registry")
public class RegistryConfig implements Watcher  {
    private static final Logger log = LoggerFactory.getLogger(RegistryConfig.class);


    private boolean inner;
    private String address;
    private String protocol;
    private String username;
    private String password;
    private String authority;
    private boolean usePlaintext = true;
    private String caCertFile;
    private String certFile;
    private String keyFile;
    private String keyPassword;
    private String enabledOcsp;

    private int sessionTimeout = 5000;
    private int connectionTimeout = 5000;
    private volatile ZkClient zk = null;
    private volatile ZooKeeper zooKeeper = null;

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public RegistryConfig() {
        ZookeeperRegistry zkr;
    }

    public String getAddress() {
        return isInner() && StringUtils.isBlank(address) ? "127.0.0.1:2181" : address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isUsePlaintext() {
        return usePlaintext;
    }

    public void setUsePlaintext(boolean usePlaintext) {
        this.usePlaintext = usePlaintext;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getCaCertFile() {
        return caCertFile;
    }

    public void setCaCertFile(String caCertFile) {
        this.caCertFile = caCertFile;
    }

    public String getCertFile() {
        return certFile;
    }

    public void setCertFile(String certFile) {
        this.certFile = certFile;
    }

    public String getKeyFile() {
        return keyFile;
    }

    public void setKeyFile(String keyFile) {
        this.keyFile = keyFile;
    }

    public String getKeyPassword() {
        return keyPassword;
    }

    public void setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
    }

    public String getEnabledOcsp() {
        return enabledOcsp;
    }

    public void setEnabledOcsp(String enabledOcsp) {
        this.enabledOcsp = enabledOcsp;
    }

    /**
     *  // TODO  zk 控制微服务配置有用到。 后续看是否支持ETCD
     * @return
     */
    public ZkClient getZkClient() {
        if (zk == null) {
            synchronized (ZkClient.class) {
                if (zk == null) {
                    zk = new ZkClient(this.getAddress(), sessionTimeout, connectionTimeout, new SerializableSerializer());
                    if (StringUtils.isNotBlank(this.username) && StringUtils.isNotBlank(this.password)) {
                        zk.addAuthInfo("digest", (this.username + ":" + this.password).getBytes());
                        IZkStateListener zkStateListener = new IZkStateListener() {
                            @Override
                            public void handleStateChanged(Watcher.Event.KeeperState state) throws Exception {

                            }

                            @Override
                            public void handleNewSession() throws Exception {
                                log.info("[" + username + ":" + password + "]digest");
                                zk.addAuthInfo("digest", (username + ":" + password).getBytes());
                            }

                            @Override
                            public void handleSessionEstablishmentError(Throwable error) throws Exception {

                            }
                        };
                        zk.subscribeStateChanges(zkStateListener);
                    }
                }
            }
        }

        return zk;
    }

    public boolean isInner() {
        return inner;
    }

    public void setInner(boolean inner) {
        this.inner = inner;
    }


    @Override
    public void process(WatchedEvent event) {
        if(Event.KeeperState.Disconnected == event.getState() || Event.KeeperState.Expired == event.getState() ) {
            zooKeeper = null;
        }
    }

}
