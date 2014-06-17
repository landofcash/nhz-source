package nhz.http;


import nhz.Account;
import nhz.Alias;
import nhz.Attachment;
import nhz.Constants;
import nhz.NhzException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nhz.http.JSONResponses.INCORRECT_ALIAS_LENGTH;
import static nhz.http.JSONResponses.INCORRECT_ALIAS_NAME;
import static nhz.http.JSONResponses.INCORRECT_URI_LENGTH;
import static nhz.http.JSONResponses.MISSING_ALIAS;
import static nhz.http.JSONResponses.MISSING_URI;

public final class AssignAlias extends CreateTransaction {

    static final AssignAlias instance = new AssignAlias();

    private AssignAlias() {
        super("alias", "uri");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NhzException {
        String alias = req.getParameter("alias");
        String uri = req.getParameter("uri");

        if (alias == null) {
            return MISSING_ALIAS;
        } else if (uri == null) {
            return MISSING_URI;
        }

        alias = alias.trim();
        if (alias.length() == 0 || alias.length() > Constants.MAX_ALIAS_LENGTH) {
            return INCORRECT_ALIAS_LENGTH;
        }

        String normalizedAlias = alias.toLowerCase();
        for (int i = 0; i < normalizedAlias.length(); i++) {
            if (Constants.ALPHABET.indexOf(normalizedAlias.charAt(i)) < 0) {
                return INCORRECT_ALIAS_NAME;
            }
        }

        uri = uri.trim();
        if (uri.length() > Constants.MAX_ALIAS_URI_LENGTH) {
            return INCORRECT_URI_LENGTH;
        }

        Account account = ParameterParser.getSenderAccount(req);

        Alias aliasData = Alias.getAlias(normalizedAlias);
        if (aliasData != null && !aliasData.getAccount().getId().equals(account.getId())) {
            JSONObject response = new JSONObject();
            response.put("errorCode", 8);
            response.put("errorDescription", "\"" + alias + "\" is already used");
            return response;
        }

        Attachment attachment = new Attachment.MessagingAliasAssignment(alias, uri);
        return createTransaction(req, account, attachment);

    }

}
