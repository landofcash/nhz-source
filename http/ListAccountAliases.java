package nhz.http;

import nhz.Account;
import nhz.Alias;
import nhz.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nhz.http.JSONResponses.INCORRECT_ACCOUNT;
import static nhz.http.JSONResponses.MISSING_ACCOUNT;
import static nhz.http.JSONResponses.UNKNOWN_ACCOUNT;

public final class ListAccountAliases extends APIServlet.APIRequestHandler {

    static final ListAccountAliases instance = new ListAccountAliases();

    private ListAccountAliases() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String account = req.getParameter("account");
        if (account == null) {
            return MISSING_ACCOUNT;
        }

        Long accountId;
        Account accountData;
        try {
            accountId = Convert.parseUnsignedLong(account);
            accountData = Account.getAccount(accountId);
            if (accountData == null) {
                return UNKNOWN_ACCOUNT;
            }
        } catch (RuntimeException e) {
            return INCORRECT_ACCOUNT;
        }

        JSONArray aliases = new JSONArray();
        for (Alias alias : Alias.getAllAliases()) {
            if (alias.getAccount().equals(accountData)) {
                JSONObject aliasData = new JSONObject();
                aliasData.put("alias", alias.getAliasName());
                aliasData.put("uri", alias.getURI());
                aliases.add(aliasData);
            }
        }

        JSONObject response = new JSONObject();
        response.put("aliases", aliases);

        return response;
    }

}
