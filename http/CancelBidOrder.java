package nhz.http;

import nhz.Account;
import nhz.Attachment;
import nhz.NhzException;
import nhz.Order;
import nhz.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nhz.http.JSONResponses.INCORRECT_ORDER;
import static nhz.http.JSONResponses.MISSING_ORDER;
import static nhz.http.JSONResponses.UNKNOWN_ACCOUNT;
import static nhz.http.JSONResponses.UNKNOWN_ORDER;

public final class CancelBidOrder extends CreateTransaction {

    static final CancelBidOrder instance = new CancelBidOrder();

    private CancelBidOrder() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NhzException.ValidationException {

        String orderValue = req.getParameter("order");
        if (orderValue == null) {
            return MISSING_ORDER;
        }

        Long order;
        try {
            order = Convert.parseUnsignedLong(orderValue);
        } catch (RuntimeException e) {
            return INCORRECT_ORDER;
        }

        Account account = getAccount(req);
        if (account == null) {
            return UNKNOWN_ACCOUNT;
        }

        Order.Bid orderData = Order.Bid.getBidOrder(order);
        if (orderData == null || !orderData.getAccount().getId().equals(account.getId())) {
            return UNKNOWN_ORDER;
        }

        Attachment attachment = new Attachment.ColoredCoinsBidOrderCancellation(order);
        return createTransaction(req, account, attachment);

    }

}
