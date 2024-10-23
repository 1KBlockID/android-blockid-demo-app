package com.onekosmos.blockidsample;

import android.util.Log;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.ParsedRequestListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Credentials;

public class JiraService {

    private static final String JIRA_URL = "https://onekosmos.atlassian.net/rest/api/2/issue";
    private static final String JIRA_EMAIL = "gaurav.rane@1kosmos.com";
    private static final String JIRA_API_TOKEN = "ATATT3xFfGF0Ou6oo7fAKurEXbFCtjoCKpFuJOlEWiUSCH8azZ7m3FesMm72yWxVcs14a006mFUgY4Go_MLgaUvYrExxALoFNgfijI6z-8pPwzoZiQ6FfcVyYE74YS2lfGPLEPNv3ymYFI_-85C-zYexLjG9dVV36DwwjFDYlSuH3Zg4pVXAAj8=305A1735"; // Or password for Jira Server

    public void createJiraTicket() throws IOException {
        // Create the JSON body for the request
        JSONObject issueData = new JSONObject();
        JSONArray components = new JSONArray();

        try {
            JSONObject android = new JSONObject();
            android.put("id", "10123");
            components.put(android);
            issueData.put("fields", new JSONObject()
                    .put("project", new JSONObject().put("id", "10016"))  // Replace PROJECT_KEY with your Jira project key
                    .put("components", components)
                    .put("summary", "This is a test issue created from Android")
                    .put("description", "Test Detailed description of the issue")
                    .put("issuetype", new JSONObject().put("name", "Bug")));    // Replace 'Bug' with your issue type
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d("JiraPayload", issueData.toString());
        // Add basic authentication header (Base64 encoded)
        String credential = Credentials.basic(JIRA_EMAIL, JIRA_API_TOKEN);
        AndroidNetworking.post(JIRA_URL)
                .addHeaders("Authorization", credential)
                .addHeaders("Content-Type", "application/json")
                .addApplicationJsonBody(issueData)
                .doNotCacheResponse()
                .build()
                .getAsObject(JSONObject.class, new ParsedRequestListener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("jira ticket response:", response.toString());

                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.e("jira ticket failure:", anError.getErrorBody());
                    }
                });
    }
}
