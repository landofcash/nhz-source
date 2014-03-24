package nhz.http;

import nhz.Account;
import nhz.Attachment;
import nhz.Constants;
import nhz.NhzException;
import nhz.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nhz.http.JSONResponses.INCORRECT_ASSET;
import static nhz.http.JSONResponses.INCORRECT_PRICE;
import static nhz.http.JSONResponses.INCORRECT_QUANTITY;
import static nhz.http.JSONResponses.MISSING_ASSET;
import static nhz.http.JSONResponses.MISSING_PRICE;
import static nhz.http.JSONResponses.MISSING_QUANTITY;
import static nhz.http.JSONResponses.NOT_ENOUGH_ASSETS;
import static nhz.http.JSONResponses.UNKNOWN_ACCOUNT;

public final class PlaceAskOrder extends CreateTransaction {

    static final PlaceAskOrder instance = new PlaceAskOrder();

    private PlaceAskOrder() {}

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NhzException.ValidationException {

        String assetValue = req.getParameter("asset");
        String quantityValue = req.getParameter("quantity");
        String priceValue = req.getParameter("price");

        if (assetValue == null) {
            return MISSING_ASSET;
        } else if (quantityValue == null) {
            return MISSING_QUANTITY;
        } else if (priceValue == null) {
            return MISSING_PRICE;
        }

        long price;
        try {
            price = Long.parseLong(priceValue);
            if (price <= 0 || price > Constants.MAX_BALANCE * 100L) {
                return INCORRECT_PRICE;
            }
        } catch (NumberFormatException e) {
            return INCORRECT_PRICE;
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
            if (quantity <= 0 || quantity > Constants.MAX_ASSET_QUANTITY) {
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
            return NOT_ENOUGH_ASSETS;
        }

        Attachment attachment = new Attachment.ColoredCoinsAskOrderPlacement(asset, quantity, price);
        return createTransaction(req, account, attachment);

    }

}
