package nhz.http;

import nhz.Asset;
import nhz.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nhz.http.JSONResponses.INCORRECT_ASSET;
import static nhz.http.JSONResponses.MISSING_ASSET;
import static nhz.http.JSONResponses.UNKNOWN_ASSET;

public final class GetAsset extends APIServlet.APIRequestHandler {

    static final GetAsset instance = new GetAsset();

    private GetAsset() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String asset = req.getParameter("asset");
        if (asset == null) {
            return MISSING_ASSET;
        }

        Asset assetData;
        try {
            assetData = Asset.getAsset(Convert.parseUnsignedLong(asset));
            if (assetData == null) {
                return UNKNOWN_ASSET;
            }
        } catch (RuntimeException e) {
            return INCORRECT_ASSET;
        }

        JSONObject response = new JSONObject();
        response.put("account", Convert.toUnsignedLong(assetData.getAccountId()));
        response.put("name", assetData.getName());
        if (assetData.getDescription().length() > 0) {
            response.put("description", assetData.getDescription());
        }
        response.put("quantity", assetData.getQuantity());

        return response;
    }

}
