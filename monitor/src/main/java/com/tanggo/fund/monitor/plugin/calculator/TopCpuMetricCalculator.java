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
 * Top命令CPU使用率解析器
 * 解析top命令输出，提取CPU使用率指标
 */
@Slf4j
public class TopCpuMetricCalculator implements MetricCalculator {

    @Override
    public Metric calculat(String topOutput) {
        try {
            log.debug("解析top命令输出: {}", topOutput);
            Map<String, Double> metrics = parseTopOutput(topOutput);

            Metric metric = new Metric();
            metric.setMetricName("cpu_usage");
            metric.setMetricValue(metrics.getOrDefault("cpu_usage_percent", 0.0));
            metric.setTimestamp(LocalDateTime.now());

            Map<String, String> tags = new HashMap<>();
            metrics.forEach((key, value) -> tags.put(key, String.format("%.2f", value)));
            metric.setTags(tags);

            log.info("解析成功，CPU使用率: {}%", metrics.get("cpu_usage_percent"));
            return metric;
        } catch (Exception e) {
            log.error("解析top输出失败: {}", e.getMessage(), e);

            Metric metric = new Metric();
            metric.setMetricName("cpu_usage");
            metric.setMetricValue(-1);
            metric.setTimestamp(LocalDateTime.now());

            Map<String, String> tags = new HashMap<>();
            tags.put("error", e.getMessage());
            metric.setTags(tags);

            return metric;
        }
    }

    /**
     * 解析top命令输出
     * 从top输出中提取CPU使用率等指标
     *
     * @param topOutput top命令的标准输出
     * @return 包含指标数据的Map
     */
    private Map<String, Double> parseTopOutput(String topOutput) {
        Map<String, Double> metrics = new HashMap<>();

        String[] lines = topOutput.split("\n");

        for (String line : lines) {
            // 查找包含CPU信息的行
            if (line.contains("%Cpu")) {
                // 示例: %Cpu(s): 12.5 us, 8.3 sy, 0.0 ni, 78.9 id, 0.3 wa, 0.0 hi, 0.0 si, 0.0 st
                double userCpu = extractValue(line, "us");
                double sysCpu = extractValue(line, "sy");
                double idleCpu = extractValue(line, "id");

                double totalCpu = userCpu + sysCpu;
                metrics.put("cpu_usage_percent", totalCpu);
                metrics.put("cpu_user_percent", userCpu);
                metrics.put("cpu_system_percent", sysCpu);
                metrics.put("cpu_idle_percent", idleCpu);
            }

            // 查找包含内存信息的行
            if (line.contains("KiB Mem")) {
                // 示例: KiB Mem : 16384000 total, 8192000 free, 5120000 used, 3072000 buff/cache
                double totalMem = extractMemValue(line, "total");
                double usedMem = extractMemValue(line, "used");

                double memUsagePercent = (usedMem / totalMem) * 100;
                metrics.put("memory_usage_percent", memUsagePercent);
                metrics.put("memory_total_kb", totalMem);
                metrics.put("memory_used_kb", usedMem);
            }
        }

        // 如果没有找到CPU信息，尝试解析其他格式
        if (!metrics.containsKey("cpu_usage_percent")) {
            metrics.put("cpu_usage_percent", 0.0);
        }

        return metrics;
    }

    /**
     * 提取CPU指标值
     *
     * @param line 包含CPU数据的行
     * @param key 指标键（如: us, sy, id）
     * @return 提取到的数值
     */
    private double extractValue(String line, String key) {
        // 使用正则表达式匹配 "12.5 us" 类型的模式
        Pattern pattern = Pattern.compile("([\\d.]+)\\s+" + key);
        Matcher matcher = pattern.matcher(line);

        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        return 0.0;
    }

    /**
     * 提取内存指标值
     *
     * @param line 包含内存数据的行
     * @param key 指标键（如: total, used, free）
     * @return 提取到的数值（KB）
     */
    private double extractMemValue(String line, String key) {
        // 使用正则表达式匹配 "16384000 total" 类型的模式
        Pattern pattern = Pattern.compile("(\\d+)\\s+" + key);
        Matcher matcher = pattern.matcher(line);

        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        return 0.0;
    }
}
