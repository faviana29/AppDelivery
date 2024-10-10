package com.example.appdelivery;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;

public class MenuActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final String CHANNEL_ID = "cold_chain_channel";
    private static final int LIMITE_TEMPERATURA = -18; // Temperatura límite en grados Celsius

    private FusedLocationProviderClient fusedLocationClient;
    private GoogleMap mMap;
    private TextView costoDespachoTextView;
    private TextView temperaturaTextView; // TextView para mostrar la temperatura

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        costoDespachoTextView = findViewById(R.id.costoDespachoTextView);
        temperaturaTextView = findViewById(R.id.temperaturaTextView); // Asegúrate de tener este TextView en tu layout

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        createNotificationChannel(); // Crear el canal de notificaciones
        verificarTemperatura(); // Verificar la temperatura en el inicio
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            obtenerUbicacionGPS();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }
    }

    private void obtenerUbicacionGPS() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                            LatLng userLocation = new LatLng(latitude, longitude);
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));

                            double totalCompra = 30000;
                            double distancia = calcularDistancia(userLocation, new LatLng(0, 0));
                            double costoDespacho = calcularCostoDespacho(totalCompra, distancia);
                            costoDespachoTextView.setText("Costo de Despacho: $" + costoDespacho);
                        } else {
                            Toast.makeText(MenuActivity.this, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private double calcularCostoDespacho(double totalCompra, double distancia) {
        double costoDespacho;
        if (totalCompra >= 50000) {
            costoDespacho = 0;
        } else if (totalCompra >= 25000 && totalCompra <= 49999) {
            costoDespacho = 150 * distancia;
        } else {
            costoDespacho = 300 * distancia;
        }
        return costoDespacho;
    }

    private double calcularDistancia(LatLng origen, LatLng destino) {
        double earthRadius = 6371; // Kilómetros
        double dLat = Math.toRadians(destino.latitude - origen.latitude);
        double dLng = Math.toRadians(destino.longitude - origen.longitude);
        double sindLat = Math.sin(dLat / 2);
        double sindLng = Math.sin(dLng / 2);
        double a = Math.pow(sindLat, 2) + Math.pow(sindLng, 2)
                * Math.cos(Math.toRadians(origen.latitude)) * Math.cos(Math.toRadians(destino.latitude));
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }



    private int obtenerTemperaturaCongelador() {
        return (int) (Math.random() * (-15 - (-25)) + (-25));
    }

    private void verificarTemperatura() {
        int temperaturaActual = obtenerTemperaturaCongelador();
        temperaturaTextView.setText("Temperatura: " + temperaturaActual + "°C");

        if (temperaturaActual > LIMITE_TEMPERATURA) {
            emitirAlarma(temperaturaActual);
        }
    }

    private void emitirAlarma(int temperatura) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("¡Alerta de temperatura!")
                .setContentText("La temperatura ha superado el límite: " + temperatura + "°C")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        notificationManager.notify(1, builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "ColdChainChannel";
            String description = "Canal para alertas de temperatura de la cadena de frío";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                obtenerUbicacionGPS();
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }
}



