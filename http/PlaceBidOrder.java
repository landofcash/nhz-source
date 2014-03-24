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
import static nhz.http.JSONResponses.UNKNOWN_ACCOUNT;

public final class PlaceBidOrder extends CreateTransaction {

    static final PlaceBidOrder instance = new PlaceBidOrder();

    private PlaceBidOrder() {}

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
        } catch (NumberFormatException e) {
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

        Attachment attachment = new Attachment.ColoredCoinsBidOrderPlacement(asset, quantity, price);
        return createTransaction(req, account, attachment);

    }

}
