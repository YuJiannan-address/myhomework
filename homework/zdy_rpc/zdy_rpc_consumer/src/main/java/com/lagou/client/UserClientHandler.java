package com.lagou.client;

import com.lagou.service.RpcRequest;
import com.lagou.service.connection.Connection;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.concurrent.Callable;

@ChannelHandler.Sharable
public class UserClientHandler extends ChannelInboundHandlerAdapter implements Callable {

    private ChannelHandlerContext context;
    private String result;
    private RpcRequest para;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.context = ctx;
    }

    /**
     * 收到服务端数据，唤醒等待的线程
     */
    @Override
    public synchronized void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        result = msg.toString();
        notify();
    }

    /**
     * 写出数据，开始等待唤醒
     */
    public synchronized Object call() throws Exception {
//        context.channel().writeAndFlush(para);
        ZookeeperLoadbalancer loadbalancer = ZookeeperLoadbalancer.INSTANCE;
        Connection connection = loadbalancer.getConnection();
        while (connection == null) {
            connection = loadbalancer.getConnection();
        }
        System.out.println("负载均衡器选择的服务节点：" + connection.getName());
        // 发送时间
        long start = System.currentTimeMillis();
        connection.getChannel().writeAndFlush(para);
        wait(5000);
        // 将响应时间写入节点, 响应时间为：当前时间 - 发送时间
        ZookeeperDiscovery.INSTANCE.setServerData(connection.getName(),
                "" + (System.currentTimeMillis() - start));
        return result;
    }

    /**
     * 设置参数
     */
    public void setPara(RpcRequest para) {
        this.para = para;
    }
}
