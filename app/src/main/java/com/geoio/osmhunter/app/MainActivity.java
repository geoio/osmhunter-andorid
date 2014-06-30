package com.geoio.osmhunter.app;

import android.accounts.AccountManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.geoio.osmhunter.app.SyncAdapter.HunterActivity;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.squareup.picasso.Picasso;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends HunterActivity {

    private SharedPreferences cache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_main);

        // user info loading
        setProgressBarIndeterminateVisibility(true);

        // user info caching
        cache = getPreferences(MODE_PRIVATE);

        Button button_discover = (Button) findViewById(R.id.button_discover);
        button_discover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), MapActivity.class);
                startActivity(intent);
            }
        });

        Button button_leaderboard = (Button) findViewById(R.id.button_leaderboard);
        button_leaderboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), LeaderboardActivity.class);
                startActivity(intent);
            }
        });

        Button button_nearby = (Button) findViewById(R.id.button_lucky);
        button_nearby.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), NearbyBuildingsActivity.class);
                startActivity(intent);
            }
        });

        Button button_about = (Button) findViewById(R.id.button_about);
        button_about.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), AboutActivity.class);
                startActivity(intent);
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setAccountInfo() {
        final ImageView imageView = (ImageView) findViewById(R.id.avatar);
        final TextView usernameView = (TextView) findViewById(R.id.username);
        final TextView pointsView = (TextView) findViewById(R.id.points);

        if(cache.contains("username") && cache.contains("image") && cache.contains("points")) {
            Picasso.with(getApplicationContext()).load(cache.getString("image", null)).into(imageView);

            usernameView.setText(cache.getString("username", null));

            int points = cache.getInt("points", 0);
            pointsView.setText(res.getQuantityString(R.plurals.points, points, points));
        }
    }

    @Override
    public void accountReady() {
        Uri.Builder b = Uri.parse(getString(R.string.geoio_api_url)).buildUpon();
        AsyncHttpClient client = new AsyncHttpClient();
        final SharedPreferences.Editor cacheEditor = cache.edit();

        // check if we have cached user information
        setAccountInfo();

        // build url
        b.appendPath("user");
        b.appendQueryParameter("apikey", user.getString(AccountManager.KEY_AUTHTOKEN));
        String url = b.build().toString();

        client.get(url, null, new JsonHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.error_api), Toast.LENGTH_LONG);
                toast.show();
            }
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    if(statusCode == 401) {
                        Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.error_api_authorization), Toast.LENGTH_LONG);
                        toast.show();

                        accountInvalidate();
                        return;
                    }

                    JSONObject result = response.getJSONObject("result");

                    cacheEditor.putString("username", result.getString("display_name"));
                    cacheEditor.putString("image", result.getString("image"));
                    cacheEditor.putInt("points", result.getInt("points"));

                    cacheEditor.commit();
                    setAccountInfo();

                    setProgressBarIndeterminateVisibility(false);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void showUserInfo() {

    }

}
