package nhz;

import nhz.http.API;
import nhz.peer.Peers;
import nhz.user.Users;
import nhz.util.Logger;
import nhz.util.ThreadPool;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class Nhz {

    public static final String VERSION = "NHZ V2";

    private static final Properties defaultProperties = new Properties();
    static {
        System.out.println("Initializing Nhz server version " + Nhz.VERSION);
        try (InputStream is = ClassLoader.getSystemResourceAsStream("nhz-default.properties")) {
            if (is != null) {
                Nhz.defaultProperties.load(is);
            } else {
                String configFile = System.getProperty("nhz-default.properties");
                if (configFile != null) {
                    try (InputStream fis = new FileInputStream(configFile)) {
                        Nhz.defaultProperties.load(fis);
                    } catch (IOException e) {
                        throw new RuntimeException("Error loading nhz-default.properties from " + configFile);
                    }
                } else {
                    throw new RuntimeException("nhz-default.properties not in classpath and system property nhz-default.properties not defined either");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error loading nhz-default.properties", e);
        }
    }
    private static final Properties properties = new Properties(defaultProperties);
    static {
        try (InputStream is = ClassLoader.getSystemResourceAsStream("nhz.properties")) {
            if (is != null) {
                Nhz.properties.load(is);
            } // ignore if missing
        } catch (IOException e) {
            throw new RuntimeException("Error loading nhz.properties", e);
        }
    }

    public static int getIntProperty(String name) {
        try {
            int result = Integer.parseInt(properties.getProperty(name));
            Logger.logMessage(name + " = \"" + result + "\"");
            return result;
        } catch (NumberFormatException e) {
            Logger.logMessage(name + " not defined, assuming 0");
            return 0;
        }
    }

    public static String getStringProperty(String name) {
        String value = properties.getProperty(name);
        if (value != null && ! "".equals(value = value.trim())) {
            Logger.logMessage(name + " = \"" + value + "\"");
            return value;
        } else {
            Logger.logMessage(name + " not defined, assuming null");
            return null;
        }
    }

    public static Boolean getBooleanProperty(String name) {
        String value = properties.getProperty(name);
        if (Boolean.TRUE.toString().equals(value)) {
            Logger.logMessage(name + " = \"true\"");
            return true;
        } else if (Boolean.FALSE.toString().equals(value)) {
            Logger.logMessage(name + " = \"false\"");
            return false;
        }
        Logger.logMessage(name + " not defined, assuming false");
        return false;
    }

    public static Blockchain getBlockchain() {
        return BlockchainImpl.getInstance();
    }

    public static BlockchainProcessor getBlockchainProcessor() {
        return BlockchainProcessorImpl.getInstance();
    }

    public static TransactionProcessor getTransactionProcessor() {
        return TransactionProcessorImpl.getInstance();
    }

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                Nhz.shutdown();
            }
        }));
        init();
    }

    public static void init(Properties customProperties) {
        properties.putAll(customProperties);
        init();
    }

    public static void init() {
        Init.init();
    }

    public static void shutdown() {
        Peers.shutdown();
        ThreadPool.shutdown();
        Db.shutdown();
        Logger.logMessage("Nhz server " + VERSION + " stopped.");
    }

    private static class Init {

        static {

            long startTime = System.currentTimeMillis();

            Logger.logMessage("logging enabled");

            if (! Nhz.getBooleanProperty("nhz.debugJetty")) {
                System.setProperty("org.eclipse.jetty.LEVEL", "OFF");
                Logger.logDebugMessage("jetty logging disabled");
            }

            Constants.init();
            Db.init();
            BlockchainProcessorImpl.getInstance();
            TransactionProcessorImpl.getInstance();
            Peers.init();
            Generator.init();
            API.init();
            Users.init();
            ThreadPool.start();

            long currentTime = System.currentTimeMillis();
            Logger.logDebugMessage("Initialization took " + (currentTime - startTime) / 1000 + " seconds");
            Logger.logMessage("Nhz server " + VERSION + " started successfully.");
            if (Constants.isTestnet) {
                Logger.logMessage("RUNNING ON TESTNET - DO NOT USE REAL ACCOUNTS!");
            }
        }

        private static void init() {}

        private Init() {} // never

    }

    private Nhz() {} // never

}
