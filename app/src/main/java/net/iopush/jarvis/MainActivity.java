package net.iopush.jarvis;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.support.design.widget.FloatingActionButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.RequestQueue;
import com.android.volley.TimeoutError;
import com.android.volley.toolbox.Volley;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.NoConnectionError;
import com.android.volley.NetworkError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.util.Log;

import android.speech.tts.TextToSpeech;



public class MainActivity extends AppCompatActivity {

    private FloatingActionButton btnSpeak;
    private RecyclerView recyclerViewConversation;
    private ProgressBar pBarWaitingJArvis;
    private List<ConversationObject> jarvisConversationList = new ArrayList<>();
    // TODO - Update alue
    private final int REQ_CODE_SPEECH_INPUT = 100;
    TextToSpeech ttsEngine;

    private String serverUrl;
    private String serverPort;
    private String serverKey;
    private Boolean sttAtStart;
    private Boolean muteRemoteJarvis;
    private Boolean muteLocalJarvis;
    private String jarvisOrder;
    private String jarvisTrigger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup progressbar
        pBarWaitingJArvis = (ProgressBar) findViewById(R.id.pBarWaitingJArvis);

        // Setup recyclerView
        recyclerViewConversation = (RecyclerView) findViewById(R.id.recyclerViewConversation);
        recyclerViewConversation.setLayoutManager(new LinearLayoutManager(this));
        jarvisConversationList.add(new ConversationObject("", getString(R.string.tap_on_mic)));
        recyclerViewConversation.setAdapter(new ConversationAdapter(jarvisConversationList));

        // Setup microphone button
        btnSpeak = (FloatingActionButton) findViewById(R.id.btnSpeak);
        btnSpeak.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                promptSpeechInput();
            }
        });


        // Init TTS
        ttsEngine = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    // TODO - https://developer.android.com/reference/android/speech/tts/TextToSpeech.html#isLanguageAvailable(java.util.Locale)
                    ttsEngine.setLanguage(Locale.getDefault());
                } else {
                    Log.w("Jarvis", "TTS init failed");
                }
            }
        });

        // Get preferences
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        serverUrl = SP.getString("serverUrl", "NA");
        serverPort = SP.getString("serverPort", "NA");
        serverKey = SP.getString("serverKey", "");
        sttAtStart = SP.getBoolean("sttAtStart", false);
        muteRemoteJarvis = SP.getBoolean("muteRemoteJarvis", false);
        muteLocalJarvis = SP.getBoolean("muteLocalJarvis", false);

        if (serverUrl == "NA") {
            Intent i = new Intent(this, MyPreferencesActivity.class);
            startActivity(i);
        } else {
            // Get Jarvis-core configuration
            updateCoreConfig();
            if (sttAtStart == true) {
                promptSpeechInput();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first

        // TODO : Better to call pref change listener
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        serverUrl = SP.getString("serverUrl", "NA");
        serverPort = SP.getString("serverPort", "NA");
        serverKey = SP.getString("serverKey", "");
        muteRemoteJarvis = SP.getBoolean("muteRemoteJarvis", false);
        muteLocalJarvis = SP.getBoolean("muteLocalJarvis", false);
        // Add "http;//" if it is missing, test only the first 4 characters in case of secure address
        if (!serverUrl.substring(0, 4).equals("http")) {
            serverUrl = "http://" + serverUrl;
            SharedPreferences.Editor editor = SP.edit();
            editor.putString("serverUrl", serverUrl);
            editor.commit();
        }
        updateCoreConfig();
    }

    /**
     * Showing google speech input dialog
     * */
    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
            // Hide mic button, show progressbar
            pBarWaitingJArvis.setVisibility(View.VISIBLE);
            btnSpeak.setVisibility(View.INVISIBLE);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }

    }

    /**
     * Receiving speech input
     * */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: { // Result from STT
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    jarvisOrder = result.get(0);
                    jarvisConversationList.add(0, new ConversationObject("You", jarvisOrder));
                    recyclerViewConversation.getAdapter().notifyItemInserted(0);
                    recyclerViewConversation.smoothScrollToPosition(0);
                    Log.i("Jarvis", "STT: " + jarvisOrder);

                    // Instantiate the RequestQueue.
                    RequestQueue queue = Volley.newRequestQueue(this);
                    String requestUrl = serverUrl+":"+serverPort + "/";

                    // Instantiate JSON object
                    try {
                        final JSONObject jsonBody = new JSONObject();
                        jsonBody.put("order", jarvisOrder);
                        jsonBody.put("key", serverKey);
                        jsonBody.put("mute", muteRemoteJarvis.toString());

                        // Request a string response from the provided URL.
                        // TODO - Change to JSONArrayRequest if Json-api change
                        StringRequest stringRequest = new StringRequest(Request.Method.POST, requestUrl,
                                new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                        // Parse answer
                                        try {
                                            JSONArray jObject = new JSONArray(response);
                                            Log.i("Jarvis", "jObject" + jObject.toString());
                                            for ( int i=0; i<jObject.length(); i++ ) {
                                                JSONObject c = jObject.getJSONObject(i);
                                                Log.i("Jarvis", "Answer: " + c.toString());
                                                jarvisConversationList.add(0, new ConversationObject(jarvisTrigger, c.getString(jarvisTrigger)));
                                                recyclerViewConversation.getAdapter().notifyItemInserted(0);
                                                recyclerViewConversation.smoothScrollToPosition(0);
                                                if (!muteLocalJarvis) {
                                                    if (android.os.Build.VERSION.SDK_INT >= 21) {
                                                        ttsEngine.speak(c.getString(jarvisTrigger), TextToSpeech.QUEUE_ADD, null, c.getString(jarvisTrigger));
                                                    } else {
                                                        ttsEngine.speak(c.getString(jarvisTrigger), TextToSpeech.QUEUE_ADD, null);
                                                    }
                                                }
                                            }

                                        } catch (final JSONException e) {
                                            Log.e("Jarvis", "Json parsing error: " + e.getMessage());
                                            Toast.makeText(getApplicationContext(),
                                                    "Json parsing error: " + e.getMessage(),
                                                    Toast.LENGTH_LONG)
                                                    .show();

                                        }
                                    }
                                }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                String defaultMessage = new String(getString(R.string.volleyError));
                                Log.e("Jarvis-Volley", error.toString());
                                if (error instanceof ServerError) {
                                    // Wrong port number
                                    defaultMessage = getString(R.string.timeoutServerNetworkError);
                                    Log.e("Jarvis-Volley", defaultMessage);
                                } else if (error instanceof NetworkError || error instanceof TimeoutError) {
                                    // Jarvis down or wrong address
                                    defaultMessage = getString(R.string.timeoutServerNetworkError);
                                    Log.e("Jarvis-Volley", defaultMessage );
                                } else if (error instanceof NoConnectionError) {
                                    defaultMessage = getString(R.string.timeoutServerNetworkError);
                                    Log.e("Jarvis-Volley", defaultMessage);
                                }
                                NetworkResponse response = error.networkResponse;
                                if(response != null && response.data != null){
                                    switch(response.statusCode) {
                                        case 400:
                                            String json = new String(response.data);
                                            if (json.contains("Invalid API Key")) {
                                                defaultMessage = getString(R.string.invalidAPIKey);
                                            }
                                            if (json.contains("Missing API Key") ||
                                                    json.contains("Empty API Key")) {
                                                defaultMessage = getString(R.string.missingAPIKey);
                                            }
                                            break;
                                    }
                                }
                                // TODO - Snackbar action button
                                Snackbar snackbarVolleyError = Snackbar
                                        .make(findViewById(R.id.mainActivity), defaultMessage, Snackbar.LENGTH_LONG);

                                snackbarVolleyError.show();
                            }
                        }){
                            @Override
                            public byte[] getBody() {
                                try {
                                    return jsonBody.toString().getBytes("utf-8");
                                } catch (UnsupportedEncodingException uee) {
                                    Log.e("Jarvis", "Unsupported Encoding while trying to get the bytes of "+jsonBody.toString()+" using utf-8");
                                    return null;
                                }
                            }
                            @Override
                            public Map<String, String> getHeaders() {
                                Map<String,String> params = new HashMap<String, String>();
                                params.put("Content-Type","application/json; charset=utf-8");
                                return params;
                            }
                        };
                        // Add the request to the RequestQueue.
                        queue.add(stringRequest);
                    } catch (final JSONException e) {
                        Log.e("Jarvis", e.toString());
                    }
                }
                // Hide progressBar show mic button.
                btnSpeak.setVisibility(View.VISIBLE);
                pBarWaitingJArvis.setVisibility(View.INVISIBLE);
                break;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent i = new Intent(this, MyPreferencesActivity.class);
            startActivity(i);
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateCoreConfig() {
        // TODO - Code refactoring (With other volley call)
        RequestQueue queue = Volley.newRequestQueue(this);
        String requestUrl = serverUrl+ ":" + serverPort + "/?action=get_config&key=" + serverKey;
        Log.i("Jarvis", "RequestURL: " + requestUrl);

        // Request a string response from the provided URL.
        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, requestUrl, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        jarvisTrigger = "Jarvis"; // Default value
                        try {
                            // Get Jarvis name (Trigger word)
                            jarvisTrigger = response.getString("trigger");

                        } catch (final JSONException e) {
                            Log.e("Jarvis", e.toString());
                        }
                        Log.i("Jarvis", "Trigger name:" + jarvisTrigger);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String defaultMessage = new String(getString(R.string.volleyError));
                        Log.e("Volley", error.toString());
                        if (error instanceof ServerError) {
                            // Wrong port number
                            defaultMessage = getString(R.string.timeoutServerNetworkError);
                            Log.e("Jarvis-Volley", defaultMessage);
                        } else if (error instanceof NetworkError || error instanceof TimeoutError) {
                            // Jarvis down or wrong address
                            defaultMessage = getString(R.string.timeoutServerNetworkError);
                            Log.e("Jarvis-Volley", defaultMessage );
                        } else if (error instanceof NoConnectionError) {
                            defaultMessage = getString(R.string.timeoutServerNetworkError);
                            Log.e("Jarvis-Volley", defaultMessage);
                        }
                        NetworkResponse response = error.networkResponse;
                        if(response != null && response.data != null) {
                            switch(response.statusCode) {
                                case 400:
                                    String json = new String(response.data);
                                    Log.i("Jarvis", "Error json: " + json.toString());
                                    if (json.contains("Invalid API Key")) {
                                        defaultMessage = getString(R.string.invalidAPIKey);
                                    }
                                    if (json.contains("Missing API Key") ||
                                            json.contains("Empty API Key")) {
                                        defaultMessage = getString(R.string.missingAPIKey);
                                    }
                                    break;
                            }
                        }
                        // TODO - Snackbar action button
                        Snackbar snackbarVolleyError = Snackbar
                                .make(findViewById(R.id.mainActivity), defaultMessage, Snackbar.LENGTH_LONG);

                        snackbarVolleyError.show();
                    }
                });

        // Add the request to the RequestQueue.
        queue.add(jsObjRequest);
    }
}
