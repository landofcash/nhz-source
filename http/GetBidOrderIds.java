package nhz.http;

import nhz.Asset;
import nhz.Order;
import nhz.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Iterator;

import static nhz.http.JSONResponses.INCORRECT_ASSET;
import static nhz.http.JSONResponses.MISSING_ASSET;
import static nhz.http.JSONResponses.UNKNOWN_ASSET;

public final class GetBidOrderIds extends APIServlet.APIRequestHandler {

    static final GetBidOrderIds instance = new GetBidOrderIds();

    private GetBidOrderIds() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String asset = req.getParameter("asset");
        if (asset == null) {
            return MISSING_ASSET;
        }

        long assetId;
        try {
            assetId = Convert.parseUnsignedLong(asset);
        } catch (RuntimeException e) {
            return INCORRECT_ASSET;
        }

        if (Asset.getAsset(assetId) == null) {
            return UNKNOWN_ASSET;
        }

        int limit;
        try {
            limit = Integer.parseInt(req.getParameter("limit"));
        } catch (NumberFormatException e) {
            limit = Integer.MAX_VALUE;
        }

        JSONArray orderIds = new JSONArray();
        Iterator<Order.Bid> bidOrders = Order.Bid.getSortedOrders(assetId).iterator();
        while (bidOrders.hasNext() && limit-- > 0) {
            orderIds.add(Convert.toUnsignedLong(bidOrders.next().getId()));
        }

        JSONObject response = new JSONObject();
        response.put("bidOrderIds", orderIds);
        return response;
    }

}
