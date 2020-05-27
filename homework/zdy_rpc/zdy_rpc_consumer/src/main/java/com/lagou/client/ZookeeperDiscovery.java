package com.lagou.client;

import com.lagou.service.connection.ConnectionManager;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ZookeeperDiscovery {
    private static final String ZOOKEEPER_HOST = "127.0.0.1:2181";
    private static final String RPC_NAMESPACE = "zdy_rpc";

    private CuratorFramework curatorClient;
    public static ZookeeperDiscovery INSTANCE;

    static {
        try {
            INSTANCE = new ZookeeperDiscovery();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(2);
        }
    }

    // 单例，私有构造
    private ZookeeperDiscovery() throws Exception {
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
            // 添加对子节点的监听
            addChildrenListener();
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
    public List<String> serverList() throws Exception {
        return curatorClient.getChildren().forPath("/");
    }

    /**
     * 获取服务注册节点的信息
     */
    public String getServerData(String path) {
        try {
            byte[] bytes = curatorClient.getData().forPath("/" + path);
            return new String(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 设置服务注册节点的信息
     */
    public void setServerData(String path, String data) {
        try {
            curatorClient.setData().forPath("/" + path, data.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 添加监听子节点的监听器
     */
    public void addChildrenListener() throws Exception {
        PathChildrenCache pcCache = new PathChildrenCache(curatorClient, "/", true);
        pcCache.getListenable().addListener(new ChildrenListener());
        pcCache.start(PathChildrenCache.StartMode.NORMAL);
    }

    static class ChildrenListener implements PathChildrenCacheListener {

        @Override
        public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
//            System.out.println("Receive Event:" + event.getType());
            String path = event.getData().getPath();
            String node = path.substring(path.lastIndexOf("/") + 1);
            // 子节点删除
            if (event.getType() == PathChildrenCacheEvent.Type.CHILD_REMOVED) {
                System.out.println("remove :" + node);
                ConnectionManager.INSTANCE.removeAndClose(node);
            } else if (event.getType() == PathChildrenCacheEvent.Type.CHILD_ADDED) {
                System.out.println("add :" + node);
                String[] hostInfo = node.split(":"); // TODO
                // 新加进来的节点先将响应时间清零
                 ZookeeperDiscovery.INSTANCE.setServerData(node, "0");
                // 有服务端注册上来，就添加连接
                RpcConsumer.connect(hostInfo[0], Integer.parseInt(hostInfo[1]));
                // ConnectionManager.INSTANCE.removeAndClose();
            }
        }
    }
}
