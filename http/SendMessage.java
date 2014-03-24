package nhz.http;

import nhz.Account;
import nhz.Attachment;
import nhz.Constants;
import nhz.NhzException;
import nhz.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nhz.http.JSONResponses.INCORRECT_ARBITRARY_MESSAGE;
import static nhz.http.JSONResponses.INCORRECT_RECIPIENT;
import static nhz.http.JSONResponses.MISSING_MESSAGE;
import static nhz.http.JSONResponses.MISSING_RECIPIENT;
import static nhz.http.JSONResponses.UNKNOWN_ACCOUNT;

public final class SendMessage extends CreateTransaction {

    static final SendMessage instance = new SendMessage();

    private SendMessage() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NhzException.ValidationException {

        String recipientValue = req.getParameter("recipient");
        String messageValue = req.getParameter("message");

        if (recipientValue == null || "0".equals(recipientValue)) {
            return MISSING_RECIPIENT;
        } else if (messageValue == null) {
            return MISSING_MESSAGE;
        }

        Long recipient;
        try {
            recipient = Convert.parseUnsignedLong(recipientValue);
        } catch (RuntimeException e) {
            return INCORRECT_RECIPIENT;
        }

        byte[] message;
        try {
            message = Convert.parseHexString(messageValue);
        } catch (RuntimeException e) {
            return INCORRECT_ARBITRARY_MESSAGE;
        }
        if (message.length > Constants.MAX_ARBITRARY_MESSAGE_LENGTH) {
            return INCORRECT_ARBITRARY_MESSAGE;
        }

        Account account = getAccount(req);
        if (account == null) {
            return UNKNOWN_ACCOUNT;
        }

        Attachment attachment = new Attachment.MessagingArbitraryMessage(message);
        return createTransaction(req, account, recipient, 0, attachment);

    }

}