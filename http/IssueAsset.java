package nhz.http;

import nhz.Account;
import nhz.Asset;
import nhz.Attachment;
import nhz.Constants;
import nhz.NhzException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nhz.http.JSONResponses.ASSET_NAME_ALREADY_USED;
import static nhz.http.JSONResponses.INCORRECT_ASSET_DESCRIPTION;
import static nhz.http.JSONResponses.INCORRECT_ASSET_NAME;
import static nhz.http.JSONResponses.INCORRECT_ASSET_NAME_LENGTH;
import static nhz.http.JSONResponses.INCORRECT_ASSET_QUANTITY;
import static nhz.http.JSONResponses.INCORRECT_QUANTITY;
import static nhz.http.JSONResponses.MISSING_NAME;
import static nhz.http.JSONResponses.MISSING_QUANTITY;
import static nhz.http.JSONResponses.UNKNOWN_ACCOUNT;

public final class IssueAsset extends CreateTransaction {

    static final IssueAsset instance = new IssueAsset();

    private IssueAsset() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NhzException.ValidationException {

        String name = req.getParameter("name");
        String description = req.getParameter("description");
        String quantityValue = req.getParameter("quantity");

        if (name == null) {
            return MISSING_NAME;
        } else if (quantityValue == null) {
            return MISSING_QUANTITY;
        }

        name = name.trim();
        if (name.length() < 3 || name.length() > 10) {
            return INCORRECT_ASSET_NAME_LENGTH;
        }

        String normalizedName = name.toLowerCase();
        for (int i = 0; i < normalizedName.length(); i++) {
            if (Constants.ALPHABET.indexOf(normalizedName.charAt(i)) < 0) {
                return INCORRECT_ASSET_NAME;
            }
        }

        if (Asset.getAsset(normalizedName) != null) {
            return ASSET_NAME_ALREADY_USED;
        }
        if (description != null && description.length() > 1000) {
            return INCORRECT_ASSET_DESCRIPTION;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityValue);
            if (quantity <= 0 || quantity > Constants.MAX_ASSET_QUANTITY) {
                return INCORRECT_ASSET_QUANTITY;
            }
        } catch (NumberFormatException e) {
            return INCORRECT_QUANTITY;
        }

        Account account = getAccount(req);
        if (account == null) {
            return UNKNOWN_ACCOUNT;
        }

        Attachment attachment = new Attachment.ColoredCoinsAssetIssuance(name, description, quantity);
        return createTransaction(req, account, attachment);

    }

}
