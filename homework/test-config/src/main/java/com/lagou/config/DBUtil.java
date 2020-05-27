package com.lagou.config;

import com.alibaba.druid.pool.DruidDataSource;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.BeanUtils;

import javax.sql.DataSource;
import java.sql.SQLException;

@Setter
@Getter
public class DBUtil {
    public static DBUtil INSTANCE = new DBUtil();

    private DBUtil() {

    }

    private DataSource dataSource;

    // 当节点发生变化是调用此方法重新构建数据源
    public void buildDataource(DbParam dbParam) {
        // 销毁
        if (dataSource != null) {
            ((DruidDataSource) dataSource).close();
            dataSource = null;
        }
        // 创建
        this.dataSource = new DruidDataSource();
        BeanUtils.copyProperties(dbParam, this.dataSource);
        try {
            ((DruidDataSource) dataSource).init();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }


}
