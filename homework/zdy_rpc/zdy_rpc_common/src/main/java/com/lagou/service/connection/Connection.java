package com.lagou.service.connection;

import com.lagou.service.RpcRequest;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Setter
@Getter
@ToString
public class Connection {

    private Channel channel;

    private String name;

    public Connection(String name, Channel channel) {
        this.channel = channel;
        this.name = name;
    }

    /**
     * 发送消息
     */
    public ChannelFuture writeAndFlush(RpcRequest para) {
        if (this.channel.isActive()) {
            return channel.writeAndFlush(para);
        }
        return null;
    }
}
