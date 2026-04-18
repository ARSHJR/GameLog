package com.example.gamelog;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check for existing login
        SharedPreferences sharedPreferences = getSharedPreferences("GameLogPrefs", MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean("is_logged_in", false);
        if (isLoggedIn && BackendUserHelper.hasStoredBackendUserId(this)) {
            startActivity(new Intent(LoginActivity.this, MainShellActivity.class));
            finish();
            return;
        }

        if (isLoggedIn) {
            sharedPreferences.edit().putBoolean("is_logged_in", false).apply();
            BackendUserHelper.clearBackendUserId(this);
        }

        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        findViewById(R.id.btnGoogleSignIn).setOnClickListener(v -> signIn());
        findViewById(R.id.login_button).setOnClickListener(v -> {
            signIn();
        });
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser currentUser = mAuth.getCurrentUser();
                        if (currentUser == null) {
                            Toast.makeText(LoginActivity.this, "Authentication failed. Please try again.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        resolveAndPersistBackendUser(currentUser);
                    } else {
                        // If sign in fails, display a message to the user.
                        Toast.makeText(LoginActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void resolveAndPersistBackendUser(FirebaseUser currentUser) {
        String authUserId = currentUser.getUid();
        String email = currentUser.getEmail();
        String displayName = currentUser.getDisplayName();

        ApiService apiService = RetrofitClient.getApiService();
        apiService.resolveBackendUser(authUserId, email, displayName).enqueue(new Callback<ResolvedBackendUser>() {
            @Override
            public void onResponse(Call<ResolvedBackendUser> call, Response<ResolvedBackendUser> response) {
                if (!response.isSuccessful() || response.body() == null || TextUtils.isEmpty(response.body().getUserId())) {
                    handleBackendIdentityFailure("Unable to resolve backend account.");
                    return;
                }

                SharedPreferences sharedPreferences = getSharedPreferences("GameLogPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("is_logged_in", true);
                editor.putString("login_provider", "google");
                editor.putString("user_email", currentUser.getEmail());
                editor.putString("user_name", currentUser.getDisplayName());
                editor.apply();

                BackendUserHelper.persistBackendUserId(LoginActivity.this, response.body().getUserId());

                startActivity(new Intent(LoginActivity.this, MainShellActivity.class));
                finish();
            }

            @Override
            public void onFailure(Call<ResolvedBackendUser> call, Throwable t) {
                handleBackendIdentityFailure("Unable to resolve backend account.");
            }
        });
    }

    private void handleBackendIdentityFailure(String message) {
        BackendUserHelper.clearBackendUserId(this);
        SharedPreferences sharedPreferences = getSharedPreferences("GameLogPrefs", MODE_PRIVATE);
        sharedPreferences.edit().putBoolean("is_logged_in", false).apply();

        FirebaseAuth.getInstance().signOut();
        mGoogleSignInClient.signOut();
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
