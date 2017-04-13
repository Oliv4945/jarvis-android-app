package net.iopush.jarvis;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.util.Log;
import java.net.URLEncoder;

import android.speech.tts.TextToSpeech;



public class MainActivity extends AppCompatActivity {

    private FloatingActionButton btnSpeak;
    private RecyclerView recyclerViewConversation;
    private List<ConversationObject> jarvisConversationList = new ArrayList<>();
    // TODO - Update alue
    private final int REQ_CODE_SPEECH_INPUT = 100;
    TextToSpeech ttsEngine;

    private String serverUrl;
    private String serverPort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get microphone button
        btnSpeak = (FloatingActionButton) findViewById(R.id.btnSpeak);

        // Setup toolbar
        //Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        //setSupportActionBar(myToolbar);

        // Setup recyclerView
        recyclerViewConversation = (RecyclerView) findViewById(R.id.recyclerViewConversation);
        recyclerViewConversation.setLayoutManager(new LinearLayoutManager(this));
        jarvisConversationList.add(new ConversationObject("", getString(R.string.tap_on_mic)));
        recyclerViewConversation.setAdapter(new ConversationAdapter(jarvisConversationList));

        btnSpeak.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                promptSpeechInput();
            }
        });

        // jarvisConversation.setMovementMethod(new ScrollingMovementMethod());

        // Init TTS
        ttsEngine = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    // TODO - https://developer.android.com/reference/android/speech/tts/TextToSpeech.html#isLanguageAvailable(java.util.Locale)
                    ttsEngine.setLanguage(Locale.getDefault());
                }
            }
        });

        // Get preferences
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        serverUrl = SP.getString("serverUrl", "NA");
        serverPort = SP.getString("serverPort", "NA");
        if (serverUrl == "NA") {
            Intent i = new Intent(this, MyPreferencesActivity.class);
            startActivity(i);
        }
    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first

        // TODO : Better to call pref change listener
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        serverUrl = SP.getString("serverUrl", "NA");
        serverPort = SP.getString("serverPort", "NA");
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
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    jarvisConversationList.add(0, new ConversationObject("You", result.get(0)));
                    recyclerViewConversation.getAdapter().notifyItemInserted(0);
                    recyclerViewConversation.smoothScrollToPosition(0);
                    Log.i("Jarvis", "STT: " + result.get(0));


                    // Instantiate the RequestQueue.
                    RequestQueue queue = Volley.newRequestQueue(this);
                    String requestUrl = serverUrl+":"+serverPort+"/?order=" + URLEncoder.encode(result.get(0));

                    // Request a string response from the provided URL.
                    StringRequest stringRequest = new StringRequest(Request.Method.GET, requestUrl,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    Log.i("Jarvis", "Server answer: " + response);
                                    // Parse answer
                                    try {
                                        JSONArray jObject = new JSONArray(response);
                                        Log.i("Jarvis", "jObject" + jObject.toString());
                                        for (int i=0; i<jObject.length(); i++) {
                                            JSONObject c = jObject.getJSONObject(i);
                                            Log.i("Jarvis", "Answer: " + c.toString());
                                            jarvisConversationList.add(0, new ConversationObject("Jarvis", c.getString("Jarvis")));
                                            recyclerViewConversation.getAdapter().notifyItemInserted(0);
                                            recyclerViewConversation.smoothScrollToPosition(0);
                                            if (android.os.Build.VERSION.SDK_INT >= 21) {
                                                ttsEngine.speak(c.getString("Jarvis"), TextToSpeech.QUEUE_ADD, null, c.getString("Jarvis"));
                                            } else {
                                                ttsEngine.speak(c.getString("Jarvis"), TextToSpeech.QUEUE_ADD, null);
                                            }
                                        }
                                    } catch (final JSONException e) {
                                        Log.e("Main", "Json parsing error: " + e.getMessage());
                                        Toast.makeText(getApplicationContext(),
                                                "Json parsing error: " + e.getMessage(),
                                                Toast.LENGTH_LONG)
                                                .show();

                                    }
                                }
                            }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e("VOLLEY", error.toString());
                            // TODO - Snackbar action button
                            Snackbar snackbarVolleyError = Snackbar
                                    .make(findViewById(R.id.mainActivity), R.string.volleyError, Snackbar.LENGTH_LONG);

                            snackbarVolleyError.show();
                            recyclerViewConversation.smoothScrollToPosition(0);
                        }
                    });
                    // Add the request to the RequestQueue.
                    queue.add(stringRequest);
                }
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
}
