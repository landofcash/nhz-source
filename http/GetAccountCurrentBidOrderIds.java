package nhz.http;

import nhz.Account;
import nhz.Order;
import nhz.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nhz.http.JSONResponses.INCORRECT_ACCOUNT;
import static nhz.http.JSONResponses.MISSING_ACCOUNT;
import static nhz.http.JSONResponses.UNKNOWN_ACCOUNT;

public final class GetAccountCurrentBidOrderIds extends APIServlet.APIRequestHandler {

    static final GetAccountCurrentBidOrderIds instance = new GetAccountCurrentBidOrderIds();

    private GetAccountCurrentBidOrderIds() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String accountId = req.getParameter("account");
        if (accountId == null) {
            return MISSING_ACCOUNT;
        }

        Account account;
        try {
            account = Account.getAccount(Convert.parseUnsignedLong(accountId));
            if (account == null) {
                return UNKNOWN_ACCOUNT;
            }
        } catch (RuntimeException e) {
            return INCORRECT_ACCOUNT;
        }

        Long assetId = null;
        try {
            assetId = Convert.parseUnsignedLong(req.getParameter("asset"));
        } catch (RuntimeException e) {
            // ignored
        }

        JSONArray orderIds = new JSONArray();
        for (Order.Bid bidOrder : Order.Bid.getAllBidOrders()) {
            if ((assetId == null || bidOrder.getAssetId().equals(assetId)) && bidOrder.getAccount().equals(account)) {
                orderIds.add(Convert.toUnsignedLong(bidOrder.getId()));
            }
        }

        JSONObject response = new JSONObject();
        response.put("bidOrderIds", orderIds);
        return response;
    }

}