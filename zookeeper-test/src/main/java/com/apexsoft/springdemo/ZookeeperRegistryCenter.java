package com.apexsoft.springdemo;

import com.apex.ams.registry.zookeeper.ZkSerializer;
import com.apexsoft.springdemo.module.RegistryConfig;
import org.I0Itec.zkclient.IZkStateListener;
import org.I0Itec.zkclient.ZkClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(
        prefix = "ams",
        value = {"registry.protocol"},
        havingValue = "zk"
)
public class ZookeeperRegistryCenter implements RegistryCenter, Watcher {
    private static final Logger LOG = LoggerFactory.getLogger(ZookeeperRegistryCenter.class);

    private int sessionTimeout = 5000;
    private int connectionTimeout = 5000;
    private ZkClient client = null;
    private ZooKeeper zooKeeper = null;
    private RegistryConfig config;

    @Autowired
    public ZookeeperRegistryCenter ( RegistryConfig config) {
        this.config = config;
        client = new ZkClient(config.getAddress(), sessionTimeout, connectionTimeout, new ZkSerializer());
        if (StringUtils.isNotBlank(config.getUsername()) && StringUtils.isNotBlank(config.getPassword())) {
            client.addAuthInfo("digest", (config.getUsername() + ":" + config.getPassword()).getBytes());
            IZkStateListener zkStateListener = new IZkStateListener() {
                @Override
                public void handleStateChanged(Watcher.Event.KeeperState state) throws Exception {

                }

                @Override
                public void handleNewSession() throws Exception {
                    LOG.info("[" + config.getUsername() + ":" + config.getPassword()+ "]digest");
                    client.addAuthInfo("digest", ( config.getUsername() + ":" + config.getPassword()).getBytes());
                }

                @Override
                public void handleSessionEstablishmentError(Throwable error) throws Exception {

                }
            };
            client.subscribeStateChanges(zkStateListener);
        }
    }

    public ZooKeeper getZookeeper(){
        if (zooKeeper == null) {
            synchronized (this.getClass()) {
                try {
                    zooKeeper = new ZooKeeper(config.getAddress(), 15000, this);
                } catch (IOException e) {
                    LOG.error("zookeeper io ????????????", e);
                }
            }
        }

        return zooKeeper;
    }

    @Override
    public List<String> getServerList() {
        List<String> serverList = null;
        try {
            if (client.exists("/ams/aas/common.helper.info@aas.common/server")) {
                serverList = client.getChildren("/ams/aas/common.helper.info@aas.common/server");
            }
        } catch (Exception e) {
            LOG.error("??????zookeeper??????", e);
        }
        return serverList;
    }

    @Override
    public List<String> getServerList2() {
        List<String> serverList = null;
        try {
            if (client.exists("/ams/workerbee/heart.beat/server")) {
                serverList = client.getChildren("/ams/workerbee/heart.beat/server");
            }
        } catch (Exception e) {
            LOG.error("??????zookeeper??????", e);
        }
        return serverList;
    }

    @Override
    public List<String> getChildren(String path) {
        List<String> children = null;
        try {

            children = client.getChildren(path);
        } catch (Exception e) {
            LOG.error("??????zookeeper??????", e);
        }
        return children;
    }

    @Override
    public boolean exists(String path) {
        boolean exist = false;
        try {
            exist = client.exists(path);
        } catch (Exception e) {
            LOG.error("??????zookeeper??????", e);
        }
        return exist;
    }

    @Override
    public RegistryCenterStatus status() {
        RegistryCenterStatus registryCenterStatus = new RegistryCenterStatus();
        registryCenterStatus.setStatus(false);
        List<RegistryCenterStatus> nodesStatus = new ArrayList<>();
        ZooKeeper zooKeeper = this.getZookeeper();
        if (null != zooKeeper && ("CONNECTING".equals(zooKeeper.getState().name()) || "CONNECTED".equals(zooKeeper.getState().name()))) {
            registryCenterStatus.setStatus(true);
        } else {
            registryCenterStatus.setStatus(false);
            registryCenterStatus.setNote(zooKeeper == null?"null": zooKeeper.getState().toString());
            return registryCenterStatus;
        }

        String[] zkNodes = config.getAddress().split(",");
        for (String zkNode : zkNodes) {
            RegistryCenterStatus nodeStatus = new RegistryCenterStatus();
            nodeStatus.setStatus(false);
            nodeStatus.setAddress(zkNode);
            String[] zkNodeSplit = zkNode.split(":");
            boolean alive = getZkSingleStatus(zkNodeSplit[0], Integer.valueOf(zkNodeSplit[1]));
            if (alive) {
                nodeStatus.setStatus(true);
            }
            nodesStatus.add(nodeStatus);
        }
        registryCenterStatus.setNodesStatus(nodesStatus);
        return registryCenterStatus;
    }

    @Override
    public List<String> getServerList(String namespace, String serviceId) {
        String path = ZookeeperUtils.toServerPath(namespace, serviceId);
        List<String> serverList = new ArrayList<>();
        try {
            if (client.exists(path)) {
                serverList = client.getChildren(path);
            }
        } catch (Exception e) {
            LOG.error("????????????["+namespace+":"+serviceId+"]????????????", e);
        }
        return serverList;
    }

    @Override
    public List<String> getClientList(String namespace, String serviceId) {
        String path = ZookeeperUtils.toClientPath(namespace, serviceId);
        List<String> clientList = new ArrayList<>();
        try {
            if (client.exists(path)) {
                clientList = client.getChildren(path);
            }
        } catch (Exception e) {
            LOG.error("????????????["+namespace+":"+serviceId+"]????????????", e);
        }
        return clientList;
    }

    @Override
    public List<String> getServiceList(String namespace) {
        String path = ZookeeperUtils.toServicePath(namespace);
        List<String> clientList = new ArrayList<>();
        try {
            if (client.exists(path)) {
                clientList = client.getChildren(path);
            }
        } catch (Exception e) {
            LOG.error("????????????list["+namespace+"]????????????", e);
        }
        return clientList;
    }

    @Override
    public String getNode(String path) {
        return client.readData(path);
    }

    @Override
    public void process(WatchedEvent event) {
        if(Event.KeeperState.Disconnected == event.getState() || Event.KeeperState.Expired == event.getState() ) {
            zooKeeper = null;
        }
    }

    /**
     * ZooKeeper????????????
     * conf
     * ??????????????????????????????????????????
     * cons
     * ???????????????????????????????????????????????????????????? / ??????????????????????????????????????? / ?????????????????????????????? id ??????????????????????????????????????????????????????
     * dump
     * ?????????????????????????????????????????????
     * envi
     * ??????????????????????????????????????????????????? conf ????????????
     * reqs
     * ???????????????????????????
     * ruok
     * ????????????????????????????????????????????????????????????????????????????????? imok ?????????????????????????????????
     * stat
     * ???????????????????????????????????????????????????
     * wchs
     * ??????????????? watch ??????????????????
     * wchc
     * ?????? session ??????????????? watch ?????????????????????????????????????????? watch ???????????????????????????
     * wchp
     * ??????????????????????????? watch ???????????????????????????????????? session ??????????????????
     */
    private static boolean getZkSingleStatus(String host, int port) {
        boolean alive = false;
        String cmd = "ruok";
        Socket sock = null;
        BufferedReader reader = null;
        try {
            sock = new Socket(host, port);
            OutputStream outstream = sock.getOutputStream();
            // ??????Zookeeper???????????????????????????????????????
            outstream.write(cmd.getBytes());
            outstream.flush();
            sock.shutdownOutput();

            reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if ("imok".equals(line.trim())) {
                    alive = true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != sock) {
                    sock.close();
                }
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        return alive;
    }
}
