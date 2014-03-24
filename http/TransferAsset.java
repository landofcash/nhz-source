package nhz.http;

import nhz.Account;
import nhz.Attachment;
import nhz.Constants;
import nhz.NhzException;
import nhz.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nhz.http.JSONResponses.INCORRECT_ASSET;
import static nhz.http.JSONResponses.INCORRECT_QUANTITY;
import static nhz.http.JSONResponses.INCORRECT_RECIPIENT;
import static nhz.http.JSONResponses.MISSING_ASSET;
import static nhz.http.JSONResponses.MISSING_QUANTITY;
import static nhz.http.JSONResponses.MISSING_RECIPIENT;
import static nhz.http.JSONResponses.NOT_ENOUGH_FUNDS;
import static nhz.http.JSONResponses.UNKNOWN_ACCOUNT;

public final class TransferAsset extends CreateTransaction {

    static final TransferAsset instance = new TransferAsset();

    private TransferAsset() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NhzException.ValidationException {

        String recipientValue = req.getParameter("recipient");
        String assetValue = req.getParameter("asset");
        String quantityValue = req.getParameter("quantity");

        if (recipientValue == null || "0".equals(recipientValue)) {
            return MISSING_RECIPIENT;
        } else if (assetValue == null) {
            return MISSING_ASSET;
        } else if (quantityValue == null) {
            return MISSING_QUANTITY;
        }

        Long recipient;
        try {
            recipient = Convert.parseUnsignedLong(recipientValue);
        } catch (RuntimeException e) {
            return INCORRECT_RECIPIENT;
        }

        Long asset;
        try {
            asset = Convert.parseUnsignedLong(assetValue);
        } catch (RuntimeException e) {
            return INCORRECT_ASSET;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityValue);
            if (quantity <= 0 || quantity >= Constants.MAX_ASSET_QUANTITY) {
                return INCORRECT_QUANTITY;
            }
        } catch (NumberFormatException e) {
            return INCORRECT_QUANTITY;
        }

        Account account = getAccount(req);
        if (account == null) {
            return UNKNOWN_ACCOUNT;
        }

        Integer assetBalance = account.getUnconfirmedAssetBalance(asset);
        if (assetBalance == null || quantity > assetBalance) {
            return NOT_ENOUGH_FUNDS;
        }

        Attachment attachment = new Attachment.ColoredCoinsAssetTransfer(asset, quantity);
        return createTransaction(req, account, recipient, 0, attachment);

    }

}
