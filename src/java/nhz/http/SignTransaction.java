package nhz.http;

import nhz.Nhz;
import nhz.NhzException;
import nhz.Transaction;
import nhz.crypto.Crypto;
import nhz.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nhz.http.JSONResponses.INCORRECT_UNSIGNED_BYTES;
import static nhz.http.JSONResponses.MISSING_SECRET_PHRASE;
import static nhz.http.JSONResponses.MISSING_UNSIGNED_BYTES;

public final class SignTransaction extends APIServlet.APIRequestHandler {

    static final SignTransaction instance = new SignTransaction();

    private SignTransaction() {
        super("unsignedTransactionBytes", "secretPhrase");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NhzException.ValidationException {

        String transactionBytes = Convert.emptyToNull(req.getParameter("unsignedTransactionBytes"));
        if (transactionBytes == null) {
            return MISSING_UNSIGNED_BYTES;
        }
        String secretPhrase = Convert.emptyToNull(req.getParameter("secretPhrase"));
        if (secretPhrase == null) {
            return MISSING_SECRET_PHRASE;
        }

        try {
            byte[] bytes = Convert.parseHexString(transactionBytes);
            Transaction transaction = Nhz.getTransactionProcessor().parseTransaction(bytes);
            transaction.validateAttachment();
            if (transaction.getSignature() != null) {
                JSONObject response = new JSONObject();
                response.put("errorCode", 4);
                response.put("errorDescription", "Incorrect \"unsignedTransactionBytes\" - transaction is already signed");
                return response;
            }
            transaction.sign(secretPhrase);
            JSONObject response = new JSONObject();
            response.put("transaction", transaction.getStringId());
            response.put("fullHash", transaction.getFullHash());
            response.put("transactionBytes", Convert.toHexString(transaction.getBytes()));
            response.put("signatureHash", Convert.toHexString(Crypto.sha256().digest(transaction.getSignature())));
            response.put("verify", transaction.verify());
            return response;
        } catch (NhzException.ValidationException|RuntimeException e) {
            //Logger.logDebugMessage(e.getMessage(), e);
            return INCORRECT_UNSIGNED_BYTES;
        }
    }

}
