package com.tanggo.fund.monitor;

import com.tanggo.fund.monitor.core.service.MetricCollectorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

@Slf4j
@SpringBootApplication
public class MonitorApplication {

    public static void main(String[] args) {
        try {
            log.info("========== 启动监控应用 ==========");

            // 加载 Spring XML 配置
            log.info("加载 Spring XML 配置: spring-ssh-cpu-monitor.xml");
            ApplicationContext context = new ClassPathXmlApplicationContext(
                    "spring-ssh-cpu-monitor.xml"
            );

            // 获取收集服务
            log.info("获取 MetricCollectorService Bean");
            MetricCollectorService service = context.getBean(MetricCollectorService.class);

            if (service != null) {
                log.info("服务实例化成功，开始执行监控任务");

                // 执行 SSH CPU 监控
                log.info("\n========== 执行 SSH CPU 监控 ==========");
                service.handleSshCpuMonitor();

                log.info("\n========== 执行 SSH 内存监控 ==========");
                service.handleSshMemoryMonitor();

                log.info("所有监控任务执行完成");
            } else {
                log.error("无法获取 MetricCollectorService Bean");
            }

            log.info("========== 监控应用执行完成 ==========");

        } catch (Exception e) {
            log.error("应用执行异常", e);
            System.exit(1);
        }
    }

}

