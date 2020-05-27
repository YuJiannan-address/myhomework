package com.lagou.util;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.util.List;

public class ZookeeperRegistry {
    private static final String ZOOKEEPER_HOST = "127.0.0.1:2181";
    private static final String RPC_NAMESPACE = "zdy_rpc";

    private CuratorFramework curatorClient;
    public static ZookeeperRegistry INSTANCE;

    static {
        try {
            INSTANCE = new ZookeeperRegistry();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(2);
        }
    }

    // 单例，私有构造
    private ZookeeperRegistry() throws Exception {
        startUp(RPC_NAMESPACE, ZOOKEEPER_HOST);
    }

    /**
     * 连接 zookeeper
     */
    private void startUp(String namespace, String hostAndPort) throws Exception {
        // 重试机制
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 5);
        // String hostAndPort = "127.0.0.1:2181";

        curatorClient = CuratorFrameworkFactory.builder()
                .connectString(hostAndPort)
                // 会话超时时间，默认60s
                .sessionTimeoutMs(10000)
                // 连接超时时间，默认15s
                .connectionTimeoutMs(30000)
                // 命名空间
                .namespace(namespace)
                .retryPolicy(retryPolicy).build();

        curatorClient.start();
        if (curatorClient.getState() == CuratorFrameworkState.STARTED) {
            System.out.println("启动成功");
        }

    }

    /**
     * 注册：注册节点信息
     */
    public void regist(String ipAndPort) throws Exception {
        curatorClient.create()
                .creatingParentsIfNeeded()
                // 使用临时节点，挂了能自动删除
                .withMode(CreateMode.EPHEMERAL)
                .forPath("/" + ipAndPort, "0".getBytes());
    }

    /**
     * 发现 : 获取可用节点列表，客户端需要调用
     */
    public List<String> discovery() throws Exception {
        return curatorClient.getChildren().forPath("/");
    }
}
