package com.lagou.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.lagou.config.CuratorUtil;
import com.lagou.config.DBUtil;
import com.lagou.config.DbParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.sql.SQLException;

@RestController
public class TestController {
    private ObjectMapper mapper = new ObjectMapper();
    private CuratorUtil curatorUtil = CuratorUtil.INSTANCE;


    @GetMapping("/test")
    public String testDb() throws SQLException {
        if (DBUtil.INSTANCE.getDataSource() == null) {
            return "没有连接数据库";

        }
        Connection connection = DBUtil.INSTANCE.getDataSource().getConnection();
        if (connection == null) {
            return "获取连接失败";
        }
        String databaseProductName = connection.getCatalog();
        connection.close();
        return databaseProductName;
    }

    @PostMapping("/set")
    public String setDbConfig(@RequestBody DbParam param) throws Exception {
        // 格式化json
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        curatorUtil.setDbConfig(param);

        return "SUCCESS";
    }
}
