package com.onekosmos.blockidsample;


import android.app.Application;
import android.text.TextUtils;
import android.util.Base64;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockid.sdk.utils.BIDUtil;
import com.onekosmos.blockid.sdk.wallet.BIDWallet;
import com.walletconnect.sign.client.Sign;
import com.walletconnect.sign.client.SignClient;
import com.walletconnect.sign.client.SignInterface;

import org.apache.commons.codec.binary.Hex;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WalletConnectHelper {
    private static WalletConnectHelper sharedInstance;
    private static final String WALLET_CONNECT_PROD_RELAY_URL = "relay.walletconnect.com";
    private static final String chainNamespace = "eip155";
    private static final String chainReference = "1";
    private WalletConnectCallback walletConnectCallback;
    private final SignInterface.WalletDelegate walletDelegate = new SignInterface.WalletDelegate() {
        @Override
        public void onConnectionStateChange(@NonNull Sign.Model.ConnectionState connectionState) {
            walletConnectCallback.onConnectionStateChange(connectionState);
        }

        @Override
        public void onError(@NonNull Sign.Model.Error error) {
            walletConnectCallback.onError(error);
        }

        @Override
        public void onSessionProposal(@NonNull Sign.Model.SessionProposal sessionProposal) {
            walletConnectCallback.onSessionProposal(sessionProposal);
        }

        @Override
        public void onSessionSettleResponse(@NonNull Sign.Model.SettledSessionResponse
                                                    settledSessionResponse) {
            walletConnectCallback.onSessionSettleResponse((Sign.Model.SettledSessionResponse.Result)
                    settledSessionResponse);
        }


        @Override
        public void onSessionDelete(@NonNull Sign.Model.DeletedSession deletedSession) {
            walletConnectCallback.onSessionDelete(deletedSession);
        }

        @Override
        public void onSessionRequest(@NonNull Sign.Model.SessionRequest sessionRequest) {
            walletConnectCallback.onSessionRequest(sessionRequest);
        }

        @Override
        public void onSessionUpdateResponse(@NonNull Sign.Model.SessionUpdateResponse
                                                    sessionUpdateResponse) {
            // FIXME tbd with vinoth
        }
    };

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
    // FIXME need to add check SDK Ready
    public static WalletConnectHelper getInstance() {
        if (sharedInstance == null) {
            sharedInstance = new WalletConnectHelper();
        }

        return sharedInstance;
    }

    public void initializeWalletConnectSDK(@NonNull Application application,
                                           @NonNull String projectId,
                                           @NonNull Sign.Model.AppMetaData metaData,
                                           @NonNull WalletConnectCallback callback) {
        sharedInstance.walletConnectCallback = callback;
        if (TextUtils.isEmpty(projectId)) {
            callback.onError(new Sign.Model.Error(new Throwable("ProjectId is empty")));
            return;
        }

        String relayServerUrl = "wss://" + WALLET_CONNECT_PROD_RELAY_URL + "?projectId=" +
                projectId;

        Sign.Params.Init init = null;

        try {
            init = new Sign.Params.Init(application, relayServerUrl, metaData, null,
                    Sign.ConnectionType.AUTOMATIC);

        } catch (Exception exception) {
            callback.onError(new Sign.Model.Error(exception));
            return;
        }

        SignClient.INSTANCE.initialize(init, error -> {
            callback.onError(error);
            return null;
        });

        SignClient.INSTANCE.setWalletDelegate(sharedInstance.walletDelegate);
    }

    public void connect(@NonNull String pairingUri) {
        Sign.Params.Pair pairingParams = new Sign.Params.Pair(pairingUri);
        SignClient.INSTANCE.pair(pairingParams, error -> {
            walletConnectCallback.onError(error);
            return null;
        });
    }

    public void approveConnectionRequest(@NonNull Sign.Model.SessionProposal sessionProposal) {
        Map<String, Sign.Model.Namespace.Proposal> requiredNamespaces =
                sessionProposal.getRequiredNamespaces();
        Sign.Model.Namespace.Proposal proposal = requiredNamespaces.get(chainNamespace);

        if (proposal == null) {
            walletConnectCallback.onError(new Sign.Model.Error(
                    new Throwable("Invalid namespace")));
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
            walletConnectCallback.onError(error);
            return null;
        });
    }

    public void rejectConnectionRequest(@NonNull Sign.Model.SessionProposal sessionProposal) {
        //noinspection ConstantConditions
        if (sessionProposal == null)
            return;

        Sign.Params.Reject reject = new Sign.Params.Reject(sessionProposal.getProposerPublicKey(),
                "Reject Session", 406);
        SignClient.INSTANCE.rejectSession(reject, error -> {
            walletConnectCallback.onError(error);
            return null;
        });
    }

    public void signTransaction(Sign.Model.SessionRequest sessionRequest) {
        Sign.Model.SessionRequest.JSONRPCRequest request = sessionRequest.getRequest();

        String method = request.getMethod();
        if (!method.equalsIgnoreCase("eth_signTransaction")) {
            walletConnectCallback.onError(new Sign.Model.Error(
                    new Throwable("Invalid session request")));
            return;
        }

        // FIXME ask for wallet
        BIDWallet wallet = BlockIDSDK.getInstance().getWallet();
        String privateKey = Hex.encodeHexString(Base64.decode(wallet.getPrivateKey(),
                Base64.NO_WRAP));
        Credentials credentials = Credentials.create(privateKey);
        String param = request.getParams().replaceAll("[\\[\\]]", "");
        SessionRequestParams requestParams = BIDUtil.JSONStringToObject(param,
                SessionRequestParams.class);
        String to = requestParams.to;
        String amount = Long.decode(requestParams.value).toString();
        BigInteger nonce = new BigInteger(requestParams.nonce.substring(2), 16);
        BigInteger gasPrice = new BigInteger(requestParams.gasPrice.substring(2), 16);
        BigInteger gasLimit = new BigInteger(requestParams.gasLimit.substring(2), 16);
        RawTransaction rawTransaction = RawTransaction.createEtherTransaction(
                nonce,
                gasPrice,
                gasLimit,
                to,
                Convert.toWei(amount, Convert.Unit.ETHER).toBigInteger());

        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, 1L,
                credentials);
        String hexValue = Numeric.toHexString(signedMessage);
        Sign.Params.Response response = new Sign.Params.Response(sessionRequest.getTopic(),
                new Sign.Model.JsonRpcResponse.JsonRpcResult(
                        request.getId(),
                        hexValue
                ));
        SignClient.INSTANCE.respond(response, error -> {
            walletConnectCallback.onError(error);
            return null;
        });
    }

    public void rejectTransaction(Sign.Model.SessionRequest sessionRequest) {
        Sign.Params.Response result = new Sign.Params.Response(sessionRequest.getTopic(),
                new Sign.Model.JsonRpcResponse.JsonRpcError(
                        sessionRequest.getRequest().getId(),
                        500,
                        "Kotlin Wallet Error"));

        SignClient.INSTANCE.respond(result, error -> {
            walletConnectCallback.onError(error);
            return null;
        });
    }

    public List<Sign.Model.Session> getConnectedSessions() {
        return SignClient.INSTANCE.getListOfSettledSessions();
    }

    public void disconnect(String sessionTopic) {
        Sign.Params.Disconnect disconnect = new Sign.Params.Disconnect(sessionTopic);
        SignClient.INSTANCE.disconnect(disconnect, error -> {
            walletConnectCallback.onError(error);
            return null;
        });
    }

    @Keep
    public static class SessionRequestParams {
        public String from;
        public String to;
        public String data;
        public String nonce;
        public String gasPrice;
        public String gasLimit;
        public String value;
    }
}