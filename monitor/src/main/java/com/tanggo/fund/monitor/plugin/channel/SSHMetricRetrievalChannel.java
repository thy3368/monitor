package com.tanggo.fund.monitor.plugin.channel;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.tanggo.fund.monitor.core.extension.MetricRetrievalChannel;
import com.tanggo.fund.monitor.core.entity.meta.ChannelMeta;
import com.tanggo.fund.monitor.core.entity.meta.CommandMeta;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;

@Slf4j
public class SSHMetricRetrievalChannel implements MetricRetrievalChannel {

    private Session session;
    private String host;
    private int port;
    private String username;
    private String password;

    @Override
    public void connect(ChannelMeta channelMeta) {
        try {
            // 从扩展属性中获取SSH连接信息
            Map<String, Object> extensions = channelMeta.getExtensions();
            this.host = (String) extensions.get("host");
            Object portObj = extensions.getOrDefault("port", 22);
            this.port = portObj instanceof Number ? ((Number) portObj).intValue() : 22;
            this.username = (String) extensions.get("username");
            this.password = (String) extensions.get("password");

            JSch jsch = new JSch();
            session = jsch.getSession(username, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(30000);

            log.info("SSH连接成功: {}@{}:{}", username, host, port);
        } catch (JSchException e) {
            log.error("SSH连接失败: {}", e.getMessage(), e);
            throw new RuntimeException("SSH连接失败", e);
        }
    }

    @Override
    public String execute(CommandMeta cmd) {
        if (session == null || !session.isConnected()) {
            throw new RuntimeException("SSH会话未连接");
        }

        try {
            String command = cmd.getCommand();
            log.info("执行命令: {}", command);

            com.jcraft.jsch.Channel channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(channel.getInputStream())
            );

            channel.connect();

            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            channel.disconnect();
            log.info("命令执行成功");

            return output.toString();
        } catch (Exception e) {
            log.error("命令执行失败: {}", e.getMessage(), e);
            throw new RuntimeException("命令执行失败", e);
        }
    }

    public void disconnect() {
        if (session != null && session.isConnected()) {
            session.disconnect();
            log.info("SSH连接已断开");
        }
    }
}
