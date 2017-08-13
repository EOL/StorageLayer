package model;

/**
 * Created by hduser on 7/30/17.
 */
public class BA_Proxy {

    private String userName;
    private String password;
    private boolean proxyExists;
    private String proxy;
    private String port;


    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public boolean isProxyExists() {
        return proxyExists;
    }

    public void setProxyExists(boolean proxyExists) {
        this.proxyExists = proxyExists;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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
}
