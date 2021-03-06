package nhz.http;

import nhz.Block;
import nhz.Nhz;
import nhz.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nhz.http.JSONResponses.INCORRECT_BLOCK;
import static nhz.http.JSONResponses.MISSING_BLOCK;
import static nhz.http.JSONResponses.UNKNOWN_BLOCK;

public final class GetBlock extends APIServlet.APIRequestHandler {

    static final GetBlock instance = new GetBlock();

    private GetBlock() {
        super("block");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String block = req.getParameter("block");
        if (block == null) {
            return MISSING_BLOCK;
        }

        Block blockData;
        try {
            blockData = Nhz.getBlockchain().getBlock(Convert.parseUnsignedLong(block));
            if (blockData == null) {
                return UNKNOWN_BLOCK;
            }
        } catch (RuntimeException e) {
            return INCORRECT_BLOCK;
        }

        return JSONData.block(blockData);

    }

}