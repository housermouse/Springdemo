package com.apex.ams.config.server.service;

import com.apex.ams.AmsConfigure;
import com.apex.ams.client.UrlServiceRefer;
import com.apex.ams.common.AmsConstants;
import com.apex.ams.config.*;
import com.apex.ams.config.client.ConfigEnv;
import com.apex.ams.config.server.env.ConfigEnvEncryptor;
import com.apex.ams.config.server.env.ConfigEnvRepository;
import com.apex.ams.config.server.env.EnvEncryptor;
import com.apex.ams.registry.Registry;
import com.apex.ams.registry.RegistryConfig;
import com.apex.ams.registry.RegistryFactory;
import com.apex.ams.registry.ServiceURL;
import com.apex.ams.server.AmsService;
import com.apex.ams.utils.NetUtils;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.util.JsonFormat;
import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.PropertySource;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.apex.ams.config.WatchRequest.RequestType.CREATE;

@AmsService
public class ConfigServiceImpl extends ConfigServiceGrpc.ConfigServiceImplBase {
    private static final Logger logger = LoggerFactory.getLogger(ConfigServiceImpl.class);
    private static String[] CONFIG_CONVERT_NAMES = {
            "overrideSystemProperties", "allowOverride", "overrideNone"
    };
    private static String CONFIG_CONVERT_PREFIX = "ams.config.";
    private static String CONFIG_CONVERT_TO_PREFIX = "spring.cloud.config.";

    private long tmRefresh;

    public class WatchInfo {
        Context context;
        WatchRequest request;
        StreamObserver<WatchResponse> responseObserver;

        public WatchInfo(Context context, WatchRequest request, StreamObserver<WatchResponse> responseObserver) {
            this.context = context;
            this.request = request;
            this.responseObserver = responseObserver;
        }

        public WatchInfo(StreamObserver<WatchResponse> responseObserver) {
            this.responseObserver = responseObserver;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            WatchInfo watchInfo = (WatchInfo) o;
            return Objects.equals(responseObserver, watchInfo.responseObserver);
        }

        @Override
        public int hashCode() {
            return Objects.hash(responseObserver);
        }
    }

    public static final int REFRESH_NOTIFY_TIMEOUT = 3000;

    private final Lock watchLock = new ReentrantLock();
    private final Condition refreshCondition = watchLock.newCondition();
    private AtomicBoolean watching = new AtomicBoolean(false);
    private ConcurrentLinkedQueue<WatchInfo> watchQueue = new ConcurrentLinkedQueue<>();

    @Autowired
    private ConfigEnvRepository envRepository;

    @Autowired(required = false)
    private ConfigEnvEncryptor configEncryptor;

    @Autowired
    private EnvEncryptor encryptor;


    @Override
    public void load(LoadRequest request, StreamObserver<LoadResponse> responseObserver) {
        ConfigEnv profiles = envRepository.findOne(request.getApplication(), request.getProfile(), request.getLabel());
        if (configEncryptor != null) {
            profiles = configEncryptor.decrypt(profiles);
        }
        LoadResponse.Builder builder = LoadResponse.newBuilder();
        if (profiles.getName() != null)
            builder.setApplication(profiles.getName());
        builder.addAllProfiles(Arrays.asList(profiles.getProfiles()));
        if (profiles.getLabel() != null)
            builder.setLabel(profiles.getLabel());
        if (profiles.getVersion() != null)
            builder.setVersion(profiles.getVersion());
        if (profiles.getState() != null)
            builder.setState(profiles.getState());
        builder.putAllProperties(convertToProperties(profiles));
        builder.setRefreshTime(tmRefresh);
        LoadResponse response = builder.build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private void watchDaemon() {
        if (watching.compareAndSet(false, true)) {
            Executors.newSingleThreadExecutor().submit(new Runnable() {
                @Override
                public void run() {
                    logger.info("我进到watchDaemon里面了");
                    while (watching.get()) {
                        List<WatchInfo> aliveWatch = new ArrayList<>();
                        try {
                            watchLock.lock();
                            try {
                                if (refreshCondition.await(50, TimeUnit.MILLISECONDS)) {
                                    for (; ; ) {
                                        WatchInfo watchInfo = watchQueue.poll();
                                        if (watchInfo == null)
                                            break;
                                        Context context = watchInfo.context;
                                        if (!context.isCancelled()) {
                                            Context previous = context.attach();
                                            try {
                                                WatchRequest request = watchInfo.request;
                                                ConfigEnv profiles = envRepository.findOne(request.getApplication()
                                                        , request.getProfile(), request.getLabel());
                                                WatchResponse.Builder builder = WatchResponse.newBuilder();
                                                if (profiles.getName() != null)
                                                    builder.setApplication(profiles.getName());
                                                builder.addAllProfiles(Arrays.asList(profiles.getProfiles()));
                                                if (profiles.getLabel() != null)
                                                    builder.setLabel(profiles.getLabel());
                                                if (profiles.getVersion() != null)
                                                    builder.setVersion(profiles.getVersion());
                                                if (profiles.getState() != null)
                                                    builder.setState(profiles.getState());
                                                builder.setType(WatchResponse.ResponseType.REFRESH);
                                                builder.setRefreshTime(tmRefresh);
                                                WatchResponse response = builder.build();

                                                watchInfo.responseObserver.onNext(response);
                                                aliveWatch.add(watchInfo);
                                            } finally {
                                                context.detach(previous);
                                            }
                                        }
                                    }

                                }
                            } catch (InterruptedException e) {
                            }
                        } finally {
                            watchLock.unlock();
                            for (; ; ) {
                                WatchInfo watchInfo = watchQueue.poll();
                                if (watchInfo == null)
                                    break;
                                Context context = watchInfo.context;
                                if (!context.isCancelled()) {
                                    aliveWatch.add(watchInfo);
                                }
                            }
                            watchQueue.addAll(aliveWatch);
                        }
                    }
                    for (; ; ) {
                        WatchInfo watchInfo = watchQueue.poll();
                        if (watchInfo == null)
                            break;
                        Context context = watchInfo.context;
                        if (!context.isCancelled()) {
                            Context previous = context.attach();
                            try {
                                watchInfo.responseObserver.onError(new StatusException(Status.CANCELLED));
                            } finally {
                                context.detach(previous);
                            }
                        }
                    }
                }
            });
        }
    }

    @Override
    public StreamObserver<WatchRequest> watch(StreamObserver<WatchResponse> responseObserver) {
        StreamObserver<WatchRequest> requestStreamObserver = new StreamObserver<WatchRequest>() {
            @Override
            public void onNext(WatchRequest request) {
                try {
                    logger.info("我进入了watch函数");
                    //watchLock.lock();
                    WatchInfo watchInfo = new WatchInfo(Context.current(), request, responseObserver);
                    switch (request.getType()) {
                        case UNSPECIFIED:
                        case CREATE:
                            if (!watchQueue.contains(watchInfo))
                                watchQueue.add(watchInfo);
                            if (request.getType() == CREATE) {
                                responseObserver.onNext(createdResponse(request));
                            }
                            break;
                        case PING:
                            if (watchQueue.contains(watchInfo))
                                responseObserver.onNext(pongResponse(request));
                            else
                                responseObserver.onError(new StatusException(Status.DATA_LOSS));
                            break;
                        case CLOSE:
                            watchQueue.remove(watchInfo);
                            responseObserver.onCompleted();
                            break;
                    }
                } catch (Exception e) {
                    logger.warn(e.getMessage());
                } finally {
                    //watchLock.unlock();
                }
            }

            @Override
            public void onError(Throwable t) {
                logger.warn("config watch cancelled");
                WatchInfo watchInfo = new WatchInfo(responseObserver);
                watchQueue.remove(watchInfo);
            }

            @Override
            public void onCompleted() {
                //--旧版本客户端非流请求，会立刻关闭
                //responseObserver.onCompleted();
            }
        };

        watchDaemon();

        return requestStreamObserver;
    }

    private WatchResponse createdResponse(WatchRequest request) {
        return WatchResponse.newBuilder().setType(WatchResponse.ResponseType.CREATED).setRefreshTime(tmRefresh).build();
    }

    private WatchResponse pongResponse(WatchRequest request) {
        return WatchResponse.newBuilder().setType(WatchResponse.ResponseType.PONG).setRefreshTime(tmRefresh).build();
    }

    @Override
    public void refresh(RefreshRequest request, StreamObserver<RefreshResponse> responseObserver) {
        String svrId = AmsConfigure.getInstance().getServerId();
        String nodeId = request.getNodeId();
        if (StringUtils.isEmpty(nodeId)) {
            logger.info("receive refresh notify from client");
            ConfigEnv profiles = envRepository.refresh(request.getApplication(), request.getProfile(), request.getLabel());

            try {
                broadcast(request, svrId);
            } catch (Exception e) {
                logger.warn("broadcast refresh notify error: {}", e.getMessage());
            }

            responseRefresh(profiles, request, responseObserver);

        } else {
            logger.info("receive refresh notify from config svr: {} ,is me: {}", nodeId, nodeId.equals(svrId));
            if (!nodeId.equals(svrId)) {
                ConfigEnv profiles = envRepository.refresh(request.getApplication(), request.getProfile(), request.getLabel());
                responseRefresh(profiles, request, responseObserver);
            } else {
                responseObserver.onNext(RefreshResponse.newBuilder().build());
                responseObserver.onCompleted();
            }
        }
    }

    @Override
    public void encrypt(EncryptRequest request, StreamObserver<EncryptResponse> responseObserver) {
        try {
            String encText = encryptor.encrypt(request.getText());
            EncryptResponse response = EncryptResponse.newBuilder().setText(encText).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asException());
        }

    }

    private void broadcast(RefreshRequest request, String svrId) throws Exception {
        //---------------------------------------------------------------------------------------------------------
        //--刷新时候需要通知其他配置服务器，这里直接检查并逐台通知，处理比较复杂，但因为仅这里内部时候，所以采用这个方案；
        // 其他时候可以考虑通过SpringCloud Bus简化
        RegistryConfig regCfg = getRegistryConfig();
        Registry registy = RegistryFactory.getInstance().getRegistry(regCfg);
        String localAddress = NetUtils.getLocalHostAddress(regCfg.getAddress(), AmsConstants.DEFAULT_PORT);
        ServiceURL serviceUrl = new ServiceURL("grpc", localAddress, 0, ConfigServiceGrpc.getServiceDescriptor()
                .getName());
        List<ServiceURL> serviceURLs = registy.discover(serviceUrl);
        if (serviceURLs.size() > 1) {
            ArrayList<ImmutablePair<ManagedChannel, ListenableFuture<RefreshResponse>>> lstCalls = new ArrayList<>();
            for (ServiceURL url : serviceURLs) {
                logger.info("call refresh to " + url);
                UrlServiceRefer<ConfigServiceGrpc> serviceRef = new UrlServiceRefer<>(url, ConfigServiceGrpc
                        .class);
                try {
                    ManagedChannel channel = serviceRef.channelBuilder().build();
                    RefreshRequest nreq = RefreshRequest.newBuilder(request).setNodeId(svrId).build();
                    ListenableFuture<RefreshResponse> future = ConfigServiceGrpc.newFutureStub(channel).refresh
                            (nreq);
                    // RefreshResponse rep = ConfigServiceGrpc.newBlockingStub(channel).refresh(nreq);
                    //logger.info(JsonFormat.printer().print(rep));
                    lstCalls.add(ImmutablePair.of(channel, future));
                } catch (Exception e) {
                    logger.warn(e.getMessage(), e);
                }
            }
            long tmDeadline = System.currentTimeMillis() + REFRESH_NOTIFY_TIMEOUT; //超时时间
            while (!lstCalls.isEmpty() && System.currentTimeMillis() < tmDeadline) {
                for (int idx = 0; idx < lstCalls.size(); idx++) {
                    ImmutablePair<ManagedChannel, ListenableFuture<RefreshResponse>> pair = lstCalls.get(idx);
                    if (pair.right.isDone()) {
                        try {
                            if (logger.isDebugEnabled())
                                logger.debug(JsonFormat.printer().print(pair.right.get()));
                        } catch (Exception e) {
                        }
                        if (!pair.left.isShutdown())
                            pair.left.shutdown();
                        lstCalls.remove(idx);
                        continue;
                    }
                }
            }
            if (!lstCalls.isEmpty()) {
                logger.warn("call refresh notify timeout");
            }
        }
    }


    protected void responseRefresh(ConfigEnv profiles, RefreshRequest request, StreamObserver<RefreshResponse>
            responseObserver) {
        tmRefresh = System.currentTimeMillis();

        RefreshResponse.Builder builder = RefreshResponse.newBuilder();
        if (profiles.getName() != null)
            builder.setApplication(profiles.getName());
        builder.addAllProfiles(Arrays.asList(profiles.getProfiles()));
        if (profiles.getLabel() != null)
            builder.setLabel(profiles.getLabel());
        if (profiles.getVersion() != null)
            builder.setVersion(profiles.getVersion());
        if (profiles.getState() != null)
            builder.setState(profiles.getState());
        builder.setRefreshTime(tmRefresh);
        RefreshResponse response = builder.build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
        try {
            watchLock.lock();
            refreshCondition.signalAll();
        } finally {
            watchLock.unlock();
        }
    }

    private RegistryConfig getRegistryConfig() throws Exception {
//        String name = AmsConfigure.getInstance().getValue("ams.registry.name", "default");
//        String protocol = AmsConfigure.getInstance().getNamedValue(name, "ams.registry.protocol", "zk");
//        String address = AmsConfigure.getInstance().getNamedValue(name, "ams.registry.address", "");
//        if (StringUtils.isEmpty(address))
//            throw new Exception("registry address is empty");
//        return new RegistryConfig(name, protocol, address);
        return RegistryConfig.newBuilder().build();
    }

    private Map<String, String> convertToProperties(ConfigEnv profiles) {

        // Map of unique keys containing full map of properties for each unique
        // key
        Map<String, Map<String, String>> map = new LinkedHashMap<>();
        List<PropertySource<Map<String, Object>>> sources = new ArrayList<>(profiles.getPropertySources());
        Collections.reverse(sources);
        Map<String, String> combinedMap = new TreeMap<>();
        for (PropertySource<Map<String, Object>> source : sources) {

            @SuppressWarnings("unchecked")
            Map<String, Object> value = source.getSource();
            Set<String> processKeys = new HashSet<>();
            for (String key : value.keySet()) {

                if (!key.contains("[")) {
                    // Not an array, add unique key to the map
                    // Check key name should to convert
                    String name = key;
                    if (name.startsWith(CONFIG_CONVERT_PREFIX)) {
                        String snv = name.substring(CONFIG_CONVERT_PREFIX.length());
                        for (String s : CONFIG_CONVERT_NAMES) {
                            if (s.equals(snv)) {
                                name = CONFIG_CONVERT_TO_PREFIX + snv;
                                break;
                            }
                        }
                    }
                    if (value.get(key) != null) {
                        combinedMap.put(name, value.get(key).toString());
                    }

                } else {

                    // An existing array might have already been added to the property map
                    // of an unequal size to the current array. Replace the array key in
                    // the current map.
                    key = key.substring(0, key.indexOf("["));
                    if (!processKeys.contains(key)) {
                        processKeys.add(key);
                        Map<String, String> filtered = new TreeMap<>();
                        for (String index : value.keySet()) {
                            if (index.startsWith(key + "[")) {
                                filtered.put(index, value.get(index).toString());
                            }
                        }
                        map.put(key, filtered);
                    }
                }
            }

        }

        // Combine all unique keys for array values into the combined map
        for (Map.Entry<String, Map<String, String>> entry : map.entrySet()) {
            combinedMap.putAll(entry.getValue());
        }

//        postProcessProperties(combinedMap);
        return combinedMap;
    }

//    private void postProcessProperties(Map<String, String> propertiesMap) {
//        for (Iterator<String> iter = propertiesMap.keySet().iterator(); iter.hasNext();) {
//            String key = iter.next();
//            if (key.equals("spring.profiles")) {
//                iter.remove();
//            }
//        }
//    }

    /**
     * Class {@code PropertyNavigator} is used to navigate through the property key and create necessary Maps and Lists
     * making up the nested structure to finally set the property value at the leaf node.
     * <p>
     * The following rules in yml/json are implemented:
     * <pre>
     * 1. an array element can be:
     *    - a value (leaf)
     *    - a map
     *    - a nested array
     * 2. a map value can be:
     *    - a value (leaf)
     *    - a nested map
     *    - an array
     * </pre>
     */
    private static class PropertyNavigator {

        private enum NodeType {LEAF, MAP, ARRAY}

        private final String propertyKey;
        private int currentPos;
        private NodeType valueType;

        private PropertyNavigator(String propertyKey) {
            this.propertyKey = propertyKey;
            currentPos = -1;
            valueType = NodeType.MAP;
        }

        private void setMapValue(Map<String, Object> map, Object value) {
            String key = getKey();
            if (NodeType.MAP.equals(valueType)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) map.get(key);
                if (nestedMap == null) {
                    nestedMap = new LinkedHashMap<>();
                    map.put(key, nestedMap);
                }
                setMapValue(nestedMap, value);
            } else if (NodeType.ARRAY.equals(valueType)) {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) map.get(key);
                if (list == null) {
                    list = new ArrayList<>();
                    map.put(key, list);
                }
                setListValue(list, value);
            } else {
                map.put(key, value);
            }
        }

        private void setListValue(List<Object> list, Object value) {
            int index = getIndex();
            // Fill missing elements if needed
            while (list.size() <= index) {
                list.add(null);
            }
            if (NodeType.MAP.equals(valueType)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) list.get(index);
                if (map == null) {
                    map = new LinkedHashMap<>();
                    list.set(index, map);
                }
                setMapValue(map, value);
            } else if (NodeType.ARRAY.equals(valueType)) {
                @SuppressWarnings("unchecked")
                List<Object> nestedList = (List<Object>) list.get(index);
                if (nestedList == null) {
                    nestedList = new ArrayList<>();
                    list.set(index, nestedList);
                }
                setListValue(nestedList, value);
            } else {
                list.set(index, value);
            }
        }

        private int getIndex() {
            // Consider [
            int start = currentPos + 1;

            for (int i = start; i < propertyKey.length(); i++) {
                char c = propertyKey.charAt(i);
                if (c == ']') {
                    currentPos = i;
                    break;
                } else if (!Character.isDigit(c)) {
                    throw new IllegalArgumentException("Invalid key: " + propertyKey);
                }
            }
            // If no closing ] or if '[]'
            if (currentPos < start || currentPos == start) {
                throw new IllegalArgumentException("Invalid key: " + propertyKey);
            } else {
                int index = Integer.parseInt(propertyKey.substring(start, currentPos));
                // Skip the closing ]
                currentPos++;
                if (currentPos == propertyKey.length()) {
                    valueType = NodeType.LEAF;
                } else {
                    switch (propertyKey.charAt(currentPos)) {
                        case '.':
                            valueType = NodeType.MAP;
                            break;
                        case '[':
                            valueType = NodeType.ARRAY;
                            break;
                        default:
                            throw new IllegalArgumentException("Invalid key: " + propertyKey);
                    }
                }
                return index;
            }
        }

        private String getKey() {
            // Consider initial value or previous char '.' or '['
            int start = currentPos + 1;
            for (int i = start; i < propertyKey.length(); i++) {
                char currentChar = propertyKey.charAt(i);
                if (currentChar == '.') {
                    valueType = NodeType.MAP;
                    currentPos = i;
                    break;
                } else if (currentChar == '[') {
                    valueType = NodeType.ARRAY;
                    currentPos = i;
                    break;
                }
            }
            // If there's no delimiter then it's a key of a leaf
            if (currentPos < start) {
                currentPos = propertyKey.length();
                valueType = NodeType.LEAF;
                // Else if we encounter '..' or '.[' or start of the property is . or [ then it's invalid
            } else if (currentPos == start) {
                throw new IllegalArgumentException("Invalid key: " + propertyKey);
            }
            return propertyKey.substring(start, currentPos);
        }
    }
}
