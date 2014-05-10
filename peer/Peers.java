package nhz.peer;

import nhz.Account;
import nhz.Constants;
import nhz.Nhz;
import nhz.util.JSON;
import nhz.util.Listener;
import nhz.util.Listeners;
import nhz.util.Logger;
import nhz.util.ThreadPool;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.FilterMapping;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlets.DoSFilter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

public final class Peers {

    public static enum Event {
        BLACKLIST, UNBLACKLIST, DEACTIVATE, REMOVE,
        DOWNLOADED_VOLUME, UPLOADED_VOLUME, WEIGHT,
        ADDED_ACTIVE_PEER, CHANGED_ACTIVE_PEER,
        NEW_PEER
    }

    static final int LOGGING_MASK_EXCEPTIONS = 1;
    static final int LOGGING_MASK_NON200_RESPONSES = 2;
    static final int LOGGING_MASK_200_RESPONSES = 4;
    static final int communicationLoggingMask;

    static final Set<String> wellKnownPeers;
    static final Set<String> knownBlacklistedPeers;

    static final int connectTimeout;
    static final int readTimeout;
    static final int blacklistingPeriod;

    static final int DEFAULT_PEER_PORT = 7774;
    static final int TESTNET_PEER_PORT = 6774;
    private static final String myPlatform;
    private static final String myAddress;
    private static final int myPeerServerPort;
    private static final String myHallmark;
    private static final boolean shareMyAddress;
    private static final int maxNumberOfConnectedPublicPeers;
    private static final boolean enableHallmarkProtection;
    private static final int pushThreshold;
    private static final int pullThreshold;
    private static final int sendToPeersLimit;

    static final JSONStreamAware myPeerInfoRequest;
    static final JSONStreamAware myPeerInfoResponse;

    private static final Listeners<Peer,Event> listeners = new Listeners<>();

    private static final ConcurrentMap<String, PeerImpl> peers = new ConcurrentHashMap<>();

    static final Collection<PeerImpl> allPeers = Collections.unmodifiableCollection(peers.values());

    private static final ExecutorService sendToPeersService = Executors.newFixedThreadPool(10);

    static {

        myPlatform = Nhz.getStringProperty("nhz.myPlatform");
        myAddress = Nhz.getStringProperty("nhz.myAddress");
        if (myAddress != null && myAddress.endsWith(":" + TESTNET_PEER_PORT) && ! Constants.isTestnet) {
            throw new RuntimeException("Port " + TESTNET_PEER_PORT + " should only be used for testnet!!!");
        }
        myPeerServerPort = Nhz.getIntProperty("nhz.peerServerPort");
        if (myPeerServerPort == TESTNET_PEER_PORT && ! Constants.isTestnet) {
            throw new RuntimeException("Port " + TESTNET_PEER_PORT + " should only be used for testnet!!!");
        }
        shareMyAddress = Nhz.getBooleanProperty("nhz.shareMyAddress");
        myHallmark = Nhz.getStringProperty("nhz.myHallmark");
        if (Peers.myHallmark != null && Peers.myHallmark.length() > 0) {
            try {
                Hallmark hallmark = Hallmark.parseHallmark(Peers.myHallmark);
                if (! hallmark.isValid() || myAddress == null) {
                    throw new RuntimeException();
                }
                URI uri = new URI("http://" + myAddress.trim());
                String host = uri.getHost();
                if (! hallmark.getHost().equals(host)) {
                    throw new RuntimeException();
                }
            } catch (RuntimeException|URISyntaxException e) {
                Logger.logMessage("Your hallmark is invalid: " + Peers.myHallmark + " for your address: " + myAddress);
                throw new RuntimeException(e.toString(), e);
            }
        }

        JSONObject json = new JSONObject();
        if (Peers.myAddress != null && Peers.myAddress.length() > 0) {
            if (! Constants.isTestnet) {
                if (Peers.myAddress.indexOf(':') > 0) {
                    json.put("announcedAddress", Peers.myAddress);
                } else {
                    json.put("announcedAddress", Peers.myAddress + (Peers.myPeerServerPort != Peers.DEFAULT_PEER_PORT ? ":" + Peers.myPeerServerPort : ""));
                }
            } else {
                json.put("announcedAddress", Peers.myAddress.split(":")[0]);
            }
        }
        if (Peers.myHallmark != null && Peers.myHallmark.length() > 0) {
            json.put("hallmark", Peers.myHallmark);
        }
        json.put("application", "NRS");
        json.put("version", Nhz.VERSION);
        json.put("platform", Peers.myPlatform);
        json.put("shareAddress", Peers.shareMyAddress);
        Logger.logDebugMessage("My peer info:\n" + json.toJSONString());
        myPeerInfoResponse = JSON.prepare(json);
        json.put("requestType", "getInfo");
        myPeerInfoRequest = JSON.prepareRequest(json);

        Set<String> addresses = new HashSet<>();
        String wellKnownPeersString = Constants.isTestnet ? Nhz.getStringProperty("nhz.testnetPeers") : Nhz.getStringProperty("nhz.wellKnownPeers");
        if (wellKnownPeersString != null && wellKnownPeersString.length() > 0) {
            for (String address : wellKnownPeersString.split(";")) {
                address = address.trim();
                if (address.length() > 0) {
                    addresses.add(address);
                }
            }
        } else if (! Constants.isTestnet) {
            Logger.logMessage("No wellKnownPeers defined, using pre-defined one");
            //snxt.nhzcrypto.org;horizon.nhzcrypto.org;digital.nhzcrypto.org;5.45.97.233;85.25.67.39;82.118.242.244;37.187.237.56;37.187.237.211;5.45.97.233;137.117.84.21;54.193.59.188;188.226.217.137;85.25.67.39;198.50.146.167;128.199.197.56;54.186.81.151;188.226.207.183;85.214.65.220;168.63.251.208;80.241.220.178;23.97.166.145;107.170.116.134;37.187.237.150;198.211.122.85;31.19.188.145;37.187.237.114;37.187.225.106;191.235.134.92;37.187.18.97;146.185.168.63;pool.nhzcrypto.org;horizon.nhzcrypto.org
			addresses.add("snxt.nhzcrypto.org");
			addresses.add("horizon.nhzcrypto.org");
			addresses.add("digital.nhzcrypto.org");	
			addresses.add("5.45.97.233");
			addresses.add("85.25.67.39");
			addresses.add("82.118.242.244");
			addresses.add("37.187.237.56");
			addresses.add("37.187.237.211");
			addresses.add("5.45.97.233");
			addresses.add("137.117.84.21");
			addresses.add("54.193.59.188");
			addresses.add("188.226.217.137");
			addresses.add("85.25.67.39");
			addresses.add("198.50.146.167");
			addresses.add("128.199.197.56");
			addresses.add("54.186.81.151");
			addresses.add("188.226.207.183");
			addresses.add("85.214.65.220");
			addresses.add("168.63.251.208");
			addresses.add("80.241.220.178");
			addresses.add("23.97.166.145");
			addresses.add("107.170.116.134");
			addresses.add("37.187.237.150");
			addresses.add("198.211.122.85");
			addresses.add("31.19.188.145");
			addresses.add("37.187.237.114");
			addresses.add("37.187.225.106");
			addresses.add("191.235.134.92");
			addresses.add("37.187.18.97");
			addresses.add("146.185.168.63");  
			addresses.add("pool.nhzcrypto.org");
			addresses.add("horizon.nhzcrypto.org");
        }
        wellKnownPeers = Collections.unmodifiableSet(addresses);

        Set<String> blacklistedAddresses = new HashSet<>();
        String knownBlacklistedPeersString = Nhz.getStringProperty("nhz.knownBlacklistedPeers");
        if (knownBlacklistedPeersString != null && knownBlacklistedPeersString.length() > 0) {
            for (String address : knownBlacklistedPeersString.split(";")) {
                address = address.trim();
                if (address.length() > 0) {
                    blacklistedAddresses.add(address);
                }
            }
        }
        knownBlacklistedPeers = Collections.unmodifiableSet(blacklistedAddresses);

        maxNumberOfConnectedPublicPeers = Nhz.getIntProperty("nhz.maxNumberOfConnectedPublicPeers");
        connectTimeout = Nhz.getIntProperty("nhz.connectTimeout");
        readTimeout = Nhz.getIntProperty("nhz.readTimeout");
        enableHallmarkProtection = Nhz.getBooleanProperty("nhz.enableHallmarkProtection");
        pushThreshold = Nhz.getIntProperty("nhz.pushThreshold");
        pullThreshold = Nhz.getIntProperty("nhz.pullThreshold");

        blacklistingPeriod = Nhz.getIntProperty("nhz.blacklistingPeriod");
        communicationLoggingMask = Nhz.getIntProperty("nhz.communicationLoggingMask");
        sendToPeersLimit = Nhz.getIntProperty("nhz.sendToPeersLimit");

        StringBuilder buf = new StringBuilder();
        for (String address : wellKnownPeers) {
            Peer peer = Peers.addPeer(address);
            if (peer != null) {
                buf.append(peer.getPeerAddress()).append("; ");
            }
        }
        Logger.logDebugMessage("Well known peers: " + buf.toString());

    }

    private static class Init {

        static {
            if (Peers.shareMyAddress) {
                final Server peerServer = new Server();
                ServerConnector connector = new ServerConnector(peerServer);
                final int port = Constants.isTestnet ? TESTNET_PEER_PORT : Peers.myPeerServerPort;
                connector.setPort(port);
                final String host = Nhz.getStringProperty("nhz.peerServerHost");
                connector.setHost(host);
                connector.setIdleTimeout(Nhz.getIntProperty("nhz.peerServerIdleTimeout"));
                peerServer.addConnector(connector);

                ServletHandler peerHandler = new ServletHandler();
                peerHandler.addServletWithMapping(PeerServlet.class, "/*");
                if (Nhz.getBooleanProperty("nhz.enablePeerServerDoSFilter")) {
                    FilterHolder filterHolder = peerHandler.addFilterWithMapping(DoSFilter.class, "/*", FilterMapping.DEFAULT);
                    filterHolder.setInitParameter("maxRequestsPerSec", Nhz.getStringProperty("nhz.peerServerDoSFilter.maxRequestsPerSec"));
                    filterHolder.setInitParameter("delayMs", Nhz.getStringProperty("nhz.peerServerDoSFilter.delayMs"));
                    filterHolder.setInitParameter("maxRequestMs", Nhz.getStringProperty("nhz.peerServerDoSFilter.maxRequestMs"));
                    filterHolder.setInitParameter("trackSessions", "false");
                    filterHolder.setAsyncSupported(true);
                }

                peerServer.setHandler(peerHandler);
                peerServer.setStopAtShutdown(true);
                ThreadPool.runBeforeStart(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            peerServer.start();
                            Logger.logMessage("Started peer networking server at " + host + ":" + port);
                        } catch (Exception e) {
                            Logger.logDebugMessage("Failed to start peer networking server", e);
                            throw new RuntimeException(e.toString(), e);
                        }
                    }
                });
            } else {
                Logger.logMessage("shareMyAddress is disabled, will not start peer networking server");
            }
        }

        private static void init() {}

        private Init() {}

    }

    private static final Runnable peerUnBlacklistingThread = new Runnable() {

        @Override
        public void run() {

            try {
                try {

                    long curTime = System.currentTimeMillis();
                    for (PeerImpl peer : peers.values()) {
                        peer.updateBlacklistedStatus(curTime);
                    }

                } catch (Exception e) {
                    Logger.logDebugMessage("Error un-blacklisting peer", e);
                }
            } catch (Throwable t) {
                Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
                t.printStackTrace();
                System.exit(1);
            }

        }

    };

    private static final Runnable peerConnectingThread = new Runnable() {

        @Override
        public void run() {

            try {
                try {

                    if (getNumberOfConnectedPublicPeers() < Peers.maxNumberOfConnectedPublicPeers) {
                        PeerImpl peer = (PeerImpl)getAnyPeer(ThreadLocalRandom.current().nextInt(2) == 0 ? Peer.State.NON_CONNECTED : Peer.State.DISCONNECTED, false);
                        if (peer != null) {
                            peer.connect();
                        }
                    }

                } catch (Exception e) {
                    Logger.logDebugMessage("Error connecting to peer", e);
                }
            } catch (Throwable t) {
                Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
                t.printStackTrace();
                System.exit(1);
            }

        }

    };

    private static final Runnable getMorePeersThread = new Runnable() {

        private final JSONStreamAware getPeersRequest;
        {
            JSONObject request = new JSONObject();
            request.put("requestType", "getPeers");
            getPeersRequest = JSON.prepareRequest(request);
        }

        @Override
        public void run() {

            try {
                try {

                    Peer peer = getAnyPeer(Peer.State.CONNECTED, true);
                    if (peer == null) {
                        return;
                    }
                    JSONObject response = peer.send(getPeersRequest);
                    if (response == null) {
                        return;
                    }
                    JSONArray peers = (JSONArray)response.get("peers");
                    if (peers == null) {
                        return;
                    }
                    for (Object announcedAddress : peers) {
                        addPeer((String) announcedAddress);
                    }

                } catch (Exception e) {
                    Logger.logDebugMessage("Error requesting peers from a peer", e);
                }
            } catch (Throwable t) {
                Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
                t.printStackTrace();
                System.exit(1);
            }

        }

    };

    static {
        Account.addListener(new Listener<Account>() {
            @Override
            public void notify(Account account) {
                for (PeerImpl peer : Peers.peers.values()) {
                    if (peer.getHallmark() != null && peer.getHallmark().getAccountId().equals(account.getId())) {
                        Peers.listeners.notify(peer, Peers.Event.WEIGHT);
                    }
                }
            }
        }, Account.Event.BALANCE);
    }

    static {
        ThreadPool.scheduleThread(Peers.peerConnectingThread, 5);
        ThreadPool.scheduleThread(Peers.peerUnBlacklistingThread, 1);
        ThreadPool.scheduleThread(Peers.getMorePeersThread, 5);
    }

    public static void init() {
        Init.init();
    }

    public static void shutdown() {
        ThreadPool.shutdownExecutor(sendToPeersService);
    }

    public static boolean addListener(Listener<Peer> listener, Event eventType) {
        return Peers.listeners.addListener(listener, eventType);
    }

    public static boolean removeListener(Listener<Peer> listener, Event eventType) {
        return Peers.listeners.removeListener(listener, eventType);
    }

    static void notifyListeners(Peer peer, Event eventType) {
        Peers.listeners.notify(peer, eventType);
    }

    public static Collection<? extends Peer> getAllPeers() {
        return allPeers;
    }

    public static Peer getPeer(String peerAddress) {
        return peers.get(peerAddress);
    }

    public static Peer addPeer(String announcedAddress) {
        if (announcedAddress == null) {
            return null;
        }
        try {
            URI uri = new URI("http://" + announcedAddress.trim());
            String host = uri.getHost();
            InetAddress inetAddress = InetAddress.getByName(host);
            return addPeer(inetAddress.getHostAddress(), announcedAddress);
        } catch (URISyntaxException | UnknownHostException e) {
            Logger.logDebugMessage("Invalid peer address: " + announcedAddress + ", " + e.toString());
            return null;
        }
    }

    static PeerImpl addPeer(final String address, final String announcedAddress) {

        String peerAddress = normalizeHostAndPort(address);
        if (peerAddress == null) {
            return null;
        }

        String announcedPeerAddress = normalizeHostAndPort(announcedAddress);

        if (Peers.myAddress != null && Peers.myAddress.length() > 0 && Peers.myAddress.equalsIgnoreCase(announcedPeerAddress)) {
            return null;
        }

        PeerImpl peer = peers.get(peerAddress);
        if (peer == null) {
            peer = new PeerImpl(peerAddress, announcedPeerAddress);
            if (Constants.isTestnet && peer.getPort() > 0 && peer.getPort() != TESTNET_PEER_PORT) {
                Logger.logDebugMessage("Peer " + peerAddress + " on testnet is not using port " + TESTNET_PEER_PORT + ", ignoring");
                return null;
            }
            peers.put(peerAddress, peer);
            listeners.notify(peer, Event.NEW_PEER);
        }

        return peer;
    }

    static PeerImpl removePeer(PeerImpl peer) {
        return peers.remove(peer.getPeerAddress());
    }

    public static void sendToSomePeers(final JSONObject request) {

        final JSONStreamAware jsonRequest = JSON.prepareRequest(request);

        int successful = 0;
        List<Future<JSONObject>> expectedResponses = new ArrayList<>();
        for (final Peer peer : peers.values()) {

            if (Peers.enableHallmarkProtection && peer.getWeight() < Peers.pushThreshold) {
                continue;
            }

            if (! peer.isBlacklisted() && peer.getState() == Peer.State.CONNECTED && peer.getAnnouncedAddress() != null) {
                Future<JSONObject> futureResponse = sendToPeersService.submit(new Callable<JSONObject>() {
                    @Override
                    public JSONObject call() {
                        return peer.send(jsonRequest);
                    }
                });
                expectedResponses.add(futureResponse);
            }
            if (expectedResponses.size() >= Peers.sendToPeersLimit - successful) {
                for (Future<JSONObject> future : expectedResponses) {
                    try {
                        JSONObject response = future.get();
                        if (response != null && response.get("error") == null) {
                            successful += 1;
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException e) {
                        Logger.logDebugMessage("Error in sendToSomePeers", e);
                    }

                }
                expectedResponses.clear();
            }
            if (successful >= Peers.sendToPeersLimit) {
                return;
            }

        }

    }

    public static Peer getAnyPeer(Peer.State state, boolean applyPullThreshold) {

        List<Peer> selectedPeers = new ArrayList<>();
        for (Peer peer : peers.values()) {
            if (! peer.isBlacklisted() && peer.getState() == state && peer.shareAddress()
                    && (!applyPullThreshold || ! Peers.enableHallmarkProtection || peer.getWeight() >= Peers.pullThreshold)) {
                selectedPeers.add(peer);
            }
        }

        if (selectedPeers.size() > 0) {
            long totalWeight = 0;
            for (Peer peer : selectedPeers) {
                long weight = peer.getWeight();
                if (weight == 0) {
                    weight = 1;
                }
                totalWeight += weight;
            }

            long hit = ThreadLocalRandom.current().nextLong(totalWeight);
            for (Peer peer : selectedPeers) {
                long weight = peer.getWeight();
                if (weight == 0) {
                    weight = 1;
                }
                if ((hit -= weight) < 0) {
                    return peer;
                }
            }
        }
        return null;
    }

    static String normalizeHostAndPort(String address) {
        try {
            if (address == null) {
                return null;
            }
            URI uri = new URI("http://" + address.trim());
            String host = uri.getHost();
            if (host == null || host.equals("") || host.equals("localhost") || host.equals("127.0.0.1") || host.equals("0:0:0:0:0:0:0:1")) {
                return null;
            }
            InetAddress inetAddress = InetAddress.getByName(host);
            if (inetAddress.isAnyLocalAddress() || inetAddress.isLoopbackAddress() || inetAddress.isLinkLocalAddress()) {
                return null;
            }
            int port = uri.getPort();
            return port == -1 ? host : host + ':' + port;
        } catch (URISyntaxException |UnknownHostException e) {
            return null;
        }
    }

    private static int getNumberOfConnectedPublicPeers() {
        int numberOfConnectedPeers = 0;
        for (Peer peer : peers.values()) {
            if (peer.getState() == Peer.State.CONNECTED && peer.getAnnouncedAddress() != null) {
                numberOfConnectedPeers++;
            }
        }
        return numberOfConnectedPeers;
    }

    private Peers() {} // never

}
