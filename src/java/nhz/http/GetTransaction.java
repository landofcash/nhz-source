package nhz.http;

import nhz.Nhz;
import nhz.Transaction;
import nhz.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nhz.http.JSONResponses.INCORRECT_TRANSACTION;
import static nhz.http.JSONResponses.MISSING_TRANSACTION;
import static nhz.http.JSONResponses.UNKNOWN_TRANSACTION;

public final class GetTransaction extends APIServlet.APIRequestHandler {

    static final GetTransaction instance = new GetTransaction();

    private GetTransaction() {
        super("transaction", "hash", "fullHash");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String transactionIdString = Convert.emptyToNull(req.getParameter("transaction"));
        String transactionHash = Convert.emptyToNull(req.getParameter("hash"));
        String transactionFullHash = Convert.emptyToNull(req.getParameter("fullHash"));
        if (transactionIdString == null && transactionHash == null && transactionFullHash == null) {
            return MISSING_TRANSACTION;
        }

        Long transactionId = null;
        Transaction transaction;
        try {
            if (transactionIdString != null) {
                transactionId = Convert.parseUnsignedLong(transactionIdString);
                transaction = Nhz.getBlockchain().getTransaction(transactionId);
            } else if (transactionHash != null) {
                transaction = Nhz.getBlockchain().getTransaction(transactionHash);
                if (transaction == null) {
                    return UNKNOWN_TRANSACTION;
                }
            } else {
                transaction = Nhz.getBlockchain().getTransactionByFullHash(transactionFullHash);
                if (transaction == null) {
                    return UNKNOWN_TRANSACTION;
                }
            }
        } catch (RuntimeException e) {
            return INCORRECT_TRANSACTION;
        }

        if (transaction == null) {
            transaction = Nhz.getTransactionProcessor().getUnconfirmedTransaction(transactionId);
            if (transaction == null) {
                return UNKNOWN_TRANSACTION;
            }
            return JSONData.unconfirmedTransaction(transaction);
        } else {
            return JSONData.transaction(transaction);
        }

    }

}
