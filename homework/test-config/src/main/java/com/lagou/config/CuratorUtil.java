package com.lagou.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.util.StringUtils;

public class CuratorUtil {
    private static final String ZOOKEEPER_HOST = "127.0.0.1:2181";
    private static final String NODE = "/dbconfig";
    private ObjectMapper mapper = new ObjectMapper();

    private CuratorFramework curatorClient;
    public static CuratorUtil INSTANCE;

    static {
        try {
            INSTANCE = new CuratorUtil();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(2);
        }
    }

    // 单例，私有构造
    private CuratorUtil() throws Exception {
        // 格式化json
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        startUp(ZOOKEEPER_HOST);
    }

    private void startUp(String zookeeperHost) throws Exception {
        // 重试机制
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 5);
        // String hostAndPort = "127.0.0.1:2181";

        curatorClient = CuratorFrameworkFactory.builder()
                .connectString(ZOOKEEPER_HOST)
                // 会话超时时间，默认60s
                .sessionTimeoutMs(10000)
                // 连接超时时间，默认15s
                .connectionTimeoutMs(30000)
                .retryPolicy(retryPolicy).build();

        curatorClient.start();
        if (curatorClient.getState() == CuratorFrameworkState.STARTED) {
            System.out.println("启动成功");
            if (curatorClient.checkExists().forPath(NODE) == null) {
                curatorClient.create().creatingParentsIfNeeded().forPath(NODE, "".getBytes());
            }
            // 添加对节点的监听
            addListener();
        }
    }

    private void addListener() throws Exception {
        final NodeCache nodeCache = new NodeCache(curatorClient, NODE, false);
        // 创建监听对象
        NodeCacheListener nodeCacheListener = () -> {
            System.out.println("Receive Event:" + nodeCache.getCurrentData().getPath());
            byte[] bytes = nodeCache.getCurrentData().getData();
            String dbConfig = new String(bytes);
            if (StringUtils.isEmpty(dbConfig)) {
                return;
            }
            System.out.println("配置变化：" + dbConfig);
            DbParam dbParam = mapper.readValue(bytes, DbParam.class);
            // 重新构建数据源
            DBUtil.INSTANCE.buildDataource(dbParam);
        };
        // 设置监听
        nodeCache.getListenable().addListener(nodeCacheListener);
        nodeCache.start();
    }

    /**
     * 设置数据库配置
     */
    public void setDbConfig(DbParam dbParam) throws Exception {
        String dbConfig = mapper.writeValueAsString(dbParam);
        curatorClient.setData().forPath(NODE, dbConfig.getBytes());
    }
}
