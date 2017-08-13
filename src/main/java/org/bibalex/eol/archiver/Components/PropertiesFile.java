package org.bibalex.eol.archiver.Components;

import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties
public class PropertiesFile {

    private String basePath;
    private String tmpPath;
    private String proxyUserName;
    private String password;
    private String proxyExists;
    private String proxy;
    private String port;
    private int threadsCount;

    public int getThreadsCount() {
        return threadsCount;
    }

    public void setThreadsCount(int threadsCount) {
        this.threadsCount = threadsCount;
    }

    public String getProxyUserName() {
        return proxyUserName;
    }

    public void setProxyUserName(String userName) {
        this.proxyUserName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getProxyExists() {
        return proxyExists;
    }

    public void setProxyExists(String proxyExists) {
        this.proxyExists = proxyExists;
    }

    public String getProxy() {
        return proxy;
    }

    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getBasePath() {
        return basePath;
    }

    public String getTmpPath() {
        return tmpPath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public void setTmpPath(String tmpPath) {
        this.tmpPath = tmpPath;
    }

}
