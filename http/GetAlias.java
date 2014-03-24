package nhz.http;

import nhz.Alias;
import nhz.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nhz.http.JSONResponses.INCORRECT_ALIAS;
import static nhz.http.JSONResponses.MISSING_ALIAS;
import static nhz.http.JSONResponses.UNKNOWN_ALIAS;

public final class GetAlias extends APIServlet.APIRequestHandler {

    static final GetAlias instance = new GetAlias();

    private GetAlias() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String alias = req.getParameter("alias");
        if (alias == null) {
            return MISSING_ALIAS;
        }

        Alias aliasData;
        try {
            aliasData = Alias.getAlias(Convert.parseUnsignedLong(alias));
            if (aliasData == null) {
                return UNKNOWN_ALIAS;
            }
        } catch (RuntimeException e) {
            return INCORRECT_ALIAS;
        }

        JSONObject response = new JSONObject();
        response.put("account", Convert.toUnsignedLong(aliasData.getAccount().getId()));
        response.put("alias", aliasData.getAliasName());
        if (aliasData.getURI().length() > 0) {
            response.put("uri", aliasData.getURI());
        }
        response.put("timestamp", aliasData.getTimestamp());
        return response;
    }

}
