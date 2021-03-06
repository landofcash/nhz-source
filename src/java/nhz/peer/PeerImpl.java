package nhz.peer;

import nhz.Account;
import nhz.BlockchainProcessor;
import nhz.Constants;
import nhz.TransactionType;
import nhz.util.Convert;
import nhz.util.Logger;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.JSONValue;

import java.io.IOException;
import java.io.StringWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

final class PeerImpl implements Peer {

    private final String peerAddress;
    private volatile String announcedAddress;
    private volatile int port;
    private volatile boolean shareAddress;
    private volatile Hallmark hallmark;
    private volatile String platform;
    private volatile String application;
    private volatile String version;
    private volatile long adjustedWeight;
    private volatile long blacklistingTime;
    private volatile State state;
    private volatile long downloadedVolume;
    private volatile long uploadedVolume;

    PeerImpl(String peerAddress, String announcedAddress) {
        this.peerAddress = peerAddress;
        this.announcedAddress = announcedAddress;
        try {
            this.port = new URL("http://" + announcedAddress).getPort();
        } catch (MalformedURLException ignore) {}
        this.state = State.NON_CONNECTED;
        this.shareAddress = true;
    }

    @Override
    public String getPeerAddress() {
        return peerAddress;
    }

    @Override
    public State getState() {
        return state;
    }

    void setState(State state) {
        if (this.state == state) {
            return;
        }
        if (this.state == State.NON_CONNECTED) {
            this.state = state;
            Peers.notifyListeners(this, Peers.Event.ADDED_ACTIVE_PEER);
        } else if (state != State.NON_CONNECTED) {
            this.state = state;
            Peers.notifyListeners(this, Peers.Event.CHANGED_ACTIVE_PEER);
        }
    }

    @Override
    public long getDownloadedVolume() {
        return downloadedVolume;
    }

    void updateDownloadedVolume(long volume) {
        synchronized (this) {
            downloadedVolume += volume;
        }
        Peers.notifyListeners(this, Peers.Event.DOWNLOADED_VOLUME);
    }

    @Override
    public long getUploadedVolume() {
        return uploadedVolume;
    }

    void updateUploadedVolume(long volume) {
        synchronized (this) {
            uploadedVolume += volume;
        }
        Peers.notifyListeners(this, Peers.Event.UPLOADED_VOLUME);
    }

    @Override
    public String getVersion() {
        return version;
    }

    void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String getApplication() {
        return application;
    }

    void setApplication(String application) {
        this.application = application;
    }

    @Override
    public String getPlatform() {
        return platform;
    }

    void setPlatform(String platform) {
        this.platform = platform;
    }

    @Override
    public String getSoftware() {
        return Convert.truncate(application, "?", 10, false)
                + " (" + Convert.truncate(version, "?", 10, false) + ")"
                + " @ " + Convert.truncate(platform, "?", 10, false);
    }

    @Override
    public boolean shareAddress() {
        return shareAddress;
    }

    void setShareAddress(boolean shareAddress) {
        this.shareAddress = shareAddress;
    }

    @Override
    public String getAnnouncedAddress() {
        return announcedAddress;
    }

    void setAnnouncedAddress(String announcedAddress) {
        String announcedPeerAddress = Peers.normalizeHostAndPort(announcedAddress);
        if (announcedPeerAddress != null) {
            this.announcedAddress = announcedPeerAddress;
            try {
                this.port = new URL("http://" + announcedPeerAddress).getPort();
            } catch (MalformedURLException ignore) {}
        }
    }

    int getPort() {
        return port;
    }

    @Override
    public boolean isWellKnown() {
        return announcedAddress != null && Peers.wellKnownPeers.contains(announcedAddress);
    }

    @Override
    public Hallmark getHallmark() {
        return hallmark;
    }

    @Override
    public int getWeight() {
        if (hallmark == null) {
            return 0;
        }
        Account account = Account.getAccount(hallmark.getAccountId());
        if (account == null) {
            return 0;
        }
        return (int)(adjustedWeight * (account.getBalanceNQT() / Constants.ONE_NHZ) / Constants.MAX_BALANCE_NHZ);
    }

    @Override
    public boolean isBlacklisted() {
        return blacklistingTime > 0 || Peers.knownBlacklistedPeers.contains(peerAddress);
    }

    @Override
    public void blacklist(Exception cause) {
        if (cause instanceof TransactionType.NotYetEnabledException || cause instanceof BlockchainProcessor.BlockOutOfOrderException) {
            // don't blacklist peers just because a feature is not yet enabled
            // prevents erroneous blacklisting during loading of blockchain from scratch
            return;
        }
        if (! isBlacklisted()) {
            Logger.logDebugMessage("Blacklisting " + peerAddress + " because of: " + cause.toString());
        }
        blacklist();
    }

    @Override
    public void blacklist() {
        blacklistingTime = System.currentTimeMillis();
        setState(State.NON_CONNECTED);
        Peers.notifyListeners(this, Peers.Event.BLACKLIST);
    }

    @Override
    public void unBlacklist() {
        setState(State.NON_CONNECTED);
        blacklistingTime = 0;
        Peers.notifyListeners(this, Peers.Event.UNBLACKLIST);
    }

    void updateBlacklistedStatus(long curTime) {
        if (blacklistingTime > 0 && blacklistingTime + Peers.blacklistingPeriod <= curTime) {
            unBlacklist();
        }
    }

    @Override
    public void deactivate() {
        setState(State.NON_CONNECTED);
        Peers.notifyListeners(this, Peers.Event.DEACTIVATE);
    }

    @Override
    public void remove() {
        Peers.removePeer(this);
        Peers.notifyListeners(this, Peers.Event.REMOVE);
    }

    @Override
    public JSONObject send(final JSONStreamAware request) {
        JSONObject response;
        String log = null;
        boolean showLog = false;

        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse resp = null;
        try {

            String address = announcedAddress != null ? announcedAddress : peerAddress;

            if (Peers.communicationLoggingMask != 0) {
                StringWriter stringWriter = new StringWriter();
                request.writeJSONString(stringWriter);
                log = "\"" + address + "\": " + stringWriter.toString();
            }

            URL url = new URL("http://" + address + (port <= 0 ? ":" + (Constants.isTestnet ? Peers.TESTNET_PEER_PORT : Peers.DEFAULT_PEER_PORT) : "") + "/nhz");

            //request
            HttpPost httpPost = new HttpPost(url.toURI().toString());
            if(Peers.myAddress!=null){
                RequestConfig requestConfig = RequestConfig.custom().setLocalAddress(InetAddress.getByName(Peers.myAddress))
                        .setConnectTimeout(Peers.connectTimeout).setSocketTimeout(Peers.readTimeout).build();
                httpPost.setConfig(requestConfig);
            }else{
                RequestConfig requestConfig = RequestConfig.custom()
                        .setConnectTimeout(Peers.connectTimeout).setSocketTimeout(Peers.readTimeout).build();
                httpPost.setConfig(requestConfig);
            }

            StringWriter stringWriter = new StringWriter();
            request.writeJSONString(stringWriter);
            String rawJsonRequest=stringWriter.toString();
            httpPost.setEntity(new StringEntity(rawJsonRequest, "UTF8"));

            //call and response
            resp = httpclient.execute(httpPost);
            //Logger.logMessage("Sent http request."+url.toURI().toString()+" resp code:"+resp.getStatusLine().getStatusCode());
            HttpEntity respEntity = resp.getEntity();
            updateUploadedVolume(rawJsonRequest.length());
            if (resp.getStatusLine().getStatusCode()==200) {
                String rawJsonResponse=EntityUtils.toString(respEntity);
                //Logger.logMessage("Parsing response."+url.toURI().toString()+" " + rawJsonResponse);
                response = (JSONObject)JSONValue.parse(rawJsonResponse);
                updateDownloadedVolume(rawJsonResponse.length());

            } else {

                if ((Peers.communicationLoggingMask & Peers.LOGGING_MASK_NON200_RESPONSES) != 0) {
                    log += " >>> Peer responded with HTTP " + resp.getStatusLine().getStatusCode() + " code!";
                    showLog = true;
                }
                if (state == State.CONNECTED) {
                    setState(State.DISCONNECTED);
                } else {
                    setState(State.NON_CONNECTED);
                }
                response = null;
            }
            resp.close();
            EntityUtils.consume(respEntity);
        } catch (RuntimeException|IOException e) {
            if (!(e instanceof UnknownHostException || e instanceof SocketTimeoutException || e instanceof SocketException)) {
                Logger.logDebugMessage("Error sending JSON request", e);
            }
            if ((Peers.communicationLoggingMask & Peers.LOGGING_MASK_EXCEPTIONS) != 0) {
                log += " >>> " + e.toString();
                showLog = true;
            }
            if (state == State.CONNECTED) {
                setState(State.DISCONNECTED);
            }
            response = null;
        } catch (URISyntaxException e) {
            log += " >>> " + e.toString();
            showLog = true;
            response = null;
        }finally {
            try {
                if (httpclient != null) {
                    httpclient.close();
                }

            }catch(Exception ex){
                Logger.logMessage("FATAL ERROR. " +ex);
            }
        }
        if (showLog) {
            Logger.logMessage(log + "\n");
        }
        return response;
    }

    @Override
    public int compareTo(Peer o) {
        if (getWeight() > o.getWeight()) {
            return -1;
        } else if (getWeight() < o.getWeight()) {
            return 1;
        }
        return 0;
    }

    void connect() {
        JSONObject response = send(Peers.myPeerInfoRequest);
        if (response != null) {
            application = (String)response.get("application");
            version = (String)response.get("version");
            platform = (String)response.get("platform");
            shareAddress = Boolean.TRUE.equals(response.get("shareAddress"));
            if (announcedAddress == null) {
                setAnnouncedAddress(peerAddress);
                Logger.logDebugMessage("Connected to peer without announced address, setting to " + peerAddress);
            }
            if (analyzeHallmark(announcedAddress, (String)response.get("hallmark"))) {
                setState(State.CONNECTED);
            } else {
                blacklist();
            }
        } else {
            setState(State.NON_CONNECTED);
        }
    }

    boolean analyzeHallmark(String address, final String hallmarkString) {

        if (hallmarkString == null && this.hallmark == null) {
            return true;
        }

        if (this.hallmark != null && this.hallmark.getHallmarkString().equals(hallmarkString)) {
            return true;
        }

        if (hallmarkString == null) {
            this.hallmark = null;
            return true;
        }

        try {
            URI uri = new URI("http://" + address.trim());
            String host = uri.getHost();

            Hallmark hallmark = Hallmark.parseHallmark(hallmarkString);
            if (! hallmark.isValid()
                    || ! (hallmark.getHost().equals(host) || InetAddress.getByName(host).equals(InetAddress.getByName(hallmark.getHost())))) {
                Logger.logDebugMessage("Invalid hallmark for " + host + ", hallmark host is " + hallmark.getHost());
                return false;
            }
            this.hallmark = hallmark;
            Long accountId = Account.getId(hallmark.getPublicKey());
            List<PeerImpl> groupedPeers = new ArrayList<>();
            int mostRecentDate = 0;
            long totalWeight = 0;
            for (PeerImpl peer : Peers.allPeers) {
                if (peer.hallmark == null) {
                    continue;
                }
                if (accountId.equals(peer.hallmark.getAccountId())) {
                    groupedPeers.add(peer);
                    if (peer.hallmark.getDate() > mostRecentDate) {
                        mostRecentDate = peer.hallmark.getDate();
                        totalWeight = peer.getHallmarkWeight(mostRecentDate);
                    } else {
                        totalWeight += peer.getHallmarkWeight(mostRecentDate);
                    }
                }
            }

            for (PeerImpl peer : groupedPeers) {
                peer.adjustedWeight = Constants.MAX_BALANCE_NHZ * peer.getHallmarkWeight(mostRecentDate) / totalWeight;
                Peers.notifyListeners(peer, Peers.Event.WEIGHT);
            }

            return true;

        } catch (URISyntaxException | UnknownHostException | RuntimeException e) {
            Logger.logDebugMessage("Failed to analyze hallmark for peer " + address + ", " + e.toString());
        }
        return false;

    }

    private int getHallmarkWeight(int date) {
        if (hallmark == null || ! hallmark.isValid() || hallmark.getDate() != date) {
            return 0;
        }
        return hallmark.getWeight();
    }

}
