package com.onekosmos.blockidsample;

import android.util.Log;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.androidnetworking.interfaces.OkHttpResponseListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class JiraService {

    private static final String JIRA_URL = "https://onekosmos.atlassian.net/rest/api/2/issue";

    private static final String JIRA_URL_GET = "https://onekosmos.atlassian.net/rest/api/2/project/10052";
    private static final String JIRA_EMAIL = "gaurav.rane@1kosmos.com";
    private static final String JIRA_API_TOKEN = "ATATT3xFfGF0bF1cs4MV2DaDg5XfVDt4cFoI_xDGusNHqxzTxB6zrdD50jdZ2nf-klGnH1a6J9OHLHfXMAzN_Mcg7DP_2FoFgMHW-nDQ9is1mJjx6R7zKA3GVmIVjor3nckRqJEMlBruxAOSha3i8R3JCTagiuSpJut8L6vr_zVXf0iLGzUtUbk=E650DAC1"; // Or password for Jira Server

    public void createJiraTicket() throws IOException {
        // Create the JSON body for the request
        JSONObject issueData = new JSONObject();
        try {
            issueData.put("fields", new JSONObject()
                    .put("summary", "This is a test issue created from Android")
                    .put("issuetype", new JSONObject().put("name", "Bug"))  // Replace 'Bug' with your issue type
                    .put("project", new JSONObject().put("key", "FEED"))  // Replace PROJECT_KEY with your Jira project key
                    //.put("components", components)
                    .put("description", "Test Detailed description of the issue."));
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d("JiraPayload", issueData.toString());

        // Add basic authentication header (Base64 encoded)
        //getJIRAIssue();
        String credential = Credentials.basic(JIRA_EMAIL, JIRA_API_TOKEN);
        AndroidNetworking.post(JIRA_URL)
                .addHeaders("Authorization", credential)
                .addHeaders("Accept", "*/*")
                .addHeaders("Content-Type", "application/json")
                .addJSONObjectBody(issueData)
                .doNotCacheResponse()
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.e("jira ticket success for POST API:", "success:" + response.toString());
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.e("jira ticket failure for POST API:", "Error:" + anError.toString());
                    }
                });
    }

    public void getJIRAIssue() {
        String credential = Credentials.basic(JIRA_EMAIL, JIRA_API_TOKEN);
        AndroidNetworking.get(JIRA_URL_GET)
                .addHeaders("Authorization", credential)
                .addHeaders("Accept", "*/*")
                .addHeaders("Content-Type", "application/json")
                .doNotCacheResponse()
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.e("jira ticket success for GET API:", "success:" + response.toString());
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.e("jira ticket failure for GET API:", "Error:" + anError.toString());
                    }
                });
    }


}
