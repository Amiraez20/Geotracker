package com.example.geotracker;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private double valLat;
    private double valLng;
    private double valAlt;
    private float  valPrecision;

    private RequestQueue fileRequetes;
    private TextView tvCoordonnees;

    private static final String ENDPOINT = "http://10.0.2.2/geotrack/saveCoord.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvCoordonnees = findViewById(R.id.tvCoordonnees);
        fileRequetes  = Volley.newRequestQueue(getApplicationContext());

        LocationManager gestLoc =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.READ_PHONE_STATE
                    }, 10);
            return;
        }

        gestLoc.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                60000,
                150,
                new LocationListener() {

                    @Override
                    public void onLocationChanged(Location loc) {
                        valLat       = loc.getLatitude();
                        valLng       = loc.getLongitude();
                        valAlt       = loc.getAltitude();
                        valPrecision = loc.getAccuracy();

                        String affichage = "Latitude  : " + valLat
                                + "\nLongitude : " + valLng
                                + "\nAltitude  : " + valAlt
                                + "\nPrécision : " + valPrecision + " m";

                        tvCoordonnees.setText(affichage);
                        Toast.makeText(getApplicationContext(), affichage, Toast.LENGTH_LONG).show();

                        envoyerPoint(valLat, valLng);
                    }

                    @Override
                    public void onStatusChanged(String fournisseur, int statut, Bundle extras) {
                        String libStatut = "";
                        switch (statut) {
                            case LocationProvider.OUT_OF_SERVICE:         libStatut = "HORS_SERVICE"; break;
                            case LocationProvider.TEMPORARILY_UNAVAILABLE: libStatut = "INDISPONIBLE"; break;
                            case LocationProvider.AVAILABLE:               libStatut = "DISPONIBLE";   break;
                        }
                        Toast.makeText(getApplicationContext(),
                                "Fournisseur " + fournisseur + " → " + libStatut,
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onProviderEnabled(String fournisseur) {
                        Toast.makeText(getApplicationContext(),
                                "GPS activé : " + fournisseur, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onProviderDisabled(String fournisseur) {
                        Toast.makeText(getApplicationContext(),
                                "GPS désactivé : " + fournisseur, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void envoyerPoint(final double lat, final double lng) {
        StringRequest requete = new StringRequest(
                Request.Method.POST,
                ENDPOINT,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String reponse) {
                        Toast.makeText(getApplicationContext(),
                                reponse, Toast.LENGTH_SHORT).show();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError erreur) {
                        Toast.makeText(getApplicationContext(),
                                "Échec de l'envoi", Toast.LENGTH_SHORT).show();
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                TelephonyManager gestTel =
                        (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

                HashMap<String, String> params = new HashMap<>();
                SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                params.put("coord_lat",   String.valueOf(lat));
                params.put("coord_lng",   String.valueOf(lng));
                params.put("horodatage",  fmt.format(new Date()));

                if (ActivityCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.READ_PHONE_STATE)
                        != PackageManager.PERMISSION_GRANTED) {
                    params.put("device_code", "inconnu");
                } else {
                    String identifiant = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
                    params.put("device_code", identifiant);
                }

                return params;
            }
        };

        fileRequetes.add(requete);
    }
}