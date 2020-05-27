package com.lagou.client;

import com.lagou.service.JSONEncoder;
import com.lagou.service.RpcRequest;
import com.lagou.service.connection.Connection;
import com.lagou.service.connection.ConnectionManager;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RpcConsumer {
    // 1. 创建一个代理对象
    private static ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private static UserClientHandler userClientHandler = new UserClientHandler();

    public RpcConsumer() throws Exception {
        // 从注册中心获取获取服务列表，循环连接服务端
        List<String> serverList = ZookeeperDiscovery.INSTANCE.serverList();
        if (serverList != null) {
            for (String ipAndPort : serverList) {
                String[] host = ipAndPort.split(":");
                connect(host[0], Integer.parseInt(host[1]));
            }
        }
    }

    // 2. 初始化netty客户端
    public Object createProxy(final Class<?> serviceClass, final String protocol) throws Exception {
        // 1. 掉用初始化netty客户端的方法
//        if (userClientHandler == null) {
//            initClient();
//        }
        Object proxy = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class<?>[]{serviceClass}, new InvocationHandler() {
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                        if ("JSON".equals(protocol)) {
                            // 封装 RpcRequest 对象
                            RpcRequest rpcRequest = new RpcRequest();
                            rpcRequest.setClassName(serviceClass.getName());
                            rpcRequest.setMethodName(method.getName());
                            rpcRequest.setParameters(args);
                            Class[] paramTypes = new Class[args.length];
                            for (int i = 0; i < args.length; i++) {
                                paramTypes[i] = args[i].getClass();
                            }
                            rpcRequest.setParameterTypes(paramTypes);

                            // 2. 设置参数
                            userClientHandler.setPara(rpcRequest);
                            // 3. 向服务端请求数据
                            Object ret = executor.submit(userClientHandler).get();
                            return ret;
                        }
                        return null;
                    }
                });
        return proxy;
    }

//    private static Bootstrap bootstrap = new Bootstrap();

    public static Bootstrap initClient() {
        EventLoopGroup group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    protected void initChannel(SocketChannel sc) throws Exception {
                        ChannelPipeline pipeline = sc.pipeline();
                        pipeline.addLast(new JSONEncoder());
                        pipeline.addLast(new StringDecoder());
                        pipeline.addLast(userClientHandler);
                    }
                });
        return bootstrap;
    }

    public synchronized static void connect(String ip, Integer port) throws Exception {
//        ChannelFuture future = bootstrap.connect(ip, port);
        // 防止重复注册
        if (ConnectionManager.INSTANCE.getConnectionMap().get(ip + ":" + port) == null) {
//            ChannelFuture future = initClient().connect(ip, port).sync();
            ChannelFuture future = initClient().connect(ip, port).addListener(o -> {
                if (o.isSuccess()) {
                    System.out.println("连接 " + ip + ":" + port + " 成功");
                } else {
                    System.err.println("连接 " + ip + ":" + port + " 失败:" + o.cause());
                }
            });
            // 创建连接并加入到连接管理器
            Connection connection = new Connection(ip + ":" + port, future.channel());
            ConnectionManager.INSTANCE.addConnection(ip + ":" + port, connection);

        }
    }

}
