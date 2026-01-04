package com.tanggo.fund.monitor.plugin.calculator;

import com.tanggo.fund.monitor.core.entity.Metric;
import com.tanggo.fund.monitor.core.entity.MetricCalculator;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Free命令内存使用率解析器
 * 解析free命令输出，提取内存使用率指标
 */
@Slf4j
public class FreeMemoryMetricCalculator implements MetricCalculator {

    @Override
    public Metric calculat(String freeOutput) {
        try {
            log.debug("解析free命令输出: {}", freeOutput);
            Map<String, Double> metrics = parseMemoryOutput(freeOutput);

            Metric metric = new Metric();
            metric.setMetricName("memory_usage");
            metric.setMetricValue(metrics.getOrDefault("memory_usage_percent", 0.0));
            metric.setTimestamp(LocalDateTime.now());

            Map<String, String> tags = new HashMap<>();
            metrics.forEach((key, value) -> tags.put(key, String.format("%.2f", value)));
            metric.setTags(tags);

            log.info("解析成功，内存使用率: {}%", metrics.get("memory_usage_percent"));
            return metric;
        } catch (Exception e) {
            log.error("解析free输出失败: {}", e.getMessage(), e);

            Metric metric = new Metric();
            metric.setMetricName("memory_usage");
            metric.setMetricValue(-1);
            metric.setTimestamp(LocalDateTime.now());

            Map<String, String> tags = new HashMap<>();
            tags.put("error", e.getMessage());
            metric.setTags(tags);

            return metric;
        }
    }

    /**
     * 解析free命令输出
     * 从free -h或free -m的输出中提取内存使用率等指标
     *
     * @param freeOutput free命令的标准输出
     * @return 包含指标数据的Map
     */
    private Map<String, Double> parseMemoryOutput(String freeOutput) {
        Map<String, Double> metrics = new HashMap<>();

        String[] lines = freeOutput.split("\n");

        for (String line : lines) {
            // 查找包含Mem的行
            if (line.trim().startsWith("Mem:")) {
                log.debug("解析内存行: {}", line);
                // 示例输出:
                // Mem:       16384000     8192000     5120000     3072000
                // 或
                // Mem:        16Gi       8.0Gi       4.9Gi       3.0Gi

                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 4) {
                    try {
                        // 提取数值部分（去掉单位）
                        double total = extractNumericValue(parts[1]);
                        double used = extractNumericValue(parts[2]);

                        if (total > 0) {
                            double usagePercent = (used / total) * 100;
                            metrics.put("memory_usage_percent", usagePercent);
                            metrics.put("memory_total", total);
                            metrics.put("memory_used", used);
                            metrics.put("memory_free", extractNumericValue(parts[3]));
                        }
                    } catch (NumberFormatException e) {
                        log.warn("解析内存数值失败: {}", line, e);
                    }
                }
            }

            // 处理 /dev/shm 或其他挂载点的行
            if (line.trim().startsWith("/dev/") || line.trim().startsWith("tmpfs")) {
                log.debug("解析挂载点行: {}", line);
            }
        }

        // 如果没有找到内存信息，返回默认值
        if (!metrics.containsKey("memory_usage_percent")) {
            metrics.put("memory_usage_percent", 0.0);
        }

        return metrics;
    }

    /**
     * 提取数值部分，去掉单位
     *
     * @param value 包含单位的字符串，如 "16Gi", "8192", "1.5G"
     * @return 提取到的数值
     */
    private double extractNumericValue(String value) {
        if (value == null || value.isEmpty()) {
            return 0.0;
        }

        // 提取纯数字部分
        Pattern pattern = Pattern.compile("([\\d.]+)");
        Matcher matcher = pattern.matcher(value);

        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        return 0.0;
    }
}
