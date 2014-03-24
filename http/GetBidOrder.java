package nhz.http;

import nhz.Order;
import nhz.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nhz.http.JSONResponses.INCORRECT_ORDER;
import static nhz.http.JSONResponses.MISSING_ORDER;
import static nhz.http.JSONResponses.UNKNOWN_ORDER;

public final class GetBidOrder extends APIServlet.APIRequestHandler {

    static final GetBidOrder instance = new GetBidOrder();

    private GetBidOrder() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String order = req.getParameter("order");
        if (order == null) {
            return MISSING_ORDER;
        }

        Order.Bid orderData;
        try {
            orderData = Order.Bid.getBidOrder(Convert.parseUnsignedLong(order));
            if (orderData == null) {
                return UNKNOWN_ORDER;
            }
        } catch (RuntimeException e) {
            return INCORRECT_ORDER;
        }

        JSONObject response = new JSONObject();
        response.put("account", Convert.toUnsignedLong(orderData.getAccount().getId()));
        response.put("asset", Convert.toUnsignedLong(orderData.getAssetId()));
        response.put("quantity", orderData.getQuantity());
        response.put("price", orderData.getPrice());

        return response;

    }

}
