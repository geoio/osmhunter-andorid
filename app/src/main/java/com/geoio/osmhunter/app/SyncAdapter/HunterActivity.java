package com.geoio.osmhunter.app.SyncAdapter;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.os.Bundle;

import com.geoio.osmhunter.app.R;

import java.io.IOException;

public class HunterActivity extends Activity {
    public Bundle user;
    public Boolean accountReady = false;
    private AccountManager am;
    private String account_type;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        am = AccountManager.get(this);
        account_type = this.getString(R.string.authenticator_account_type);

        am.getAuthTokenByFeatures(account_type, "", null, this, null, null, new AccountManagerCallback<Bundle>() {
            @Override
            public void run(AccountManagerFuture<Bundle> future) {
                try {
                    user = future.getResult();
                    accountReady = true;

                    accountReady();

                } catch (OperationCanceledException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (AuthenticatorException e) {
                    e.printStackTrace();
                }
            }
        }, null);
    }

    /**
     * This function gets called when user is filled. Please override it in your activity.
     */
    public void accountReady() {

    }

    public void accountInvalidate() {
        am.invalidateAuthToken(account_type, user.getString(AccountManager.KEY_AUTHTOKEN));
    }
}
