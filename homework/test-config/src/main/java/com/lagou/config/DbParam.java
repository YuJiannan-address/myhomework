package com.lagou.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class DbParam {
    private String driverClassName;
    private String url;
    private String username;
    private String password;
    private String initialSize;
}
