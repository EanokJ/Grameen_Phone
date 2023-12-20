package com.grameenphone.wipro.task_executor.config;

import com.grameenphone.wipro.task_executor.util.PropertyUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class FlexMFSDbConnectionPool {
    private static HikariConfig config = new HikariConfig();
    public static HikariDataSource dataSource;

    static {
        config.setJdbcUrl(PropertyUtil.getProperty("api_db_url"));
        config.setUsername(PropertyUtil.getProperty("api_db_username"));
        config.setPassword(PropertyUtil.getProperty("api_db_password"));
        config.setMinimumIdle(0);
        if(PropertyUtil.getProperty("api_db_pool_size") == null) {
            config.setMaximumPoolSize(5);
        }
        config.setConnectionTimeout(60000);
        config.setIdleTimeout(300000);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        dataSource = new HikariDataSource(config);
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private FlexMFSDbConnectionPool(){}
}
