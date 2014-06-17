package nhz.http;

import nhz.Nhz;
import nhz.NhzException;
import nhz.Transaction;
import nhz.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nhz.http.JSONResponses.INCORRECT_TRANSACTION_BYTES;
import static nhz.http.JSONResponses.MISSING_TRANSACTION_BYTES;

public final class ParseTransaction extends APIServlet.APIRequestHandler {

    static final ParseTransaction instance = new ParseTransaction();

    private ParseTransaction() {
        super("transactionBytes");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NhzException.ValidationException {

        String transactionBytes = req.getParameter("transactionBytes");
        if (transactionBytes == null) {
            return MISSING_TRANSACTION_BYTES;
        }
        JSONObject response;
        try {
            byte[] bytes = Convert.parseHexString(transactionBytes);
            Transaction transaction = Nhz.getTransactionProcessor().parseTransaction(bytes);
            response = JSONData.unconfirmedTransaction(transaction);
            response.put("verify", transaction.verify());
        } catch (NhzException.ValidationException|RuntimeException e) {
            //Logger.logDebugMessage(e.getMessage(), e);
            return INCORRECT_TRANSACTION_BYTES;
        }
        return response;
    }

}
