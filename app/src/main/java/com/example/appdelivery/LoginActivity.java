package com.example.appdelivery;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private FirebaseAuth mAuth;
    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Inicializar FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        // Inicializar FirebaseDatabase
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");

        // Inicializar FusedLocationProviderClient para obtener la ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Inicializar vistas
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        Button loginButton = findViewById(R.id.loginButton);

        // Configurar el botón de login
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                if (TextUtils.isEmpty(email)) {
                    emailEditText.setError("Ingresa un email");
                    return;
                }

                if (TextUtils.isEmpty(password)) {
                    passwordEditText.setError("Ingresa una contraseña");
                    return;
                }

                // Autenticar con Firebase
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    // Login exitoso
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    Toast.makeText(LoginActivity.this, "Login exitoso", Toast.LENGTH_SHORT).show();

                                    // Obtener la ubicación del dispositivo
                                    if (user != null) {
                                        getLastLocation(user.getUid());
                                    }

                                    // Abrir MenuActivity
                                    Intent intent = new Intent(LoginActivity.this, MenuActivity.class);
                                    startActivity(intent);
                                } else {
                                    // Error de autenticación
                                    Toast.makeText(LoginActivity.this, "Error en el login: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });
    }

    // Método para obtener la última ubicación conocida
    private void getLastLocation(final String userId) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Solicitar permisos de ubicación si no están otorgados
            ActivityCompat.requestPermissions(LoginActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            // Almacenar la ubicación en Firebase Realtime Database
                            saveLocationToFirebase(userId, location);
                        } else {
                            Toast.makeText(LoginActivity.this, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // Método para guardar la ubicación en Firebase
    private void saveLocationToFirebase(String userId, Location location) {
        // Crear un objeto de ubicación
        UserLocation userLocation = new UserLocation(location.getLatitude(), location.getLongitude());

        // Guardar la ubicación bajo el ID del usuario en Firebase Realtime Database
        databaseReference.child(userId).setValue(userLocation)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "Ubicación guardada exitosamente", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(LoginActivity.this, "Error al guardar la ubicación", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // Clase interna para almacenar las coordenadas de la ubicación
    public static class UserLocation {
        public double latitude;
        public double longitude;

        public UserLocation() {
            // Constructor vacío requerido por Firebase
        }

        public UserLocation(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}

