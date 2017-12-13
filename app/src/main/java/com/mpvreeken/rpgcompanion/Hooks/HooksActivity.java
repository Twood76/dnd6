package com.mpvreeken.rpgcompanion.Hooks;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.os.Bundle;

import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import android.widget.Toast;

import com.mpvreeken.rpgcompanion.R;
import com.mpvreeken.rpgcompanion.RPGCActivity;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HooksActivity extends RPGCActivity {

    ArrayList<Hook> hooksArray = new ArrayList<>();
    HookArrayAdapter hookArrayAdapter;
    ListView hooks_lv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hooks);
        setupLoadingAnim();


        Button new_btn = findViewById(R.id.hooks_new_btn);

        new_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, NewHookActivity.class);
                startActivity(intent);
            }
        });

        //Fetch hooks from db
        this.hooksArray = new ArrayList<>();
        this.hookArrayAdapter = new HookArrayAdapter(this, hooksArray);
        this.hooks_lv = findViewById(R.id.hooks_lv);

        showLoadingAnim();

        String hooks_url = getResources().getString(R.string.url_get_hooks);

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(hooks_url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                displayError("Could not connect to server. Please try again");
                hideLoadingAnim();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull final Response response) throws IOException {
                hideLoadingAnim();
                if (!response.isSuccessful()) {
                    onUnsuccessfulResponse(response);
                }
                else {
                    try {
                        JSONArray r = new JSONArray(response.body().string());
                        for (int i=0; i<r.length(); i++) {
                            hooksArray.add(
                                new Hook(
                                    r.getJSONObject(i).getString("id"),
                                    r.getJSONObject(i).getString("title"),
                                    r.getJSONObject(i).getString("username"),
                                    r.getJSONObject(i).getInt("user_id"),
                                    r.getJSONObject(i).getString("description"),
                                    r.getJSONObject(i).getInt("upvotes"),
                                    r.getJSONObject(i).getInt("downvotes"),
                                    r.getJSONObject(i).getInt("voted"),
                                    r.getJSONObject(i).getString("created_at"),
                                    r.getJSONObject(i).getString("updated_at")
                                )
                            );
                        }

                        //We can't update the UI on a background thread, so run on the UI thread
                        HooksActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setupUI();
                            }
                        });
                    }
                    catch (JSONException e) {
                        displayError("An unknown error occurred. Please try again");
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void setupUI() {

        hooks_lv.setAdapter(hookArrayAdapter);
        Button btn = new Button(this);
        btn.setText("Load More");

        hooks_lv.addFooterView(btn);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadMoreHooks();
            }
        });

        hooks_lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                Intent intent = new Intent(parent.getContext(), DisplayHookActivity.class);

                Bundle bundle = new Bundle();
                bundle.putString("HOOK_ID", hooksArray.get(position).getId());
                intent.putExtras(bundle);
                //intent.putExtra("hook_id", riddlesArray.get(position).getId());
                startActivity(intent);
            }
        });
    }

    public void loadMoreHooks() {
        showLoadingAnim();

        String hooks_url = getResources().getString(R.string.url_get_hooks);

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(hooks_url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                hideLoadingAnim();
                displayError("Could not connect to server. Please try again");
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull final Response response) throws IOException {
                hideLoadingAnim();
                if (!response.isSuccessful()) {
                    onUnsuccessfulResponse(response);
                }
                else {
                    try {
                        JSONArray r = new JSONArray(response.body().string());
                        for (int i=0; i<r.length(); i++) {
                            hooksArray.add(
                                    new Hook(
                                            r.getJSONObject(i).getString("id"),
                                            r.getJSONObject(i).getString("title"),
                                            r.getJSONObject(i).getString("username"),
                                            r.getJSONObject(i).getInt("user_id"),
                                            r.getJSONObject(i).getString("description"),
                                            r.getJSONObject(i).getInt("upvotes"),
                                            r.getJSONObject(i).getInt("downvotes"),
                                            r.getJSONObject(i).getInt("voted"),
                                            r.getJSONObject(i).getString("created_at"),
                                            r.getJSONObject(i).getString("updated_at")
                                    )
                            );
                        }

                        HooksActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //Get scroll position of listview, so we can retain their position after loading more
                                int firstVisibleItem = hooks_lv.getFirstVisiblePosition();
                                int oldCount = hookArrayAdapter.getCount();
                                View view = hooks_lv.getChildAt(0);
                                int pos = (view == null ? 0 :  view.getBottom());

                                //Set the new data
                                hooks_lv.setAdapter(hookArrayAdapter);

                                //Set the listview position back to where they were
                                hooks_lv.setSelectionFromTop(firstVisibleItem + hookArrayAdapter.getCount() - oldCount + 1, pos);
                            }
                        });

                    }
                    catch (JSONException e) {
                        displayError("An unknown error occurred. Please try again");
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void displayError(final String s) {
        HooksActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, s, Toast.LENGTH_LONG).show();
            }
        });
    }
}
