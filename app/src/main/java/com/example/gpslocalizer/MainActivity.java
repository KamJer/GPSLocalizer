package com.example.gpslocalizer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    public static final int REQUEST_CODE_0 = 0;

    private Location currentLocation;
    private Location oldLocation;

    private Button btnStart;
    private Button btnStop;

    private TextView longitudeText;
    private TextView latitudeText;
    private TextView samplePointsText;

    private EditText pointName;

    private Thread update;
    private File file;
    private int samplePoints = 0;
    private LocationManager locaMen;
    private LocationListener locaLis = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            if (getGPSStatus()) {
                currentLocation = location;

                longitudeText.setText(String.valueOf(currentLocation.getLongitude()));
                latitudeText.setText(String.valueOf(currentLocation.getLatitude()));
            }
        }
    };



//  Setting runnable for saving update location
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            try {
                File fileFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "GPSLocalizer");
                if (!fileFolder.exists()) {
                    if (!fileFolder.mkdirs()) {
                        Log.d("test", "coś się zjebało");
                        System.exit(0);
                    }
                }
                file = new File(fileFolder.getPath() + File.separator + pointName.getText()  + ".csv");
                FileOutputStream fos = new FileOutputStream(file);
                String location = "Longitude,Latitude \n";
                fos.write(location.getBytes());
                while (!isStop()) {
//                    if (!currentLocation.equals(oldLocation)) {
                    if (oldLocation == null) {
                        location = String.valueOf(currentLocation.getLongitude()) + "," + String.valueOf(currentLocation.getLatitude()) + "\n";
                        oldLocation = currentLocation;
                        fos.write(location.getBytes());
                        samplePoints ++;
                        samplePointsText.setText(String.valueOf(samplePoints));
                        Log.d("test", location + " : " + oldLocation.getLongitude() + "," + oldLocation.getLatitude());
                    } else {
                        if ((currentLocation.getLatitude() != oldLocation.getLatitude() && currentLocation.getLongitude() != oldLocation.getLongitude())) {
                            location = String.valueOf(currentLocation.getLongitude()) + "," + String.valueOf(currentLocation.getLatitude()) + "\n";
                            oldLocation = currentLocation;
                            fos.write(location.getBytes());
                            samplePoints++;
                            samplePointsText.setText(String.valueOf(samplePoints));
                            Log.d("test", location + " : " + oldLocation.getLongitude() + "," + oldLocation.getLatitude());

                            Log.d("test", String.valueOf(currentLocation.getExtras().getInt("satellites")));
                        }
                    }
                    Thread.sleep(500);
                }
                Log.d("test", "kończe pobierać punkty");
                stop = false;
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };
    private boolean stop = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//      requesting permissions gor saving and checking location
        requestPer();
//      setting behavior for locationManager
        if (getGPSStatus()) {
            getLocation();
        } else {
            Toast.makeText(this, "GPS jest wyłączony", Toast.LENGTH_SHORT).show();
        }
        locaMen = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
//      getting View element
        longitudeText = findViewById(R.id.longitude);
        latitudeText = findViewById(R.id.latitude);
        samplePointsText = findViewById(R.id.samplePoints);
        pointName = findViewById(R.id.pointName);
//      getting the button to start
        btnStart = findViewById(R.id.btnStart);
//      setting behavior when clicked, starting updating
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//              checking the status of the GPS device, if it's disabled show a Toast
                hideKeyboard();
                pointName.clearFocus();
                if (getGPSStatus()) {
                    update = new Thread(runnable);
                    update.start();

                } else {
                    Toast.makeText(MainActivity.this, "GPS jest wyłączony", Toast.LENGTH_SHORT).show();
                }
            }
        });
//      getting the button for stoping
        btnStop = findViewById(R.id.btnStop);
//      setting behavior when clicked, stoping updating
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//              setting 'stop' to true, to stop the updates
                stop = true;
//                showing a message that measuring is finished
                Toast.makeText(MainActivity.this, "Skończyłem", Toast.LENGTH_LONG).show();
//                making the saved numPoints 0
                samplePoints = 0;
//                resenting the message
                samplePointsText.setText("ilość pobrantch punktów");
//                resenting the oldLocation
                oldLocation = null;
            }
        });
    }

    /**
     * Sets behavior for Location Manager
     */
    @SuppressLint("MissingPermission")
    public void getLocation() {
        locaMen = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        currentLocation = locaMen.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        locaMen.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0, locaLis);
    }

    /**
     * Checks status of the GPS provider
     * @return
     */
    public boolean getGPSStatus() {
        return Settings.Secure.isLocationProviderEnabled(getBaseContext().getContentResolver(), LocationManager.GPS_PROVIDER);
    }

    /**
     * Checks if the app has permission for writing to the external storage
     * @return
     */
    private boolean storagePer () {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Checks if the app has permission for checking in the fine location
     * @return
     */
    private boolean locationPer () {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Requests permissions
     */
    private void requestPer() {
//        ArrayList<String> requestedPer = new ArrayList<>();
        String[] requestedPer = new String[2];
        if (!storagePer()) {
            requestedPer[0] = (Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!locationPer()) {
            requestedPer[1] = (Manifest.permission.ACCESS_FINE_LOCATION);
        }
        boolean empty = true;
        for (int i = 0; i < requestedPer.length; i++) {
            if (requestedPer[i] != null) {
                empty = false;
                break;
            }
        }
        if (!empty) {
            ActivityCompat.requestPermissions(this, requestedPer, REQUEST_CODE_0);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_0) {
            for (int i = 0; i < grantResults.length; i++){
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Permissions requested", permissions[i] + " granted");
                } else {
                    Log.d("Permissions requested", permissions[i] + " not granted");
                    System.exit(0);
                }
            }
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = this.getCurrentFocus();
        if (view == null) {
            view = new View(this);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public synchronized boolean isStop() {
        return stop;
    }
}