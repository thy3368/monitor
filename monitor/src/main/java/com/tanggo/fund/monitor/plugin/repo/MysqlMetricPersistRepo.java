package com.tanggo.fund.monitor.plugin.repo;

import com.tanggo.fund.monitor.core.entity.Metric;
import com.tanggo.fund.monitor.core.extension.collector.MetricPersistRepo;
import lombok.extern.slf4j.Slf4j;
import java.sql.*;
import javax.sql.DataSource;

/**
 * MySQL数据库持久化实现
 * 将监控指标数据保存到MySQL数据库
 */
@Slf4j
public class MysqlMetricPersistRepo implements MetricPersistRepo {

    private DataSource dataSource;

    public MysqlMetricPersistRepo(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 将指标数据插入数据库
     *
     * @param metric 待保存的指标数据
     */
    @Override
    public void insert(Metric metric) {
        String sql = "INSERT INTO metrics (metric_name, metric_value, timestamp, tags) VALUES (?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, metric.getMetricName());
            pstmt.setDouble(2, metric.getMetricValue());
            pstmt.setTimestamp(3, Timestamp.valueOf(metric.getTimestamp()));

            // 将标签转换为JSON字符串
            String tagsJson = metric.getTags() != null
                ? metric.getTags().toString()
                : "{}";
            pstmt.setString(4, tagsJson);

            int result = pstmt.executeUpdate();

            if (result > 0) {
                log.info("指标数据已保存到数据库: metricName={}, value={}",
                    metric.getMetricName(), metric.getMetricValue());
            } else {
                log.warn("指标数据保存失败: metricName={}", metric.getMetricName());
            }

        } catch (SQLException e) {
            log.error("保存指标到数据库异常: {}", e.getMessage(), e);
            throw new RuntimeException("数据库操作失败", e);
        }
    }

    /**
     * 获取数据源
     *
     * @return DataSource
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * 设置数据源
     *
     * @param dataSource 数据源
     */
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
}
