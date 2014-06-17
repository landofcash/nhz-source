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

public final class BroadcastTransaction extends APIServlet.APIRequestHandler {

    static final BroadcastTransaction instance = new BroadcastTransaction();

    private BroadcastTransaction() {
        super("transactionBytes");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NhzException.ValidationException {

        String transactionBytes = req.getParameter("transactionBytes");
        if (transactionBytes == null) {
            return MISSING_TRANSACTION_BYTES;
        }

        try {

            byte[] bytes = Convert.parseHexString(transactionBytes);
            Transaction transaction = Nhz.getTransactionProcessor().parseTransaction(bytes);

            JSONObject response = new JSONObject();

            try {
                Nhz.getTransactionProcessor().broadcast(transaction);
                response.put("transaction", transaction.getStringId());
                response.put("hash", transaction.getHash());
                response.put("fullHash", transaction.getFullHash());
            } catch (NhzException.ValidationException e) {
                response.put("error", e.getMessage());
            }

            return response;

        } catch (RuntimeException e) {
            return INCORRECT_TRANSACTION_BYTES;
        }
    }

    @Override
    boolean requirePost() {
        return true;
    }

}
