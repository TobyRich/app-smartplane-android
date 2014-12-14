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
    public int weather_code;
}

public class MeteoTask extends AsyncTask<Void, Void, MeteoData> {
    @SuppressWarnings("FieldCanBeLocal")
    private final static String TAG = "MeteoTask";
    private final Activity activity;
    final String BASE_FETCH_URL = "http://api.openweathermap.org/data/2.5/find?";

    private final Hashtable<Integer, String> iconMapping;

    public MeteoTask(Activity activity) {
        this.activity = activity;
        iconMapping = new Hashtable<Integer, String>();

        iconMapping.put(200, "F");
        iconMapping.put(201, "F");
        iconMapping.put(202, "F");
        iconMapping.put(210, "F");
        iconMapping.put(211, "F");
        iconMapping.put(212, "F");
        iconMapping.put(221, "F");
        iconMapping.put(230, "F");
        iconMapping.put(231, "F");
        iconMapping.put(232, "F");
        iconMapping.put(300, "-");
        iconMapping.put(301, "-");
        iconMapping.put(302, "-");
        iconMapping.put(310, "-");
        iconMapping.put(311, "-");
        iconMapping.put(312, "-");
        iconMapping.put(313, "-");
        iconMapping.put(314, "-");
        iconMapping.put(321, "-");
        iconMapping.put(500, "$");
        iconMapping.put(501, "$");
        iconMapping.put(502, "*");
        iconMapping.put(503, "*");
        iconMapping.put(504, "*");
        iconMapping.put(511, "*");
        iconMapping.put(520, "$");
        iconMapping.put(521, "*");
        iconMapping.put(522, "*");
        iconMapping.put(531, "*");
        iconMapping.put(600, "9");
        iconMapping.put(601, "9");
        iconMapping.put(602, "9");
        iconMapping.put(611, "9");
        iconMapping.put(612, "9");
        iconMapping.put(615, "9");
        iconMapping.put(616, "9");
        iconMapping.put(620, "9");
        iconMapping.put(621, "9");
        iconMapping.put(622, "9");
        iconMapping.put(701, "?");
        iconMapping.put(711, "?");
        iconMapping.put(721, "?");
        iconMapping.put(731, "?");
        iconMapping.put(741, "<");
        iconMapping.put(751, "<");
        iconMapping.put(761, "<");
        iconMapping.put(762, "<");
        iconMapping.put(771, "<");
        iconMapping.put(781, "X");
        iconMapping.put(800, "I");
        iconMapping.put(801, "!");
        iconMapping.put(802, "!");
        iconMapping.put(803, "!");
        iconMapping.put(804, "!");
        iconMapping.put(900, "X");
        iconMapping.put(901, "F");
        iconMapping.put(902, "X");
        iconMapping.put(903, "9");
        iconMapping.put(904, "I");
        iconMapping.put(905, "B");
        iconMapping.put(906, "3");
        iconMapping.put(950, "!");
        iconMapping.put(951, "!");
        iconMapping.put(952, "B");
        iconMapping.put(953, "B");
        iconMapping.put(954, "B");
        iconMapping.put(955, "B");
        iconMapping.put(956, "C");
        iconMapping.put(957, "C");
        iconMapping.put(958, "C");
        iconMapping.put(959, "C");
        iconMapping.put(960, "F");
        iconMapping.put(961, "F");
        iconMapping.put(962, "X");

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
        } catch (JSONException ex) {
            Log.d(TAG, "Exception while getting data from JSON");
            return null;
        }
        return meteoData;
    }

    @Override
    public void onPostExecute(MeteoData result) {
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
            humidity_data.setText(result.humidity + "%");
            pressure_data.setText(result.pressure + " hPa");
            temperature_data.setText(result.temperature + "K");
            windSpeed_data.setText(result.wind_speed + "");
            windDirection_data.setText(result.wind_deg + "°");
        } else {
            final String UNAVAILABLE = "N/A";
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

        int code = result.weather_code;
        String weatherIcon_id = iconMapping.get(code);
        if (weatherIcon_id == null) {
            weatherIcon_id = "I";  // sunny
        }

        // default is sunny
        weatherIcon.setText(weatherIcon_id);
    }
}
