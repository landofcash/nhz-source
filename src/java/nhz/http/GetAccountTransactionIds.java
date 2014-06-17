package nhz.http;

import nhz.Account;
import nhz.Nhz;
import nhz.NhzException;
import nhz.Transaction;
import nhz.util.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAccountTransactionIds extends APIServlet.APIRequestHandler {

    static final GetAccountTransactionIds instance = new GetAccountTransactionIds();

    private GetAccountTransactionIds() {
        super("account", "timestamp", "type", "subtype");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NhzException {

        Account account = ParameterParser.getAccount(req);
        int timestamp = ParameterParser.getTimestamp(req);

        byte type;
        byte subtype;
        try {
            type = Byte.parseByte(req.getParameter("type"));
        } catch (NumberFormatException e) {
            type = -1;
        }
        try {
            subtype = Byte.parseByte(req.getParameter("subtype"));
        } catch (NumberFormatException e) {
            subtype = -1;
        }

        JSONArray transactionIds = new JSONArray();
        try (DbIterator<? extends Transaction> iterator = Nhz.getBlockchain().getTransactions(account, type, subtype, timestamp)) {
            while (iterator.hasNext()) {
                Transaction transaction = iterator.next();
                transactionIds.add(transaction.getStringId());
            }
        }

        JSONObject response = new JSONObject();
        response.put("transactionIds", transactionIds);
        return response;

    }

}
