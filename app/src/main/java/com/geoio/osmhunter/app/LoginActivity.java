package com.geoio.osmhunter.app;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.apache.http.Header;
import org.apache.http.entity.ByteArrayEntity;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;


public class LoginActivity extends AccountAuthenticatorActivity {
    WebView web;
    String session_id;
    String signup_url;
    AccountManager am;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_login);

        // web page loading
        setProgressBarIndeterminateVisibility(true);

        am = AccountManager.get(this);

        web = (WebView) this.findViewById(R.id.web);
        web.setWebViewClient(new WebClient());

        Uri.Builder b = Uri.parse(getString(R.string.geoio_api_url)).buildUpon();
        AsyncHttpClient client = new AsyncHttpClient();

        // build url
        b.appendPath("user");
        b.appendPath("signup");
        signup_url = b.build().toString();

        client.get(signup_url, null, new JsonHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                // please do anything!
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {

                    JSONObject result = response.getJSONObject("result");
                    String redirect_url = result.getString("redirect_url");
                    session_id = result.getString("session_id");

                    web.loadUrl(redirect_url);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    private void finishAuthentication(Intent intent) {
        this.setAccountAuthenticatorResult(intent.getExtras());
        this.setResult(RESULT_OK, intent);
        this.finish();
    }

    public class WebClient extends WebViewClient {
        public WebClient() {
            super();
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            setProgressBarIndeterminateVisibility(false);

            Uri uri = Uri.parse(url);
            Uri base_uri = Uri.parse(getString(R.string.geoio_api_url));
            String oauth_token = uri.getQueryParameter("oauth_token");

            if (uri.getHost().equals(base_uri.getHost())) {
                if (oauth_token != null) {
                    // get the final secret
                    AsyncHttpClient client = new AsyncHttpClient();
                    JSONObject params = new JSONObject();
                    ByteArrayEntity entity;

                    try {
                        params.put("oauth_token", oauth_token);
                        params.put("session_id", session_id);
                        entity = new ByteArrayEntity(params.toString().getBytes("UTF-8"));

                        client.post(getApplicationContext(), signup_url, entity, "application/json", new JsonHttpResponseHandler() {
                            @Override
                            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                                // please do anything!
                            }

                            @Override
                            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                try {

                                    JSONObject result = response.getJSONObject("result");

                                    // create a new account
                                    Account account = new Account(result.getString("name"), getString(R.string.authenticator_account_type));
                                    Bundle account_data = new Bundle();

                                    account_data.putInt("osm_id", result.getInt("osm_id"));

                                    am.addAccountExplicitly(account, "", account_data);
                                    am.setAuthToken(account, "", result.getString("apikey"));

                                    // return the user
                                    Intent intent = new Intent();
                                    intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, result.getString("name"));

                                    finishAuthentication(intent);

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

}
