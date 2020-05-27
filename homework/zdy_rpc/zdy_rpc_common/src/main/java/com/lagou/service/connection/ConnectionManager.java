package com.lagou.service.connection;

import io.netty.channel.ChannelId;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@ToString
public class ConnectionManager {
    private Map<String, Connection> connectionMap = new HashMap<>();

    public static ConnectionManager INSTANCE = new ConnectionManager();

    // 私有构造，单例
    private ConnectionManager() {

    }

    public void addConnection(String id, Connection connection) {
        connectionMap.putIfAbsent(id, connection);
    }

//    public Connection removeConnection(String key) {
//        return connectionMap.remove(key);
//    }

    public void removeAndClose(String id) {
        Connection conn = connectionMap.remove(id);
        conn.getChannel().close();
    }

    public List<Connection> getAllConnections() {
        return new ArrayList<>(connectionMap.values());
    }
}
