package nhz.http;

import nhz.Nhz;
import nhz.Transaction;
import nhz.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nhz.http.JSONResponses.INCORRECT_TRANSACTION;
import static nhz.http.JSONResponses.MISSING_TRANSACTION;
import static nhz.http.JSONResponses.UNKNOWN_TRANSACTION;

public final class GetTransaction extends APIServlet.APIRequestHandler {

    static final GetTransaction instance = new GetTransaction();

    private GetTransaction() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String transactionIdString = Convert.emptyToNull(req.getParameter("transaction"));
        String transactionHash = Convert.emptyToNull(req.getParameter("hash"));
        if (transactionIdString == null && transactionHash == null) {
            return MISSING_TRANSACTION;
        }

        Long transactionId = null;
        Transaction transaction;
        try {
            if (transactionIdString != null) {
                transactionId = Convert.parseUnsignedLong(transactionIdString);
                transaction = Nhz.getBlockchain().getTransaction(transactionId);
            } else {
                transaction = Nhz.getBlockchain().getTransaction(transactionHash);
                if (transaction == null) {
                    return UNKNOWN_TRANSACTION;
                }
            }
        } catch (RuntimeException e) {
            return INCORRECT_TRANSACTION;
        }

        JSONObject response;
        if (transaction == null) {
            transaction = Nhz.getTransactionProcessor().getUnconfirmedTransaction(transactionId);
            if (transaction == null) {
                return UNKNOWN_TRANSACTION;
            }
            response = transaction.getJSONObject();
        } else {
            response = transaction.getJSONObject();
            response.put("block", Convert.toUnsignedLong(transaction.getBlockId()));
            response.put("confirmations", Nhz.getBlockchain().getLastBlock().getHeight() - transaction.getHeight());
            response.put("blockTimestamp", transaction.getBlockTimestamp());
        }
        response.put("sender", Convert.toUnsignedLong(transaction.getSenderId()));
        response.put("hash", transaction.getHash());


        return response;
    }

}
