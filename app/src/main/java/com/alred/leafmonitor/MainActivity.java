package com.alred.leafmonitor;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

import org.joda.time.DateTime;
import org.joda.time.Hours;
import org.joda.time.Minutes;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.text.DecimalFormat;
import java.util.List;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;


public class MainActivity extends Activity {

    private static final String TAG = "LeafMain";
    private static final int CHARGE_DATA_THRESHOLD = 50;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                        .setDefaultFontPath("fonts/SourceSansPro-ExtraLight.ttf")
                        .setFontAttrId(R.attr.fontPath)
                        .build()
        );

        setContentView(R.layout.activity_main);
        startService(new Intent(this, LeafMonitorService.class));

        Parse.initialize(this, getResources().getText(R.string.parse_app_id).toString(),
                getResources().getText(R.string.parse_app_key).toString());

    }

    @Override
    protected void onResume() {
        super.onResume();
        getChargingData();
    }

    /**
     * Compute for the different status values.
     */
    protected void getChargingData() {

        //DateTimeFormatter isoFormat = DateTimeFormat.forPattern("YYYY-MM-dd'T'HH:mm:ssZZ");
        //query.whereLessThan("timestamp", isoFormat.print(isoFormat.parseDateTime("2015-02-19T00:00:00-05:00")));

        // get the latest data from parse.com, only concerned
        // about the charging state.

        ParseQuery<ParseObject> query = ParseQuery.getQuery("LeafData");
        query.whereGreaterThan("data", CHARGE_DATA_THRESHOLD);
        query.orderByDescending("createdAt");
        query.setLimit(1000);

        query.findInBackground(new FindCallback<ParseObject>() {

            public void done(List<ParseObject> dataList, ParseException e) {
                if (e == null) {
                    Log.d(TAG, "Retrieved " + dataList.size() + " data points");
                    processData(dataList);
                } else {
                    Log.d(TAG, "Error: " + e.getMessage());
                }
            }

        });


    }

    protected void processData(List<ParseObject> dataList) {

        DateTimeFormatter isoFormat = DateTimeFormat.forPattern("YYYY-MM-dd'T'HH:mm:ssZZ");

        DateTime startTimestamp = null;
        DateTime endTimestamp = null;
        double totalKwhUsed = 0;

        int counter = 0;
        DateTime previousTimestamp = null;

        for (ParseObject data : dataList) {

            // get the current timestamp.
            DateTime timestamp = isoFormat.parseDateTime(data.get("timestamp").toString());

            if (counter == 0) {
                // the first record should be the charge end time.
                endTimestamp = timestamp;
                Log.d(TAG, "Charge end timestamp: " + isoFormat.print(endTimestamp));
                totalKwhUsed = getKwhFromData(data.get("data").toString()) / 60d;
            }
            else {

                // get the gap between the last reading and the current.
                // if the gap is more than 30 minutes, then we can assume the previous timestamp was
                // the 'start charge event'.
                int gap = Minutes.minutesBetween(timestamp, previousTimestamp).getMinutes();
                if (gap > 30) {
                    startTimestamp = previousTimestamp;
                    Log.d(TAG, "Charge start timestamp: " + isoFormat.print(startTimestamp));
                    break;
                } else {
                    // add to totalKwhUsed, since I'm inside the charging session window.
                    totalKwhUsed = totalKwhUsed + (getKwhFromData(data.get("data").toString()) / 60d);
                }
            }

            previousTimestamp = timestamp;
            counter ++;

        }

        Log.d(TAG, "Relevant record count: " + counter);
        Log.d(TAG, "Last relevant timestamp: " + isoFormat.print(previousTimestamp));

        if (startTimestamp == null) {
            startTimestamp = previousTimestamp;
        }

        // i should have both start/end timestamps, compute for the hours.  I want decimal places
        // so I should use minutes and divide by 60.
        double hoursCharged = Minutes.minutesBetween(startTimestamp, endTimestamp).getMinutes() / 60d;

        // get the hours from last charge end time.
        DateTime now = new DateTime();
        double hoursAgo = Minutes.minutesBetween(endTimestamp, now).getMinutes() / 60d;


        // update the UI.
        ((TextView) findViewById(R.id.chargeHours)).setText(new DecimalFormat("#.##").format(hoursCharged));
        ((TextView) findViewById(R.id.lastCharged)).setText(new DecimalFormat("#.##").format(hoursAgo));
        ((TextView) findViewById(R.id.usedKwh)).setText(new DecimalFormat("#.##").format(totalKwhUsed));

        double percentCharge = hoursCharged / 3.63636363 * 100;
        ((TextView) findViewById(R.id.addlCharge)).setText(new DecimalFormat("##").format(percentCharge));


    }

    protected double getKwhFromData(String raw) {

        int rawInt = Integer.parseInt(raw);

        double kwh = rawInt * 0.003222656 / 147 * 2000 * 220 / 1000;
        return kwh;

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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }
}
