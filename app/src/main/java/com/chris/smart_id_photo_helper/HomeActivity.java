package com.chris.smart_id_photo_helper;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.util.Date;

public class HomeActivity extends AppCompatActivity {

    Button albumBtn;
    Button processBtn;
    Button logoutBtn;
    TextView banner;
    FirebaseUser user;
    DatabaseReference reference;
    String userId;
    public static final int CAM_PERM_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        albumBtn = findViewById(R.id.albumButton);
        processBtn = findViewById(R.id.processButton);
        logoutBtn = findViewById(R.id.logoutButton);
        banner = findViewById(R.id.banner);
        user = FirebaseAuth.getInstance().getCurrentUser();
        userId = user.getUid();
        reference = FirebaseDatabase.getInstance().getReference("Users");


        showWelcomeBanner();


        albumBtn.setOnClickListener(v -> {
            Intent albumActivity = new Intent(this, AlbumActivity.class);
            startActivity(albumActivity);
        });

        processBtn.setOnClickListener(v -> {
            getCameraPermission();
        });

        logoutBtn.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent mainActivity = new Intent(this, MainActivity.class);
            startActivity(mainActivity);
        });
    }

    private void showWelcomeBanner() {
        reference.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User profile = snapshot.getValue(User.class);
                if (profile != null) {
                    String name = profile.name;
                    banner.setText("Welcome, " + name);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(HomeActivity.this, "Something wrong...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getCameraPermission() {
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) || (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) || (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, CAM_PERM_CODE);
        } else {
            goToSelectImageActivity();
        }
    }

    private void goToSelectImageActivity(){
        Intent selectImageActivity = new Intent(this, SelectImageActivity.class);
        startActivity(selectImageActivity);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAM_PERM_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this.getApplicationContext(), permissions[1]) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this.getApplicationContext(), permissions[2]) == PackageManager.PERMISSION_GRANTED) {
                goToSelectImageActivity();
            } else {
                Toast.makeText(this, "Permissions are required", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
