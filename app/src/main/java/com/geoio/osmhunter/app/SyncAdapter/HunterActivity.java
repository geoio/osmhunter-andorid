package com.geoio.osmhunter.app.SyncAdapter;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;

import com.geoio.osmhunter.app.R;
import com.joshdholtz.sentry.Sentry;

import java.io.IOException;

public class HunterActivity extends FragmentActivity {

    public static Bundle user;
    public static Boolean accountReady = false;
    private AccountManager am;
    private String account_type;

    public static Resources res;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // A working Sentry shouldn't be a dependency of this project!
        String dsn = getString(R.string.sentry_dsn);
        if(dsn.length() == 0) {
            dsn = "https://xx:xx@xx.xx/xx";
        }
        Sentry.init(this, getString(R.string.sentry_url), dsn);


        am = AccountManager.get(this);
        account_type = this.getString(R.string.authenticator_account_type);

        res = getResources();
    }

    @Override
    protected void onResume() {
        super.onResume();
        accountGet();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * This function gets called when user is filled. Please override it in your activity.
     */
    public void accountReady() {

    }

    public void accountInvalidate() {
        am.invalidateAuthToken(account_type, user.getString(AccountManager.KEY_AUTHTOKEN));
        accountGet();
    }

    private void accountGet() {
        am.getAuthTokenByFeatures(account_type, "", null, this, null, null, new AccountManagerCallback<Bundle>() {
            @Override
            public void run(AccountManagerFuture<Bundle> future) {
                try {
                    user = future.getResult();
                    accountReady = true;
                    accountReady();

                } catch (OperationCanceledException e) {
                    Sentry.captureException(e);
                    e.printStackTrace();
                } catch (IOException e) {
                    Sentry.captureException(e);
                    e.printStackTrace();
                } catch (AuthenticatorException e) {
                    Sentry.captureException(e);
                    e.printStackTrace();
                }
            }
        }, null);
    }
}
