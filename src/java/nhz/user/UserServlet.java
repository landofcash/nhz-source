package nhz.user;

import nhz.Nhz;
import nhz.NhzException;
import nhz.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static nhz.user.JSONResponses.DENY_ACCESS;
import static nhz.user.JSONResponses.INCORRECT_REQUEST;
import static nhz.user.JSONResponses.POST_REQUIRED;

public final class UserServlet extends HttpServlet  {

    abstract static class UserRequestHandler {
        abstract JSONStreamAware processRequest(HttpServletRequest request, User user) throws NhzException, IOException;
        boolean requirePost() {
            return false;
        }
    }

    private static final boolean enforcePost = Nhz.getBooleanProperty("nhz.uiServerEnforcePOST");

    private static final Map<String,UserRequestHandler> userRequestHandlers;

    static {
        Map<String,UserRequestHandler> map = new HashMap<>();
        map.put("generateAuthorizationToken", GenerateAuthorizationToken.instance);
        map.put("getInitialData", GetInitialData.instance);
        map.put("getNewData", GetNewData.instance);
        map.put("lockAccount", LockAccount.instance);
        map.put("removeActivePeer", RemoveActivePeer.instance);
        map.put("removeBlacklistedPeer", RemoveBlacklistedPeer.instance);
        map.put("removeKnownPeer", RemoveKnownPeer.instance);
        map.put("sendMoney", SendMoney.instance);
        map.put("unlockAccount", UnlockAccount.instance);
        userRequestHandlers = Collections.unmodifiableMap(map);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        process(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        process(req, resp);
    }

    private void process(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private");
        resp.setHeader("Pragma", "no-cache");
        resp.setDateHeader("Expires", 0);

        User user = null;

        try {

            String userPasscode = req.getParameter("user");
            if (userPasscode == null) {
                return;
            }
            user = Users.getUser(userPasscode);

            if (Users.allowedUserHosts != null && ! Users.allowedUserHosts.contains(req.getRemoteHost())) {
                user.enqueue(DENY_ACCESS);
                return;
            }

            String requestType = req.getParameter("requestType");
            if (requestType == null) {
                user.enqueue(INCORRECT_REQUEST);
                return;
            }

            UserRequestHandler userRequestHandler = userRequestHandlers.get(requestType);
            if (userRequestHandler == null) {
                user.enqueue(INCORRECT_REQUEST);
                return;
            }

            if (enforcePost && userRequestHandler.requirePost() && ! "POST".equals(req.getMethod())) {
                user.enqueue(POST_REQUIRED);
                return;
            }

            JSONStreamAware response = userRequestHandler.processRequest(req, user);
            if (response != null) {
                user.enqueue(response);
            }

        } catch (RuntimeException|NhzException e) {

            Logger.logMessage("Error processing GET request", e);
            if (user != null) {
                JSONObject response = new JSONObject();
                response.put("response", "showMessage");
                response.put("message", e.toString());
                user.enqueue(response);
            }

        } finally {

            if (user != null) {
                user.processPendingResponses(req, resp);
            }

        }

    }

}