package nhz.http;

import nhz.Account;
import nhz.Nhz;
import nhz.Transaction;
import nhz.util.Convert;
import nhz.util.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nhz.http.JSONResponses.INCORRECT_ACCOUNT;
import static nhz.http.JSONResponses.INCORRECT_TIMESTAMP;
import static nhz.http.JSONResponses.MISSING_ACCOUNT;
import static nhz.http.JSONResponses.MISSING_TIMESTAMP;
import static nhz.http.JSONResponses.UNKNOWN_ACCOUNT;

public final class GetAccountTransactionIds extends APIServlet.APIRequestHandler {

    static final GetAccountTransactionIds instance = new GetAccountTransactionIds();

    private GetAccountTransactionIds() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String accountId = req.getParameter("account");
        String timestampValue = req.getParameter("timestamp");
        if (accountId == null) {
            return MISSING_ACCOUNT;
        } else if (timestampValue == null) {
            return MISSING_TIMESTAMP;
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

        int timestamp;
        try {
            timestamp = Integer.parseInt(timestampValue);
            if (timestamp < 0) {
                return INCORRECT_TIMESTAMP;
            }
        } catch (NumberFormatException e) {
            return INCORRECT_TIMESTAMP;
        }

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
        try (DbIterator<? extends Transaction> iterator = Nhz.getBlockchain().getAllTransactions(account, type, subtype, timestamp)) {
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
