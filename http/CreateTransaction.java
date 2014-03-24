package nhz.http;

import nhz.Account;
import nhz.Attachment;
import nhz.Constants;
import nhz.Genesis;
import nhz.Nhz;
import nhz.NhzException;
import nhz.Transaction;
import nhz.crypto.Crypto;
import nhz.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nhz.http.JSONResponses.INCORRECT_DEADLINE;
import static nhz.http.JSONResponses.INCORRECT_FEE;
import static nhz.http.JSONResponses.INCORRECT_REFERENCED_TRANSACTION;
import static nhz.http.JSONResponses.MISSING_DEADLINE;
import static nhz.http.JSONResponses.MISSING_FEE;
import static nhz.http.JSONResponses.MISSING_SECRET_PHRASE;
import static nhz.http.JSONResponses.NOT_ENOUGH_FUNDS;

abstract class CreateTransaction extends APIServlet.APIRequestHandler {

    final Account getAccount(HttpServletRequest req) {
        String secretPhrase = Convert.emptyToNull(req.getParameter("secretPhrase"));
        if (secretPhrase != null) {
            return Account.getAccount(Crypto.getPublicKey(secretPhrase));
        }
        String publicKeyString = Convert.emptyToNull(req.getParameter("publicKey"));
        if (publicKeyString == null) {
            return null;
        }
        return Account.getAccount(Convert.parseHexString(publicKeyString));
    }

    final JSONStreamAware createTransaction(HttpServletRequest req, Account senderAccount, Attachment attachment)
        throws NhzException.ValidationException {
        return createTransaction(req, senderAccount, Genesis.CREATOR_ID, 0, attachment);
    }

    final JSONStreamAware createTransaction(HttpServletRequest req, Account senderAccount, Long recipientId,
                                            int amount, Attachment attachment) throws NhzException.ValidationException {
        String deadlineValue = req.getParameter("deadline");
        String feeValue = req.getParameter("fee");
        String referencedTransactionValue = req.getParameter("referencedTransaction");
        String secretPhrase = Convert.emptyToNull(req.getParameter("secretPhrase"));
        String publicKeyValue = Convert.emptyToNull(req.getParameter("publicKey"));

        if (secretPhrase == null && publicKeyValue == null) {
            return MISSING_SECRET_PHRASE;
        } else if (feeValue == null) {
            return MISSING_FEE;
        } else if (deadlineValue == null) {
            return MISSING_DEADLINE;
        }

        short deadline;
        try {
            deadline = Short.parseShort(deadlineValue);
            if (deadline < 1 || deadline > 1440) {
                return INCORRECT_DEADLINE;
            }
        } catch (NumberFormatException e) {
            return INCORRECT_DEADLINE;
        }

        int fee;
        try {
            fee = Integer.parseInt(feeValue);
            if (fee <= 0 || fee >= Constants.MAX_BALANCE) {
                return INCORRECT_FEE;
            }
        } catch (NumberFormatException e) {
            return INCORRECT_FEE;
        }

        if ((amount + fee) * 100L > senderAccount.getUnconfirmedBalance()) {
            return NOT_ENOUGH_FUNDS;
        }

        Long referencedTransaction;
        try {
            referencedTransaction = referencedTransactionValue == null ? null : Convert.parseUnsignedLong(referencedTransactionValue);
        } catch (RuntimeException e) {
            return INCORRECT_REFERENCED_TRANSACTION;
        }

        // shouldn't try to get publicKey from senderAccount as it may have not been set yet
        byte[] publicKey = secretPhrase != null ? Crypto.getPublicKey(secretPhrase) : Convert.parseHexString(publicKeyValue);

        Transaction transaction = attachment == null ?
                Nhz.getTransactionProcessor().newTransaction(deadline, publicKey, recipientId,
                        amount, fee, referencedTransaction)
                :
                Nhz.getTransactionProcessor().newTransaction(deadline, publicKey, recipientId,
                        amount, fee, referencedTransaction, attachment);

        JSONObject response = new JSONObject();
        if (secretPhrase != null) {
            transaction.sign(secretPhrase);
            Nhz.getTransactionProcessor().broadcast(transaction);
            response.put("transaction", transaction.getStringId());
        }
        response.put("transactionBytes", Convert.toHexString(transaction.getBytes()));
        response.put("hash", transaction.getHash());
        return response;

    }

    @Override
    final boolean requirePost() {
        return true;
    }

}
