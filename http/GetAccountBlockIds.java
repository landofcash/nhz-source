package nhz.http;

import nhz.Account;
import nhz.Block;
import nhz.Nhz;
import nhz.util.Convert;
import nhz.util.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nhz.http.JSONResponses.INCORRECT_ACCOUNT;
import static nhz.http.JSONResponses.INCORRECT_TIMESTAMP;
import static nhz.http.JSONResponses.MISSING_ACCOUNT;
import static nhz.http.JSONResponses.MISSING_TIMESTAMP;
import static nhz.http.JSONResponses.UNKNOWN_ACCOUNT;

public final class GetAccountBlockIds extends APIServlet.APIRequestHandler {

    static final GetAccountBlockIds instance = new GetAccountBlockIds();

    private GetAccountBlockIds() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String account = req.getParameter("account");
        String timestampValue = req.getParameter("timestamp");
        if (account == null) {
            return MISSING_ACCOUNT;
        } else if (timestampValue == null) {
            return MISSING_TIMESTAMP;
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

        int timestamp;
        try {
            timestamp = Integer.parseInt(timestampValue);
            if (timestamp < 0) {
                return INCORRECT_TIMESTAMP;
            }
        } catch (NumberFormatException e) {
            return INCORRECT_TIMESTAMP;
        }

        JSONArray blockIds = new JSONArray();
        try (DbIterator<? extends Block> iterator = Nhz.getBlockchain().getAllBlocks(accountData, timestamp)) {
            while (iterator.hasNext()) {
                Block block = iterator.next();
                blockIds.add(block.getStringId());
            }
        }

        JSONObject response = new JSONObject();
        response.put("blockIds", blockIds);

        return response;
    }

}
