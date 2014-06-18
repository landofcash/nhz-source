package nhz.peer;

import nhz.Nhz;
import nhz.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class ProcessTransactions extends PeerServlet.PeerRequestHandler {

    static final ProcessTransactions instance = new ProcessTransactions();

    private ProcessTransactions() {}


    @Override
    JSONStreamAware processRequest(JSONObject request, Peer peer) {

        try {
            Nhz.getTransactionProcessor().processPeerTransactions(request);
        } catch (RuntimeException e) {
            peer.blacklist(e);
        }

        return JSON.emptyJSON;
    }

}
