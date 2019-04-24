package org.webswing;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;
import org.webswing.toolkit.util.Logger;

public class ServerMain {

    public static boolean stop = false;
    public static String startBat = "";
    public static Server server = null;
    public static boolean restart = false;
	private static final String JMS_BASE_STR = "nio://127.0.0.1:";

    public static void main(String[] args) throws Exception {
        Configuration config = ConfigurationImpl.parse(args);
        System.out.println(config.toString());
        System.setProperty(Constants.SERVER_EMBEDED_FLAG, "true");
        System.setProperty(Constants.SERVER_PORT, config.getHttpPort());
        System.setProperty(Constants.SERVER_HOST, config.getHost());
        if (null != config.getJmsUrl()) {
        	System.setProperty(Constants.JMS_URL, JMS_BASE_STR + config.getJmsUrl());
        }
        if (config.getConfigFile() != null) {
            File configFile = new File(config.getConfigFile());
            if (configFile.exists()) {
                System.setProperty(Constants.CONFIG_FILE_PATH, configFile.toURI().toString());
            } else {
                Logger.error(
                        "Webswing configuration file " + config.getConfigFile() + " not found. Using default location.");
            }
        }
        if (config.getUsersFile() != null) {
            File usersFile = new File(config.getUsersFile());
            if (usersFile.exists()) {
                System.setProperty(Constants.USER_FILE_PATH, usersFile.toURI().toString());
            } else {
                Logger.error(
                        "Webswing users property file " + config.getUsersFile() + " not found. Using default location.");
            }
        }

        if (config.getAllowedCorsOrigins() != null) {
            System.setProperty(Constants.ALLOWED_CORS_ORIGINS, config.getAllowedCorsOrigins());
        }

        server = new Server();
        List<Connector> connectors = new ArrayList<Connector>();
        if (config.isHttp()) {
        	HttpConfiguration http_config = new HttpConfiguration();
        	http_config.setSendServerVersion(false);
        	if (config.isHttps()) {
        		http_config.setSecurePort(Integer.parseInt(config.getHttpPort()));
        	}
        	ServerConnector http = new ServerConnector(server,new HttpConnectionFactory(http_config));
            http.setPort(Integer.parseInt(config.getHttpPort()));
            http.setHost(config.getHost());
            connectors.add(http);
        }
        if (config.isHttps()) {
            if (config.isHttps() && config.getTruststore() != null && !config.getTruststore().isEmpty() && config.getKeystore() != null && config.getKeystore().isEmpty()) {
                Logger.error(
                        "SSL configuration is invalid. Please specify the location of truststore and keystore files.");
            } else {
                if (!new File(config.getTruststore()).exists()) {
                    Logger.error("SSL configuration is invalid. Truststore file " + new File(
                            config.getTruststore()).getAbsolutePath() + " does not exist.");
                } else if (!new File(config.getKeystore()).exists()) {
                    Logger.error("SSL configuration is invalid. Keystore file " + new File(
                            config.getKeystore()).getAbsolutePath() + " does not exist.");
                } else {
                    SslContextFactory sslContextFactory = new SslContextFactory();
                    sslContextFactory.setKeyStorePath(config.getKeystore());
                    sslContextFactory.setKeyStorePassword(config.getKeystorePassword());
                    sslContextFactory.setTrustStorePath(config.getTruststore());
                    sslContextFactory.setTrustStorePassword(config.getTruststorePassword());
                    sslContextFactory.setNeedClientAuth(false);
                    sslContextFactory.addExcludeProtocols("SSLv3", "SSLv2Hello", "TLSv1");
                    
                    HttpConfiguration https_config = new HttpConfiguration();
                    https_config.setSendServerVersion(false);
                    SecureRequestCustomizer src = new SecureRequestCustomizer();
                    https_config.addCustomizer(src);
                    
                    ServerConnector https = new ServerConnector(server,sslContextFactory,new HttpConnectionFactory(https_config));
                    https.setPort(Integer.parseInt(config.getHttpsPort()));
                    https.setHost(config.getHost());
                    connectors.add(https);
                }
            }
        }

        server.setConnectors(connectors.toArray(new Connector[connectors.size()]));

        // enable jmx
//		MBeanContainer mbcontainer = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
//		server.getContainer().addEventListener(mbcontainer);
//		server.addBean(mbcontainer);
        // mbcontainer.addBean(Log.getLog());

        final WebAppContext webapp = new WebAppContext();
        webapp.setContextPath("/");
        webapp.setWar(System.getProperty(Constants.WAR_FILE_LOCATION));
        webapp.setTempDirectory(new File(URI.create(System.getProperty(Constants.TEMP_DIR_PATH))));
        webapp.setPersistTempDirectory(true);
        server.setHandler(webapp);
        server.start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(5000);
                        if (stop) {
                            Logger.error("stop....");
                            server.stop();
                            Logger.error("stopped");
                            break;
                        } else {
                            Logger.warn("not stop .....");
                        }
                        if(restart){
                            restart = false;
                            Logger.info("about to restarting proxy");
                            Logger.info("stopping proxy");
                            server.stop();
                            server.start();
                            Logger.info("start proxy finished");
                        }

                    } catch (Exception e) {
                        Logger.error("stop or restart server catch exception ", e);
                        restart = false;
                    }
                }
            }
        }, "stop-jetty-thread").start();

        server.join();
        if (stop) {
            Logger.error("jetty server is stopped, try to start new one");
            Runtime.getRuntime().exec(startBat);
            Logger.error("new server is starting, this one is waiting to exit");
            Thread.sleep(10000);
            System.exit(0);
        }
    }

}
