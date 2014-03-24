package nhz.http;

import nhz.Account;
import nhz.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import static nhz.http.JSONResponses.INCORRECT_ACCOUNT;
import static nhz.http.JSONResponses.MISSING_ACCOUNT;
import static nhz.http.JSONResponses.UNKNOWN_ACCOUNT;

public final class GetAccount extends APIServlet.APIRequestHandler {

    static final GetAccount instance = new GetAccount();

    private GetAccount() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String account = req.getParameter("account");
        if (account == null) {
            return MISSING_ACCOUNT;
        }

        Account accountData;
        try {
            accountData = Account.getAccount(Convert.parseUnsignedLong(account));
            if (accountData == null) {
                return UNKNOWN_ACCOUNT;
            }
        } catch (RuntimeException e) {
            return INCORRECT_ACCOUNT;
        }

        JSONObject response = new JSONObject();
        synchronized (accountData) {
            if (accountData.getPublicKey() != null) {
                response.put("publicKey", Convert.toHexString(accountData.getPublicKey()));
            }

            response.put("balance", accountData.getBalance());
            response.put("effectiveBalance", accountData.getEffectiveBalance() * 100L);

            JSONArray assetBalances = new JSONArray();
            for (Map.Entry<Long, Integer> assetBalanceEntry : accountData.getAssetBalances().entrySet()) {

                JSONObject assetBalance = new JSONObject();
                assetBalance.put("asset", Convert.toUnsignedLong(assetBalanceEntry.getKey()));
                assetBalance.put("balance", assetBalanceEntry.getValue());
                assetBalances.add(assetBalance);

            }
            if (assetBalances.size() > 0) {

                response.put("assetBalances", assetBalances);

            }
        }
        return response;
    }

}
