package nhz;

import nhz.peer.Peer;
import nhz.util.Observable;
import org.json.simple.JSONObject;

public interface BlockchainProcessor extends Observable<Block,BlockchainProcessor.Event> {

    public static enum Event {
        BLOCK_PUSHED, BLOCK_POPPED, BLOCK_GENERATED, BLOCK_SCANNED,
        RESCAN_BEGIN, RESCAN_END
    }

    Peer getLastBlockchainFeeder();

    void processPeerBlock(JSONObject request) throws NhzException;

    void fullReset();


    public static class BlockNotAcceptedException extends NhzException {

        BlockNotAcceptedException(String message) {
            super(message);
        }

    }

    public static class BlockOutOfOrderException extends BlockNotAcceptedException {

        BlockOutOfOrderException(String message) {
            super(message);
        }

	}

}
