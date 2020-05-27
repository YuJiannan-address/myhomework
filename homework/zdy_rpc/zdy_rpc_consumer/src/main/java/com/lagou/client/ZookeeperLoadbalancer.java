package com.lagou.client;

import com.lagou.service.connection.Connection;
import com.lagou.service.connection.ConnectionManager;

import java.util.List;

public class ZookeeperLoadbalancer {

    // public String get

    public static ZookeeperLoadbalancer INSTANCE = new ZookeeperLoadbalancer();

    private ZookeeperLoadbalancer() {

    }

    public Connection getConnection() {
        ZookeeperDiscovery discovery = ZookeeperDiscovery.INSTANCE;
        ConnectionManager connectionManager = ConnectionManager.INSTANCE;
        // 响应时间最小的节点
        String minTimeServer = null;
        Connection minTimeConn = null;
        try {
            // 获取服务列表
            List<String> serverList = discovery.serverList();
            int minTm = 0;
            if (serverList != null && !serverList.isEmpty()) {
                // 默认第一个
                minTm = Integer.parseInt(discovery.getServerData(serverList.get(0)));
                minTimeServer = serverList.get(0);
                for (String server : serverList) {
                    String serverData = discovery.getServerData(server);
                    int responseTime = Integer.parseInt(serverData);
                    if (responseTime < minTm
                            && connectionManager.getConnectionMap().get(server) != null) {
                        minTimeServer = server;
                        minTm = Integer.parseInt(serverData);
                    }
                    // 如果当前响应时间 > 5秒，则清零，下一轮就有机会选择到这个节点。
                    if (responseTime > 5000) {
                        discovery.setServerData(server, "0");
                    }
                    System.out.println("服务节点：" + server + ", 响应时间：" + serverData);
                }
            }
            System.out.println("最小响应时间节点：" + minTimeServer + ", 响应时间：" + minTm);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return connectionManager.getConnectionMap().get(minTimeServer);
    }
}
