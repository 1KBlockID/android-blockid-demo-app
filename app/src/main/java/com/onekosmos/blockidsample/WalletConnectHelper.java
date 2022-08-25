package com.onekosmos.blockidsample;


import android.app.Application;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.Keep;

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
            Log.e("Approve DApp", "Chain Name space not found in session proposal");
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

    public void signTransaction(Sign.Model.SessionRequest sessionRequest) {
        Sign.Model.SessionRequest.JSONRPCRequest request = sessionRequest.getRequest();

        String method = request.getMethod();
        if (!method.equalsIgnoreCase("eth_signTransaction")) {
            Log.e("Sign Transaction Error", "Wrong method");
            return;
        }

        // FIXME ask for wallet
        BIDWallet wallet = BlockIDSDK.getInstance().getWallet();
        // demo -- e56da0e170b5e09a8bb8f1b693392c7d56c3739a9c75740fbc558a2877868540
        // my   -- 00f8e1eaeacd1e354627d4b1d8c7719e95129a9f06fe212557a99a411a6484bdbc
        // e036c8a534694a458a1d7ade94edb617184b153d
        // APjh6urNHjVGJ9Sx2MdxnpUSmp8G/iElV6maQRpkhL28
        // WJgy4+kWWcudCcCnk1zsT8P77Oh9s90fNZyNnHpairg9RiDMKrEJcMPDxJiw8NLWzwU17mwPCvBefKP+T3/Qgg==

        String base64PrivateKey = wallet.getPrivateKey();
        String privateKey = Hex.encodeHexString(Base64.decode(base64PrivateKey, Base64.NO_WRAP));
        Credentials credentials = Credentials.create(privateKey);
        String param = request.getParams().replaceAll("[\\[\\]]", "");

        SessionRequestParams requestParams = BIDUtil.JSONStringToObject(param, SessionRequestParams.class);
//        {
//            "from": "0xe036c8a534694a458a1d7ade94edb617184b153d",
//                "to": "0xe036c8a534694a458a1d7ade94edb617184b153d",
//                "data": "0x",
//                "nonce": "0x00",
//                "gasPrice": "0x02790d2551",
//                "gasLimit": "0x5208",
//                "value": "0x00"
//        }

        String from = requestParams.from;
        String to = requestParams.to;

        String amount = Long.decode(requestParams.value).toString();
        BigInteger nonce = BigInteger.valueOf(Long.decode(requestParams.nonce));
        BigInteger gasPrice = BigInteger.valueOf(Long.decode(requestParams.gasPrice));
        BigInteger gasLimit = BigInteger.valueOf(Long.decode(requestParams.gasLimit));
        RawTransaction rawTransaction = RawTransaction.createEtherTransaction(
                nonce,
                gasPrice,
                gasLimit,
                to,
                Convert.toWei(amount, Convert.Unit.ETHER).toBigInteger()
        );

        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, 1L, credentials);
        String hexValue = Numeric.toHexString(signedMessage);

        Sign.Params.Response response = new Sign.Params.Response(sessionRequest.getTopic(),
                new Sign.Model.JsonRpcResponse.JsonRpcResult(
                        request.getId(),
                        hexValue
                ));

        SignClient.INSTANCE.respond(response, error -> {
            if (error != null) {
                Log.e("Sign Transaction Error", error.getThrowable().getMessage());
            } else {
                Log.e("Sign Transaction", "Successfully");
            }
            return null;
        });
    }

    public void rejectTransaction(Sign.Model.SessionRequest sessionRequest) {
        Sign.Params.Response result = new Sign.Params.Response(
                sessionRequest.getTopic(),
                new Sign.Model.JsonRpcResponse.JsonRpcError(
                        sessionRequest.getRequest().getId(),
                        500,
                        "Kotlin Wallet Error"));

        SignClient.INSTANCE.respond(result, error -> {
            if (error != null) {
                Log.e("Reject Transaction Error", error.getThrowable().getMessage());
            } else {
                Log.e("Reject Transaction", "Successfully");
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

    @Keep
    public class SessionRequestParams {
        public String from;
        public String to;
        public String data;
        public String nonce;
        public String gasPrice;
        public String gasLimit;
        public String value;
    }

    private String byteArrayToHex(byte[] bytes) {
        char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}