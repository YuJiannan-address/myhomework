package com.lagou.client;

import com.lagou.service.UserService;

public class ClientBootStrap {

    public static final String providerName = "UserService#sayHello#";

    public static void main(String[] args) throws Exception {

        RpcConsumer rpcConsumer = new RpcConsumer();

//        UserService proxy = (UserService) rpcConsumer.createProxy(UserService.class, providerName);
        UserService proxy = (UserService) rpcConsumer.createProxy(UserService.class, "JSON");

        // System.out.println(ConnectionManager.INSTANCE.getAllConnections());
        while (true) {
            Thread.sleep(2000);
            System.out.println(proxy.sayHello("are you ok?"));
        }
    }
}
