package nhz.http;

import nhz.Account;
import nhz.Attachment;
import nhz.Constants;
import nhz.NhzException;
import nhz.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nhz.http.JSONResponses.INCORRECT_ARBITRARY_MESSAGE;
import static nhz.http.JSONResponses.MISSING_MESSAGE;

public final class SendMessage extends CreateTransaction {

    static final SendMessage instance = new SendMessage();

    private SendMessage() {
        super("recipient", "message");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NhzException {

        Long recipient = ParameterParser.getRecipientId(req);

        String messageValue = req.getParameter("message");
        if (messageValue == null) {
            return MISSING_MESSAGE;
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

        Account account = ParameterParser.getSenderAccount(req);

        Attachment attachment = new Attachment.MessagingArbitraryMessage(message);
        return createTransaction(req, account, recipient, 0, attachment);

    }

}
