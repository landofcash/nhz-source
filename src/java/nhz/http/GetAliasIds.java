package nhz.http;

import nhz.Alias;
import nhz.NhzException;
import nhz.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAliasIds extends APIServlet.APIRequestHandler {

    static final GetAliasIds instance = new GetAliasIds();

    private GetAliasIds() {
        super("timestamp");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NhzException {

        int timestamp = ParameterParser.getTimestamp(req);

        JSONArray aliasIds = new JSONArray();
        for (Alias alias : Alias.getAllAliases()) {
            if (alias.getTimestamp() >= timestamp) {
                aliasIds.add(Convert.toUnsignedLong(alias.getId()));
            }
        }

        JSONObject response = new JSONObject();

        response.put("aliasIds", aliasIds);
        return response;
    }

}
