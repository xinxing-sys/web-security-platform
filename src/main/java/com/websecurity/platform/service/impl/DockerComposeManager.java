
package com.websecurity.platform.service.impl;

import com.websecurity.platform.config.DockerLabProperties;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import java.net.HttpURLConnection;
import java.net.URL;


@Component
public class DockerComposeManager {

    private final DockerLabProperties properties;

    public DockerComposeManager(DockerLabProperties properties) {
        this.properties = properties;
    }

    /**
     * 为指定任务创建实例目录和环境变量文件。
     */
    public Path prepareInstance(String projectName, int port) {

        validateProjectName(projectName);
        validatePort(port);

        try {
            Path instancesRoot = Paths.get(
                    properties.getInstancesDir()
            ).toAbsolutePath().normalize();

            Path instanceDir = instancesRoot.resolve(projectName)
                    .normalize();

            if (!instanceDir.startsWith(instancesRoot)) {
                throw new IllegalArgumentException(
                        "实例目录不合法"
                );
            }

            Files.createDirectories(instanceDir);

            Path envFile = instanceDir.resolve(".env");

            Files.writeString(
                    envFile,
                    "DVWA_HOST_PORT=" + port + System.lineSeparator(),
                    StandardCharsets.UTF_8
            );

            return envFile;

        } catch (IOException e) {
            throw new IllegalStateException(
                    "创建靶场实例配置失败：" + e.getMessage(),
                    e
            );
        }
    }

    /**
     * 检查端口是否可用。
     *
     * 注意：此检查不能完全消除并发抢占，
     * 后续启动失败时仍需要处理端口冲突。
     */
    public boolean isPortAvailable(int port) {

        try (ServerSocket socket = new ServerSocket()) {

            socket.setReuseAddress(false);

            socket.bind(
                    new InetSocketAddress("127.0.0.1", port)
            );

            return true;

        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 在配置范围内寻找可用端口。
     */
    public int findAvailablePort() {

        int start = properties.getPortStart();
        int end = properties.getPortEnd();

        if (start < 1024 || end > 65535
                || start > end) {
            throw new IllegalStateException(
                    "Docker端口范围配置不正确"
            );
        }

        for (int port = start; port <= end; port++) {

            if (isPortAvailable(port)) {
                return port;
            }
        }

        throw new IllegalStateException(
                "没有可用的DVWA端口，请检查端口范围"
        );
    }

    /**
     * 启动指定Docker Compose项目。
     */
    public String start(
            String projectName,
            Path envFile) {

        validateProjectName(projectName);

        Path composeFile = Paths.get(
                properties.getComposeFile()
        ).toAbsolutePath().normalize();

        Path normalizedEnvFile = envFile
                .toAbsolutePath()
                .normalize();

        if (!Files.isRegularFile(composeFile)) {
            throw new IllegalStateException(
                    "Docker Compose模板不存在"
            );
        }

        if (!Files.isRegularFile(normalizedEnvFile)) {
            throw new IllegalStateException(
                    "实例环境变量文件不存在"
            );
        }

        List<String> command = new ArrayList<>();

        command.add(properties.getDockerExecutable());
        command.add("compose");
        command.add("-p");
        command.add(projectName);
        command.add("-f");
        command.add(composeFile.toString());
        command.add("--env-file");
        command.add(normalizedEnvFile.toString());
        command.add("up");
        command.add("-d");

        return execute(command);
    }

    /**
     * 查询指定服务的容器ID。
     */
    public String getContainerId(
            String projectName,
            Path envFile,
            String serviceName) {

        if (!"dvwa".equals(serviceName)
                && !"db".equals(serviceName)) {
            throw new IllegalArgumentException(
                    "不支持的容器服务名称"
            );
        }

        List<String> command = composeCommand(
                projectName,
                envFile
        );

        command.add("ps");
        command.add("-q");
        command.add(serviceName);

        String output = execute(command).trim();

        if (output.isEmpty()) {
            return null;
        }

        String[] lines = output.split("\\R");

        return lines[0].trim();
    }

    /**
     * 停止指定Docker Compose项目。
     *
     * 不使用 -v，避免删除数据库卷。
     */
    public String stop(
            String projectName,
            Path envFile) {

        List<String> command = composeCommand(
                projectName,
                envFile
        );

        command.add("down");

        return execute(command);
    }

    /**
     * 检查容器是否处于运行状态。
     */
    public boolean isContainerRunning(String containerId) {

        if (containerId == null
                || !containerId.matches("[a-fA-F0-9]{12,64}")) {
            return false;
        }

        List<String> command = new ArrayList<>();

        command.add(properties.getDockerExecutable());
        command.add("inspect");
        command.add("-f");
        command.add("{{.State.Running}}");
        command.add(containerId);

        String output = execute(command).trim();

        return "true".equalsIgnoreCase(output);
    }

    /**
     * 等待Web容器启动。
     */
    public void waitForWebContainer(
            String containerId) {

        int timeout = properties.getStartTimeoutSeconds();

        if (timeout < 1) {
            timeout = 180;
        }

        long deadline = System.nanoTime()
                + TimeUnit.SECONDS.toNanos(timeout);

        while (System.nanoTime() < deadline) {

            if (isContainerRunning(containerId)) {
                return;
            }

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();

                throw new IllegalStateException(
                        "等待DVWA启动时被中断",
                        e
                );
            }
        }

        throw new IllegalStateException(
                "DVWA容器启动超时"
        );
    }

    /**
     * 构造Compose命令。
     */
    private List<String> composeCommand(
            String projectName,
            Path envFile) {

        validateProjectName(projectName);

        List<String> command = new ArrayList<>();

        command.add(properties.getDockerExecutable());
        command.add("compose");
        command.add("-p");
        command.add(projectName);
        command.add("-f");
        command.add(
                Paths.get(properties.getComposeFile())
                        .toAbsolutePath()
                        .normalize()
                        .toString()
        );
        command.add("--env-file");
        command.add(
                envFile.toAbsolutePath()
                        .normalize()
                        .toString()
        );

        return command;
    }

    /**
     * 执行系统命令并获取输出。
     */
    private String execute(List<String> command) {

        ProcessBuilder builder = new ProcessBuilder(command);

        builder.redirectErrorStream(true);

        try {
            Process process = builder.start();

            StringBuilder output = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            process.getInputStream(),
                            StandardCharsets.UTF_8
                    ))) {

                String line;

                while ((line = reader.readLine()) != null) {
                    output.append(line)
                            .append(System.lineSeparator());
                }
            }

            boolean finished = process.waitFor(
                    180,
                    TimeUnit.SECONDS
            );

            if (!finished) {
                process.destroyForcibly();

                throw new IllegalStateException(
                        "Docker命令执行超时"
                );
            }

            int exitCode = process.exitValue();

            if (exitCode != 0) {
                throw new IllegalStateException(
                        "Docker命令执行失败，退出码："
                                + exitCode
                                + "，输出："
                                + output
                );
            }

            return output.toString();

        } catch (IOException e) {
            throw new IllegalStateException(
                    "无法执行Docker命令，请检查Docker Desktop是否运行："
                            + e.getMessage(),
                    e
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Docker命令执行被中断",
                    e
            );
        }
    }

    private void validateProjectName(String projectName) {

        if (projectName == null
                || !projectName.matches("lab_task_[0-9]+")) {
            throw new IllegalArgumentException(
                    "Docker项目名称不合法"
            );
        }
    }

    private void validatePort(int port) {

        if (port < properties.getPortStart()
                || port > properties.getPortEnd()) {
            throw new IllegalArgumentException(
                    "端口超出允许范围"
            );
        }
    }



    /**
     * 检查DVWA网页是否可以正常响应。
     *
     * 这里只检查HTTP可访问性，不代表数据库已经初始化，
     * 也不代表DVWA登录认证已经完成。
     */
    public boolean isHttpReachable(String url) {

        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection)
                    new URL(url).openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.setInstanceFollowRedirects(true);

            int responseCode = connection.getResponseCode();

            return responseCode >= 200 && responseCode < 400;

        } catch (Exception e) {
            return false;

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 等待DVWA网页可以正常响应。
     */
    public void waitForHttp(String url) {

        int timeout = properties.getStartTimeoutSeconds();

        if (timeout < 1) {
            timeout = 180;
        }

        long deadline = System.nanoTime()
                + TimeUnit.SECONDS.toNanos(timeout);

        while (System.nanoTime() < deadline) {

            if (isHttpReachable(url)) {
                return;
            }

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();

                throw new IllegalStateException(
                        "等待DVWA网页启动时被中断",
                        e
                );
            }
        }

        throw new IllegalStateException(
                "DVWA网页启动超时，HTTP服务未能正常响应"
        );
    }



}
