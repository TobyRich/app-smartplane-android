package com.tobyrich.app.SmartPlane.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.tobyrich.app.SmartPlane.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Hashtable;

/**
 * @author Radu Hambasan
 * @date 16 Jul 2014
 */

class MeteoData {
    public int temperature;
    public int pressure;
    public int humidity;
    public int wind_speed;
    public int wind_deg;
    public String weather_descr;
    public String location;
    public int weather_code;
}

public class MeteoTask extends AsyncTask<Void, Void, MeteoData> {
    @SuppressWarnings("FieldCanBeLocal")
    private final static String TAG = "MeteoTask";
    private final static String CLIMA_FONT_NAME = "climacons.ttf";
    private final Activity activity;
    final String BASE_FETCH_URL = "http://api.openweathermap.org/data/2.5/find?";

    private final Hashtable<String, String> iconMapping;

    public MeteoTask(Activity activity) {
        this.activity = activity;
        iconMapping = new Hashtable<String, String>();

        iconMapping.put("rainy", "f");
        iconMapping.put("sunny", "I");
        iconMapping.put("windy", "B");
        iconMapping.put("stormy", "F");
        iconMapping.put("cloudy", "!");
    }

    @Override
    public void onPreExecute() {
        activity.findViewById(R.id.weatherProgressBar).setVisibility(View.VISIBLE);

        activity.findViewById(R.id.horizon_wind_txt).setVisibility(View.GONE);
        activity.findViewById(R.id.horizon_temp_txt).setVisibility(View.GONE);
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

            meteoData.temperature = (int) Math.round(main_obj.getDouble("temp"));
            meteoData.humidity = main_obj.getInt("humidity");
            meteoData.pressure = main_obj.getInt("pressure");

            meteoData.wind_speed = (int) Math.round(wind_obj.getDouble("speed"));
            meteoData.wind_deg = wind_obj.getInt("deg");
            meteoData.weather_descr = weather_arr.getJSONObject(0).getString("description");
            meteoData.weather_code = weather_arr.getJSONObject(0).getInt("id");
            meteoData.location = first_result.getString("name");
        } catch (JSONException ex) {
            Log.d(TAG, "Exception while getting data from JSON");
            return null;
        }
        return meteoData;
    }

    @Override
    public void onPostExecute(MeteoData result) {
        TextView weather_location =
                (TextView) activity.findViewById(R.id.weather_location);
        TextView humidity_data =
                (TextView) activity.findViewById(R.id.weather_humidity_data);
        TextView pressure_data =
                (TextView) activity.findViewById(R.id.weather_pressure_data);
        TextView temperature_data =
                (TextView) activity.findViewById(R.id.weather_temperature_data);
        TextView windSpeed_data =
                (TextView) activity.findViewById(R.id.weather_windspeed_data);
        TextView windDirection_data =
                (TextView) activity.findViewById(R.id.weather_direction_data);


        if (result != null) {
            final double KELVIN_TO_CELSIUS = 273;
            long celsius_temp = Math.round(result.temperature - KELVIN_TO_CELSIUS);

            String wind_txt = result.wind_speed + "kmph";
            String temp_txt = celsius_temp + "℃";

            /* On main screen */
            TextView wind_txt_vw = (TextView) activity.findViewById(R.id.horizon_wind_txt);
            wind_txt_vw.setText(wind_txt);
            wind_txt_vw.setVisibility(View.VISIBLE);

            TextView temp_txt_vw = (TextView) activity.findViewById(R.id.horizon_temp_txt);
            temp_txt_vw.setText(temp_txt);
            temp_txt_vw.setVisibility(View.VISIBLE);

            /* On weather screen */
            weather_location.setText(result.location);
            humidity_data.setText(result.humidity + "%");
            pressure_data.setText(result.pressure + " hPa");
            temperature_data.setText(result.temperature + "K");
            windSpeed_data.setText(result.wind_speed + "");
            windDirection_data.setText(result.wind_deg + "°");
        } else {
            final String UNAVAILABLE = "N/A";
            weather_location.setText(UNAVAILABLE);
            humidity_data.setText(UNAVAILABLE);
            pressure_data.setText(UNAVAILABLE);
            temperature_data.setText(UNAVAILABLE);
            windSpeed_data.setText(UNAVAILABLE);
            windDirection_data.setText(UNAVAILABLE);
        }

        activity.findViewById(R.id.weatherProgressBar).setVisibility(View.GONE);

        if (result == null) {
            return;  // nothing to do anymore
        }

        TextView weatherIcon = (TextView) activity.findViewById(R.id.weatherIcon);
        final Typeface CLIMA_FONT =
                Typeface.createFromAsset(activity.getAssets(), "fonts/climacons.ttf");
        weatherIcon.setTypeface(CLIMA_FONT);

        String weatherIcon_id = iconMapping.get("sunny");
        int code = result.weather_code;

        if ((code >= 300 && code <= 321) || (code >= 500 || code <= 531)) {
            weatherIcon_id = iconMapping.get("rainy");
            int blackColor = activity.getResources().getColor(R.color.black);
            weatherIcon.setTextColor(blackColor);
        } else if (code >= 900 && code <= 962) {
            weatherIcon_id = iconMapping.get("windy");
            int blackColor = activity.getResources().getColor(R.color.black);
            weatherIcon.setTextColor(blackColor);
        } else if (code >= 200 && code <= 232) {
            weatherIcon_id = iconMapping.get("stormy");
            int blackColor = activity.getResources().getColor(R.color.black);
            weatherIcon.setTextColor(blackColor);
        } else if (code >= 801 && code <= 804) {
            weatherIcon_id = iconMapping.get("cloudy");
            int whiteColor = activity.getResources().getColor(R.color.white);
            weatherIcon.setTextColor(whiteColor);
        }
        // default is sunny
        weatherIcon.setText(weatherIcon_id);
    }
}
