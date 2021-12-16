package com.chris.smart_id_photo_helper;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {

    Button registerBtn;
    EditText editTextEmail, editTextUsername, editTextpassword;
    FirebaseAuth mAuth;
    ProgressBar progressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        mAuth = FirebaseAuth.getInstance();

        registerBtn = findViewById(R.id.registerButton);
        progressBar = findViewById(R.id.progressBar);
        editTextEmail = findViewById(R.id.editTextTextEmailAddress);
        editTextUsername = findViewById(R.id.editTextTextPersonName);
        editTextpassword = findViewById(R.id.editTextTextPassword);


        registerBtn.setOnClickListener(v -> {
            registerUser();
        });

    }

    private void registerUser() {
        String email = editTextEmail.getText().toString().trim();
        String username = editTextUsername.getText().toString().trim();
        String password = editTextpassword.getText().toString().trim();

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Please provide valid email!");
            editTextEmail.requestFocus();
            return;
        }

        if (username.isEmpty()) {
            editTextUsername.setError("Username is required!");
            editTextUsername.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            editTextpassword.setError("Password is required!");
            editTextpassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            editTextpassword.setError("Minimum length of password should be 6 characters!");
            editTextpassword.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);


        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            progressBar.setVisibility(View.VISIBLE);
                            Log.d("register", "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            User new_user = new User(email, username, password);
                            FirebaseDatabase.getInstance().getReference("Users").child(user.getUid()).setValue(new_user).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(RegisterActivity.this, "User has been registered successfully.", Toast.LENGTH_SHORT).show();
                                    } else {
                                        progressBar.setVisibility(View.GONE);
                                        Toast.makeText(RegisterActivity.this, "Register failed",
                                                Toast.LENGTH_SHORT).show();
                                    }

                                }
                            });

                            Intent homeActivity = new Intent(RegisterActivity.this, HomeActivity.class);
                            startActivity(homeActivity);
                        } else {
                            // If sign in fails, display a message to the user.
                            progressBar.setVisibility(View.GONE);
                            Log.w("register", "createUserWithEmail:failure", task.getException());
                            Toast.makeText(RegisterActivity.this, "Register failed, this email has already registered",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });

    }


}
