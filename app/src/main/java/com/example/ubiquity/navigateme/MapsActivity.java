package com.example.ubiquity.navigateme;

import android.Manifest;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ubiquity.navigateme.Remote.IGoogleApi;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.google.android.gms.maps.model.JointType.ROUND;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener
{

    private static final int MY_PERMISSION_CODE=1000;
    private GoogleApiClient mGoogleApiClient;
    private GoogleMap mMap;
    ArrayList<LatLng> listPoints;
    private double latitude, longitude;
    private Location mLocation;
    private Marker mMarker;
    private LocationRequest mLocationRequest;

    IGoogleApi mService;


    @Override
    protected void onCreate(Bundle savedInatanceState) {
        super.onCreate(savedInatanceState);
        setContentView(R.layout.activity_maps);


        SupportMapFragment mapFragment= (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        listPoints= new ArrayList<>();

        mService= Common.getIGoogleApi();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            checkLocationPermission();
        }

    }

    private boolean checkLocationPermission() {
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(this, new String[]{

                        Manifest.permission.ACCESS_FINE_LOCATION
                }, MY_PERMISSION_CODE);
            }
            else{
                ActivityCompat.requestPermissions(this, new String[]{

                        Manifest.permission.ACCESS_FINE_LOCATION
                }, MY_PERMISSION_CODE);
            }
            return false;
        }
        else{
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_CODE: {

                if(grantResults.length >0 && grantResults[0]== PackageManager.PERMISSION_GRANTED) {

                    if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED){

                        if(mGoogleApiClient== null) {
                            buildGoogleAPIClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }
                }
                else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        //Init Google Play Services
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                buildGoogleAPIClient();
                mMap.setMyLocationEnabled(true);
            }

        }
        else {
            buildGoogleAPIClient();
            mMap.setMyLocationEnabled(true);

        }
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                //Reset marker when already 2
                if(listPoints.size() == 2) {
                    listPoints.clear();
                    mMap.clear();
                }
                //save first point select
                listPoints.add(latLng);
                //Create marker
                MarkerOptions markerOptions=new MarkerOptions()
                        .position(latLng);

                if(listPoints.size()==1) {
                    //Add first marker to the app
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN));
                }
                else{
                    //Add second marker to the app
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                }
                mMap.addMarker(markerOptions);

                if(listPoints.size()==2) {
                    //Create the URL to get request from first marker to second
                    String url= getRequestUrl(listPoints.get(0),listPoints.get(1));
                    TaskRequestDirections taskRequestDirections=new TaskRequestDirections();
                    taskRequestDirections.execute(url);
                }
            }
        });
    }

    private String getRequestUrl(LatLng origin, LatLng dest) {
        //Value of origin
        String str_org= "origin=" + origin.latitude + "," + origin.longitude;
        //value of dest
        String str_dest="destination=" + dest.latitude + ","+ origin.longitude;
        //Set value enable the sensor
        String sensor= "sensor=false";
        //Mode for find direction
        String mode="mode=driving";
        //Build the full param
        String param= str_org+"&"+str_dest+"&"+sensor+"&"+mode;
        //Output format
        String output="json";
        //Create url to request
        String url="https://maps.googleapis.com/maps/api/directions/"+output+"?"+param;
        return url;
    }

    private String requestDirection(String reqUrl) throws IOException {
        String responseString="";
        InputStream inputStream=null;
        HttpURLConnection httpURLConnection=null;
        try{
            URL url= new URL(reqUrl);
            httpURLConnection= (HttpURLConnection) url.openConnection();
            httpURLConnection.connect();

            //Get the response result
            inputStream= httpURLConnection.getInputStream();
            InputStreamReader inputStreamReader= new InputStreamReader(inputStream);
            BufferedReader bufferedReader= new BufferedReader(inputStreamReader);

            StringBuffer stringBuffer=new StringBuffer();
            String line="";
            while((line= bufferedReader.readLine()) != null) {
                stringBuffer.append(line);
            }

            responseString =stringBuffer.toString();
            bufferedReader.close();
            inputStream.close();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(inputStream !=null) {
                inputStream.close();
            }
            httpURLConnection.disconnect();
        }
        return responseString;
    }

    private synchronized void buildGoogleAPIClient() {
        mGoogleApiClient=new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }



    @Override
    public void onLocationChanged(Location location) {
        mLocation= location;
        if(mMarker !=null) {
            mMarker.remove();
        }

        latitude=location.getLatitude();
        longitude=location.getLongitude();

        LatLng latlng= new LatLng(latitude,longitude);

        MarkerOptions markerOptions=new MarkerOptions()
                .position(latlng)
                .title("Your Position")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        mMarker= mMap.addMarker(markerOptions);

        mMap.moveCamera(CameraUpdateFactory.newLatLng(latlng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

        if(mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, (com.google.android.gms.location.LocationListener) this);
        }


    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,mLocationRequest, (com.google.android.gms.location.LocationListener) this);
        }

        }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    public class TaskRequestDirections extends AsyncTask<String,Void,String> {

        @Override
        protected String doInBackground(String... strings) {
            String responseString="";
            try{
                responseString=requestDirection(strings[0]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return responseString;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            //Parse json
            TaskParser taskParser= new TaskParser();
            taskParser.execute(s);
        }
    }

    public class TaskParser extends AsyncTask<String, Void, List<List<HashMap<String,String>>>> {

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... strings) {
            JSONObject jsonObject=null;
            List<List<HashMap<String,String>>> routes=null;
            try {
                jsonObject =new JSONObject(strings[0]);
                DirectionParser directionParser= new DirectionParser();
                routes= directionParser.parse(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return routes;
        }
        @Override
        protected void onPostExecute (List<List<HashMap<String, String>>> lists) {
            //Get list route and display it into the map

            ArrayList points= null;

            PolylineOptions polylineOptions=null;

            for(List<HashMap<String, String>> path: lists) {
                points = new ArrayList<>();
                polylineOptions= new PolylineOptions();


                for(HashMap<String, String> point: path) {
                    double lat = Double.parseDouble(point.get("lat"));
                    double lon = Double.parseDouble(point.get("lon"));

                    points.add(new LatLng(lat,lon));
                }

                polylineOptions= polylineOptions.addAll(points);
                polylineOptions= polylineOptions.width(15);
                polylineOptions=polylineOptions.color(Color.BLUE);
                polylineOptions=polylineOptions.geodesic(true);
            }

            if(polylineOptions != null) {
                mMap.addPolyline(polylineOptions);
            }
            else{
                Toast.makeText(MapsActivity.this, "Direction not found!!", Toast.LENGTH_SHORT).show();
            }


            super.onPostExecute(lists);
        }
    }

}


