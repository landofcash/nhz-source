package nhz.http;

import nhz.peer.Peer;
import nhz.peer.Peers;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nhz.http.JSONResponses.MISSING_PEER;
import static nhz.http.JSONResponses.UNKNOWN_PEER;

public final class GetPeer extends APIServlet.APIRequestHandler {

    static final GetPeer instance = new GetPeer();

    private GetPeer() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String peerAddress = req.getParameter("peer");
        if (peerAddress == null) {
            return MISSING_PEER;
        }

        Peer peer = Peers.getPeer(peerAddress);
        if (peer == null) {
            return UNKNOWN_PEER;
        }

        JSONObject response = new JSONObject();
        response.put("state", peer.getState().ordinal());
        response.put("announcedAddress", peer.getAnnouncedAddress());
        response.put("shareAddress", peer.shareAddress());
        if (peer.getHallmark() != null) {
            response.put("hallmark", peer.getHallmark().getHallmarkString());
        }
        response.put("weight", peer.getWeight());
        response.put("downloadedVolume", peer.getDownloadedVolume());
        response.put("uploadedVolume", peer.getUploadedVolume());
        response.put("application", peer.getApplication());
        response.put("version", peer.getVersion());
        response.put("platform", peer.getPlatform());
        response.put("blacklisted", peer.isBlacklisted());

        return response;
    }

}
