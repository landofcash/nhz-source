package nhz.http;

import nhz.Alias;
import nhz.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nhz.http.JSONResponses.MISSING_ALIAS;
import static nhz.http.JSONResponses.UNKNOWN_ALIAS;

public final class GetAliasURI extends APIServlet.APIRequestHandler {

    static final GetAliasURI instance = new GetAliasURI();

    private GetAliasURI() {
        super("alias");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String alias = req.getParameter("alias");
        if (alias == null) {
            return MISSING_ALIAS;
        }

        Alias aliasData = Alias.getAlias(alias.toLowerCase());
        if (aliasData == null) {
            return UNKNOWN_ALIAS;
        }

        if (aliasData.getURI().length() > 0) {

            JSONObject response = new JSONObject();
            response.put("uri", aliasData.getURI());
            return response;

        } else {
            return JSON.emptyJSON;
        }
    }

}
