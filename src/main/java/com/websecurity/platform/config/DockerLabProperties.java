
package com.websecurity.platform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DockerLabProperties {

    @Value("${app.docker.executable}")
    private String dockerExecutable;

    @Value("${app.docker.compose-file}")
    private String composeFile;

    @Value("${app.docker.instances-dir}")
    private String instancesDir;

    @Value("${app.docker.port-start:4292}")
    private Integer portStart;

    @Value("${app.docker.port-end:4399}")
    private Integer portEnd;

    @Value("${app.docker.start-timeout-seconds:180}")
    private Integer startTimeoutSeconds;

    public String getDockerExecutable() {
        return dockerExecutable;
    }

    public String getComposeFile() {
        return composeFile;
    }

    public String getInstancesDir() {
        return instancesDir;
    }

    public Integer getPortStart() {
        return portStart;
    }

    public Integer getPortEnd() {
        return portEnd;
    }

    public Integer getStartTimeoutSeconds() {
        return startTimeoutSeconds;
    }
}
