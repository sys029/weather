package com.example.weatherforecast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.os.StrictMode;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final  int REQUEST_CODE_LOCATION_PERMISSION=1;
    private ProgressBar progressBar;
    private ResultReceiver resultReceiver;
    private TextView view_city;
    private TextView view_temp,view_temp1,view_temp2,view_temp3;
    private TextView view_desc,view_desc1,view_desc2,view_desc3;

    ImageView view_weather,view_weather1,view_weather2,view_weather3;
    EditText search;
    FloatingActionButton search_floating;
    FloatingActionButton location_floating;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        resultReceiver=new AddressResultReceiver(new Handler());
        progressBar=findViewById(R.id.progressBar);


        view_city=findViewById(R.id.town);
        view_city.setText("Hola!");
        view_temp=findViewById(R.id.temp);
        view_temp.setText("");
        view_temp1=findViewById(R.id.temp1);
        view_temp1.setText("");
        view_temp2=findViewById(R.id.temp2);
        view_temp2.setText("");
        view_temp3=findViewById(R.id.temp3);
        view_temp3.setText("");
        view_desc=findViewById(R.id.desc);
        view_desc.setText("");
        view_desc1=findViewById(R.id.desc1);
        view_desc1.setText("");
        view_desc2=findViewById(R.id.desc2);
        view_desc2.setText("");
        view_desc3=findViewById(R.id.desc3);
        view_desc3.setText("");

        view_weather=findViewById(R.id.wheather_image);
        view_weather1=findViewById(R.id.wheather_image1);
        view_weather2=findViewById(R.id.wheather_image2);
        view_weather3=findViewById(R.id.wheather_image3);
        search=findViewById(R.id.search_edit);
        search_floating=findViewById(R.id.floating_search);
        location_floating=findViewById(R.id.floating_location);

        location_floating.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(ContextCompat.checkSelfPermission(
                        getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION
                )!= PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(
                            MainActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            REQUEST_CODE_LOCATION_PERMISSION
                    );
                }else {
                    getCurrentLocation();
                }

            }
        });

        search_floating.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(TextUtils.isEmpty(search.getText().toString())){

                    Toast.makeText(getApplicationContext(),"Enter a city",Toast.LENGTH_SHORT).show();
                }
                else {

                    InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    api_key(String.valueOf(search.getText()));
                    api_key1(String.valueOf(search.getText()));
                    api_key2(String.valueOf(search.getText()));
                    api_key3(String.valueOf(search.getText()));

                }
            }
        });


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode==REQUEST_CODE_LOCATION_PERMISSION&&grantResults.length>0){
            getCurrentLocation();
        }else {
            Toast.makeText(this,"Permission Denied",Toast.LENGTH_SHORT).show();
        }
    }

    private void getCurrentLocation() {

        progressBar.setVisibility(View.VISIBLE);

        final LocationRequest locationRequest= new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationServices.getFusedLocationProviderClient(MainActivity.this)
                .requestLocationUpdates(locationRequest,new LocationCallback(){

                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        super.onLocationResult(locationResult);
                        LocationServices.getFusedLocationProviderClient(MainActivity.this)
                                .removeLocationUpdates(this);
                        if (locationResult != null && locationResult.getLocations().size()>0){
                            int latestLocationIndex= locationResult.getLocations().size()- 1;
                            double latitude=locationResult.getLocations().get(latestLocationIndex).getLatitude();
                            double longitude=locationResult.getLocations().get(latestLocationIndex).getLongitude();
                            Location location=new Location("providerNA");
                            location.setLatitude(latitude);
                            location.setLongitude(longitude);
                            fetchAddressFromLatLong(location);
                        }else {

                            progressBar.setVisibility(View.GONE);
                        }

                    }
                }, Looper.getMainLooper());

    }

    private void fetchAddressFromLatLong(Location location){
        Intent intent=new Intent(this,FetchAddressIntentService.class);
        intent.putExtra(Constants.RECEIVER,resultReceiver);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA,location);
        startService(intent);
    }

    private class AddressResultReceiver extends ResultReceiver{

        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);
            if(resultCode==Constants.SUCCESS_RESULT){
                setText(search,resultData.getString(Constants.RESULT_DATA_KEY));
            }else {
                Toast.makeText(MainActivity.this,resultData.getString(Constants.RESULT_DATA_KEY),Toast.LENGTH_SHORT).show();
            }
            progressBar.setVisibility(View.GONE);
        }
    }

    private void api_key(final String City) {

        OkHttpClient client=new OkHttpClient();

        Request request=new Request.Builder()
                .url("https://api.openweathermap.org/data/2.5/weather?q="+City+"&appid=a6f41d947e0542a26580bcd5c3fb90ef&units=metric")
                .get()
                .build();
        final StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        try {
            Response response= client.newCall(request).execute();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {

                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    String responseData= response.body().string();
                    try {
                        JSONObject json=new JSONObject(responseData);
                        JSONArray array=json.getJSONArray("weather");
                        JSONObject object=array.getJSONObject(0);

                        String description=object.getString("description");
                        String icons = object.getString("icon");

                        JSONObject temp1= json.getJSONObject("main");
                        Double Temperature=temp1.getDouble("temp");

                        setText(view_city,City);

                        String temps=Math.round(Temperature)+" °C";
                        setText(view_temp,temps);
                        setText(view_desc,description.toUpperCase());
                        setImage(view_weather,icons);


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }catch (IOException e){
            e.printStackTrace();
        }


    }

    private void api_key1(final String City) {
        OkHttpClient client=new OkHttpClient();

        Request request=new Request.Builder()
                .url("https://api.openweathermap.org/data/2.5/forecast?q="+City+"&appid=a6f41d947e0542a26580bcd5c3fb90ef&units=metric")
                .get()
                .build();
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        try {
            Response response= client.newCall(request).execute();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {

                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    String responseData= response.body().string();
                    try {
                        JSONObject json=new JSONObject(responseData);
                        JSONArray array=json.getJSONArray("list");
                        JSONObject object=array.getJSONObject(0);

                        JSONArray array1=object.getJSONArray("weather");
                        JSONObject obj=array1.getJSONObject(0);

                        String description=obj.getString("description");
                        String icons = obj.getString("icon");

                        JSONObject temp1= object.getJSONObject("main");
                        Double Temperature=temp1.getDouble("temp");

                        String temps=Math.round(Temperature)+" °C";
                        setText(view_temp1,temps);
                        setText(view_desc1,description.toUpperCase());
                        setImage(view_weather1,icons);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }catch (IOException e){
            e.printStackTrace();
        }


    }

    private void api_key2(final String City) {
        OkHttpClient client=new OkHttpClient();

        Request request=new Request.Builder()
                .url("https://api.openweathermap.org/data/2.5/forecast?q="+City+"&appid=a6f41d947e0542a26580bcd5c3fb90ef&units=metric")
                .get()
                .build();
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        try {
            Response response= client.newCall(request).execute();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {

                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    String responseData= response.body().string();
                    try {
                        JSONObject json=new JSONObject(responseData);
                        JSONArray array=json.getJSONArray("list");
                        JSONObject object=array.getJSONObject(1);

                        JSONArray array1=object.getJSONArray("weather");
                        JSONObject obj=array1.getJSONObject(0);

                        String description=obj.getString("description");
                        String icons = obj.getString("icon");

                        JSONObject temp1= object.getJSONObject("main");
                        Double Temperature=temp1.getDouble("temp");

                        String temps=Math.round(Temperature)+" °C";
                        setText(view_temp2,temps);
                        setText(view_desc2,description.toUpperCase());
                        setImage(view_weather2,icons);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }catch (IOException e){
            e.printStackTrace();
        }


    }

    private void api_key3(final String City) {
        OkHttpClient client=new OkHttpClient();

        Request request=new Request.Builder()
                .url("https://api.openweathermap.org/data/2.5/forecast?q="+City+"&appid=a6f41d947e0542a26580bcd5c3fb90ef&units=metric")
                .get()
                .build();
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        try {
            Response response= client.newCall(request).execute();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {

                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    String responseData= response.body().string();
                    try {
                        JSONObject json=new JSONObject(responseData);
                        JSONArray array=json.getJSONArray("list");
                        JSONObject object=array.getJSONObject(2);

                        JSONArray array1=object.getJSONArray("weather");
                        JSONObject obj=array1.getJSONObject(0);

                        String description=obj.getString("description");
                        String icons = obj.getString("icon");

                        JSONObject temp1= object.getJSONObject("main");
                        Double Temperature=temp1.getDouble("temp");

                        String temps=Math.round(Temperature)+" °C";
                        setText(view_temp3,temps);
                        setText(view_desc3,description.toUpperCase());
                        setImage(view_weather3,icons);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }catch (IOException e){
            e.printStackTrace();
        }


    }



    private void setText(final TextView text, final String value){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                text.setText(value);
            }
        });
    }
    private void setImage(final ImageView imageView, final String value){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                switch (value){
                    case "01d": imageView.setImageDrawable(getResources().getDrawable(R.drawable.d01d));
                        break;
                    case "01n": imageView.setImageDrawable(getResources().getDrawable(R.drawable.d01d));
                        break;
                    case "02d": imageView.setImageDrawable(getResources().getDrawable(R.drawable.d02d));
                        break;
                    case "02n": imageView.setImageDrawable(getResources().getDrawable(R.drawable.d02d));
                        break;
                    case "03d": imageView.setImageDrawable(getResources().getDrawable(R.drawable.d03d));
                        break;
                    case "03n": imageView.setImageDrawable(getResources().getDrawable(R.drawable.d03d));
                        break;
                    case "04d": imageView.setImageDrawable(getResources().getDrawable(R.drawable.d04d));
                        break;
                    case "04n": imageView.setImageDrawable(getResources().getDrawable(R.drawable.d04d));
                        break;
                    case "09d": imageView.setImageDrawable(getResources().getDrawable(R.drawable.d09d));
                        break;
                    case "09n": imageView.setImageDrawable(getResources().getDrawable(R.drawable.d09d));
                        break;
                    case "10d": imageView.setImageDrawable(getResources().getDrawable(R.drawable.d10d));
                        break;
                    case "10n": imageView.setImageDrawable(getResources().getDrawable(R.drawable.d10d));
                        break;
                    case "11d": imageView.setImageDrawable(getResources().getDrawable(R.drawable.d11d));
                        break;
                    case "11n": imageView.setImageDrawable(getResources().getDrawable(R.drawable.d11d));
                        break;
                    case "13d": imageView.setImageDrawable(getResources().getDrawable(R.drawable.d13d));
                        break;
                    case "13n": imageView.setImageDrawable(getResources().getDrawable(R.drawable.d13d));
                        break;
                    default:
                        imageView.setImageDrawable(getResources().getDrawable(R.drawable.wheather));

                }
            }
        });
    }
}
