package com.onekosmos.blockidsample;

import com.walletconnect.sign.client.Sign;

public interface WalletConnectCallback {
    /**
     * After wallet connect initialization, connection is available or not
     *
     * @param connectionState {@link Sign.Model.ConnectionState}
     */
    void onConnectionStateChange(Sign.Model.ConnectionState connectionState);

    /**
     * After Connection this method will call
     * Using this session proposal, approve or reject proposal
     *
     * @param sessionProposal {@link Sign.Model.SessionProposal}
     */
    void onSessionProposal(Sign.Model.SessionProposal sessionProposal);

    /**
     * Connection established
     *
     * @param settleSessionResponse {@link Sign.Model.SettledSessionResponse}
     */
    void onSessionSettleResponse(Sign.Model.SettledSessionResponse settleSessionResponse);

    /**
     * Connection deleted
     *
     * @param deletedSession {@link Sign.Model.DeletedSession}
     */
    void onSessionDelete(Sign.Model.DeletedSession deletedSession);

    /**
     * Sign Transaction request will receive here
     *
     * @param sessionRequest {@link Sign.Model.SessionRequest}
     */
    void onSessionRequest(Sign.Model.SessionRequest sessionRequest);

    /**
     * Error
     *
     * @param error {@link Sign.Model.Error}
     */
    void onError(Sign.Model.Error error);

    //    fun onSessionUpdateResponse(sessionUpdateResponse: Sign.Model.SessionUpdateResponse)
}
