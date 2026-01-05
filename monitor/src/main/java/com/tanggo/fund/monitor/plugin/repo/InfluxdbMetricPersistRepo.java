package com.tanggo.fund.monitor.plugin.repo;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.tanggo.fund.monitor.core.entity.Metric;
import com.tanggo.fund.monitor.core.extension.collector.MetricPersistRepo;
import lombok.extern.slf4j.Slf4j;
import java.time.ZoneId;

/**
 * InfluxDB数据库持久化实现
 * 将监控指标数据保存到InfluxDB
 */
@Slf4j
public class InfluxdbMetricPersistRepo implements MetricPersistRepo {

    private final InfluxDBClient influxDBClient;
    private final String bucket;
    private final String organization;

    /**
     * 构造函数
     *
     * @param influxDBClient InfluxDB客户端
     * @param bucket         InfluxDB桶名称
     * @param organization   InfluxDB组织名称
     */
    public InfluxdbMetricPersistRepo(InfluxDBClient influxDBClient, String bucket, String organization) {
        this.influxDBClient = influxDBClient;
        this.bucket = bucket;
        this.organization = organization;
    }

    /**
     * 将指标数据插入InfluxDB
     *
     * @param metric 待保存的指标数据
     */
    @Override
    public void insert(Metric metric) {
        try {
            // 将LocalDateTime转换为纳秒时间戳
            long timestamp = metric.getTimestamp()
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli() * 1_000_000;

            // 创建Point对象
            Point point = Point.measurement(metric.getMetricName())
                    .time(timestamp, WritePrecision.NS)
                    .addField("value", metric.getMetricValue());

            // 添加meterId作为tag
            if (metric.getMeterId() != null) {
                point.addTag("meterId", metric.getMeterId());
            }

            // 添加自定义标签
            if (metric.getTags() != null && !metric.getTags().isEmpty()) {
                metric.getTags().forEach(point::addTag);
            }

            // 写入数据
            WriteApi writeApi = influxDBClient.getWriteApi();
            writeApi.writePoint(bucket, organization, point);

            log.info("指标数据已保存到InfluxDB: metricName={}, value={}, timestamp={}",
                    metric.getMetricName(), metric.getMetricValue(), metric.getTimestamp());

        } catch (Exception e) {
            log.error("保存指标到InfluxDB异常: metricName={}, error={}",
                    metric.getMetricName(), e.getMessage(), e);
            throw new RuntimeException("InfluxDB操作失败", e);
        }
    }

    /**
     * 获取InfluxDB客户端
     *
     * @return InfluxDBClient
     */
    public InfluxDBClient getInfluxDBClient() {
        return influxDBClient;
    }

    /**
     * 获取桶名称
     *
     * @return 桶名称
     */
    public String getBucket() {
        return bucket;
    }

    /**
     * 获取组织名称
     *
     * @return 组织名称
     */
    public String getOrg() {
        return organization;
    }
}
