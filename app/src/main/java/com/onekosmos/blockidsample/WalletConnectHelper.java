package com.onekosmos.blockidsample;


import android.app.Application;
import android.text.TextUtils;
import android.util.Log;

import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.walletconnect.sign.client.Sign;
import com.walletconnect.sign.client.SignClient;
import com.walletconnect.sign.client.SignInterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WalletConnectHelper {
    private static WalletConnectHelper sharedInstance;
    private static final String WALLET_CONNECT_PROD_RELAY_URL = "relay.walletconnect.com";
    private static final String chainNamespace = "eip155";
    private static final String chainReference = "1";

    /**
     * private constructor
     * restricted to this class itself
     */
    private WalletConnectHelper() {
    }

    /**
     * create instance of Singleton class {@link WalletConnectHelper}
     *
     * @return {@link WalletConnectHelper}
     */
    public static WalletConnectHelper getInstance() {
        if (sharedInstance == null) {
            sharedInstance = new WalletConnectHelper();
        }
        return sharedInstance;
    }

    public void initializeWalletConnectSDK(Application context, String projectId,
                                           Sign.Model.AppMetaData metaData,
                                           SignInterface.WalletDelegate walletDelegate) {
        if (TextUtils.isEmpty(projectId)) {
            Log.e("Init error", "project id is empty");
            return;
        }

        String relayServerUrl = "wss://" + WALLET_CONNECT_PROD_RELAY_URL + "?projectId=" +
                projectId;

        Sign.Params.Init init = null;
        try {
            init = new Sign.Params.Init(context, relayServerUrl, metaData,
                    null, Sign.ConnectionType.AUTOMATIC);

        } catch (Exception e) {
            Log.e("Init error1", "Exception --> " + e.getMessage());
            return;
        }

        SignClient.INSTANCE.initialize(init, error -> {
            if (error != null) {
                // Software caused connection abort (offline)
                Log.e("Init2 Error", error.getThrowable().getMessage());
            } else {
                Log.e("Init", "success");
            }
            return null;
        });

        SignClient.INSTANCE.setWalletDelegate(walletDelegate);
    }

    public void connect(String pairingUri) {
        Sign.Params.Pair pairingParams = new Sign.Params.Pair(pairingUri);
        SignClient.INSTANCE.pair(pairingParams, error -> {
            if (error != null) {
                // Software caused connection abort (offline)
                Log.e("Pair Error", error.getThrowable().getMessage());
            } else {
                Log.e("Pair", "success");
            }
            return null;
        });
        // look for session propose
    }

    public void approveDApp(Sign.Model.SessionProposal sessionProposal) {
        if (sessionProposal == null) {
            Log.e("Approve Dapp", "Session Proposal is empty");
            return;
        }
        Map<String, Sign.Model.Namespace.Proposal> requiredNamespaces =
                sessionProposal.getRequiredNamespaces();
        Sign.Model.Namespace.Proposal proposal = requiredNamespaces.get(chainNamespace);

        if (proposal == null) {
            Log.e("Approve Dapp", "Chain Name space not found in session proposal");
            return;
        }

        String account = chainNamespace + ":" + chainReference + ":" + "0x" +
                BlockIDSDK.getInstance().getDID();

        List<String> accounts = new ArrayList<>();
        accounts.add(account);

        List<String> methods = proposal.getMethods();
        List<String> events = proposal.getEvents();
        Sign.Model.Namespace.Session session = new Sign.Model.Namespace.Session(
                accounts, methods, events, null);

        Map<String, Sign.Model.Namespace.Session> namespaces = new HashMap<>();
        namespaces.put(chainNamespace, session);

        Sign.Params.Approve approve = new Sign.Params.Approve(
                sessionProposal.getProposerPublicKey(), namespaces, null);

        SignClient.INSTANCE.approveSession(approve, error -> {
            if (error != null) {
                Log.e("Approve Proposal", error.getThrowable().getMessage());
            } else {
                Log.e("Proposal", "Approved");
            }
            return null;
        });
    }

    public void rejectDApp(Sign.Model.SessionProposal sessionProposal) {
        Sign.Params.Reject reject = new Sign.Params.Reject(sessionProposal.getProposerPublicKey(),
                "Reject Session", 406);
        SignClient.INSTANCE.rejectSession(reject, error -> {
            if (error != null) {
                Log.e("Reject Dapp", error.getThrowable().getMessage());
            } else {
                Log.e("Rejected", "Successfully");
            }
            return null;
        });
    }

    public List<Sign.Model.Session> getConnectedSessions() {
        return SignClient.INSTANCE.getListOfSettledSessions();
    }

    public void disconnect(String sessionTopic) {
        Sign.Params.Disconnect disconnect = new Sign.Params.Disconnect(sessionTopic);
        SignClient.INSTANCE.disconnect(disconnect, error -> {
            if (error != null) {
                Log.e("Disconnect Error", error.getThrowable().getMessage());
            } else {
                Log.e("Disconnected", "Successfully");
            }
            return null;
        });
    }
}