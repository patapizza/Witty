package ai.wit.eval.witty;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.AlarmClock;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.Date;
import java.util.ArrayList;

import ai.wit.sdk.IWitListener;
import ai.wit.sdk.Wit;


public class MainActivity extends ActionBarActivity implements IWitListener {

    Wit _wit;

    private final String ACCESS_TOKEN = "YOUR_TOKEN";
    private final String HOME = "Your home address";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        _wit = new Wit(ACCESS_TOKEN, this);
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

    public void toggle(View v) {
        try {
            _wit.toggleListening();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void witDidGraspIntent(String intent, HashMap<String, JsonElement> entities, String body, double confidence, Error error) {
        ((TextView) findViewById(R.id.txtText)).setText(body);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonOutput = gson.toJson(entities);
        ((TextView) findViewById(R.id.jsonView)).setText(Html.fromHtml("<span><b>Intent: " + intent +
                "<b></span><br/>") + jsonOutput +
                Html.fromHtml("<br/><span><b>Confidence: " + confidence + "<b></span>"));
        if (intent != null) {
            if (intent.equals("navigate")) {
                String location = "";
                JsonElement jsonLocation = entities.get("location");
                if (jsonLocation.isJsonObject()) {
                    location = jsonLocation.getAsJsonObject().get("value").getAsString();
                    if (location.equals("home")) {
                        location = HOME;
                    }
                } else if (jsonLocation.isJsonArray()) {
                    for (JsonElement loc : entities.get("location").getAsJsonArray()) {
                        location += loc.getAsJsonObject().get("value").getAsString() + ", ";
                    }
                }
                Intent gmaps = new Intent(Intent.ACTION_VIEW, Uri.parse("http://maps.google.com/maps?daddr=" + location));
                startActivity(gmaps);
            } else if (intent.equals("play_music")) {
                String artist = "";
                String song = "";
                if (entities.containsKey("artist")) {
                    artist = entities.get("artist").getAsJsonObject().get("value").getAsString();
                }
                if (entities.containsKey("song")) {
                    song = entities.get("song").getAsJsonObject().get("value").getAsString();
                }
                Intent spotify = new Intent(Intent.ACTION_VIEW, Uri.parse("http://open.spotify.com/search/" + artist + "+" + song));
                startActivity(spotify);
            } else if (intent.equals("translate")) {
                String text = "";
                String lang = "fr";
                if (entities.containsKey("phrase_to_translate")) {
                    text = entities.get("phrase_to_translate").getAsJsonObject().get("value").getAsString();
                }
                if (entities.containsKey("language")) {
                    lang = entities.get("language").getAsJsonObject().get("value").getAsString();
                }
                Intent translate = new Intent(Intent.ACTION_VIEW, Uri.parse("https://translate.google.com/#en/" + lang + "/" + text));
                startActivity(translate);
            } else if (intent.equals("set_alarm")) {
                if (entities.containsKey("datetime")) {
                    String start = entities.get("datetime").getAsJsonObject().get("value").getAsJsonObject().get("from").getAsString();
                    DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
                    Date result;
                    try {
                        result = df1.parse(start);
                        Intent setAlarm = new Intent(AlarmClock.ACTION_SET_ALARM);
                        ArrayList<Integer> days = new ArrayList<Integer>();
                        days.add(result.getDay() + 1);
                        setAlarm.putExtra(AlarmClock.EXTRA_DAYS, days);
                        setAlarm.putExtra(AlarmClock.EXTRA_HOUR, result.getHours());
                        setAlarm.putExtra(AlarmClock.EXTRA_MINUTES, result.getMinutes());
                        startActivity(setAlarm);
                    } catch (ParseException e) {
                        // do nothing
                    }
                }
            } else if (intent.equals("find_place")) {
                if (entities.containsKey("place")) {
                    String loc = "";
                    String place = entities.get("place").getAsJsonObject().get("value").getAsString();
                    if (entities.containsKey("location")) {
                        loc = "&find_loc=" + entities.get("location").getAsJsonObject().get("value").getAsString();
                    }
                    Intent yelp = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.yelp.com/search?find_desc=" + place + loc));
                    startActivity(yelp);
                }
            } else if (intent.equals("browse")) {
                if (entities.containsKey("website")) {
                    String url = entities.get("website").getAsJsonObject().get("value").getAsString();
                    Intent chrome = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(chrome);
                }
            } else if (intent.equals("take_picture")) {
                Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivity(camera);
            }
        }
    }

    @Override
    public void witDidStartListening() {
        ((TextView) findViewById(R.id.txtText)).setText("Listening...");
    }

    @Override
    public void witDidStopListening() {
        ((TextView) findViewById(R.id.txtText)).setText("Processing...");
    }

    public static class PlaceholderFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            // Inflate the layout for this fragment
            return inflater.inflate(R.layout.wit_button, container, false);
        }
    }
}
