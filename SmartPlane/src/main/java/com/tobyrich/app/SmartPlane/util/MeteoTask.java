package com.tobyrich.app.SmartPlane.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Radu Hambasan
 * @date 16 Jul 2014
 */
public class MeteoTask extends AsyncTask<Void, Void, MeteoData> {
    private final String TAG = "MeteoTask";
    private final Activity activity;
    final String BASE_FETCH_URL = "http://api.openweathermap.org/data/2.5/find?";

    public MeteoTask(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void onPreExecute() {
    }

    @Override
    protected MeteoData doInBackground(Void... params) {
        LocationManager locManager =
                (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        if (locManager == null) {
            Log.e(TAG, "Could not get location manager (fatal)");
            return null;
        }

        Location myLocation;
        try {
            myLocation = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (myLocation == null) {
                Log.e(TAG, "Could not get location from GPS. Falling back on network.");
                myLocation = locManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            // if we were unable to get the location in any way:
            if (myLocation == null) {
                Log.e(TAG, "Could not get location from network (fatal).");
                return null;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

        String longitude = "" + myLocation.getLongitude();
        String latitude = "" + myLocation.getLatitude();

        // no signal
        if (longitude.compareTo(latitude) == 0) {
            Log.e(TAG, "No signal (fatal)");
            return null;
        }
        final String fetchLocation =
                BASE_FETCH_URL + "lat=" + latitude + "&lon=" + longitude + "&cnt=1";

        String meteoResponse = Util.readUrl(fetchLocation);
        if (meteoResponse == null) {
            Log.e(TAG, "No internet connection");
            return null;
        }

        // if we there was a problem, there is nothing to do
        if (meteoResponse.isEmpty()) {
            return null;
        }

        // Parse json
        JSONObject meteoJSON;
        try {
            meteoJSON = new JSONObject(meteoResponse);
        } catch (JSONException e) {
            Log.e(TAG, "Invalid meteo JSON");
            return null;
        }

        // Setting meteo data
        MeteoData meteoData = new MeteoData();
        try {
            JSONArray results_arr = meteoJSON.getJSONArray("list");
            JSONObject first_result = results_arr.getJSONObject(0);

            JSONObject main_obj = first_result.getJSONObject("main");
            JSONObject wind_obj = first_result.getJSONObject("wind");
            JSONArray weather_arr = first_result.getJSONArray("weather");

            meteoData.temperature = main_obj.getDouble("temp");
            meteoData.humidity = main_obj.getInt("humidity");
            meteoData.pressure = main_obj.getInt("pressure");

            meteoData.wind_speed = wind_obj.getDouble("speed");
            meteoData.wind_deg = wind_obj.getInt("deg");

            meteoData.weather_descr = weather_arr.getJSONObject(0).getString("description");
        } catch (JSONException ex) {
            Log.d(TAG, "Exception while getting data from JSON");
        }
        return meteoData;
    }

    @Override
    public void onPostExecute(MeteoData result) {
        if (result == null) {
            return;
        }
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // dismiss
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        String message =
                "Humidity: " + result.humidity + "\n" +
                        "Pressure: " + result.pressure + "\n" +
                        "Temperature: " + result.temperature + "\n" +
                        "Wind speed: " + result.wind_speed + " km/h " + "\n" +
                        "Wind orientation: " + result.wind_deg + " degrees" + "\n" +
                        "Weather forecast: " + result.weather_descr + "\n";
        builder.setMessage(message).setNeutralButton("OK", dialogClickListener).show();
    }
}
