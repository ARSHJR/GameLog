package com.example.gamelog;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
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

public class SignUpActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;

    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private EditText emailInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        auth = FirebaseAuth.getInstance();
        emailInput = findViewById(R.id.sign_up_email_input);
        passwordInput = findViewById(R.id.sign_up_password_input);
        confirmPasswordInput = findViewById(R.id.sign_up_confirm_password_input);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        ImageView backButton = findViewById(R.id.sign_up_back_button);
        backButton.setOnClickListener(v -> finish());

        findViewById(R.id.sign_up_submit_button).setOnClickListener(v -> signUp());
        findViewById(R.id.sign_up_google_button).setOnClickListener(v -> signInWithGoogle());

        TextView signInLink = findViewById(R.id.sign_in_link);
        signInLink.setOnClickListener(v -> {
            startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
            finish();
        });
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

    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(SignUpActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    FirebaseUser currentUser = auth.getCurrentUser();
                    if (currentUser == null) {
                        Toast.makeText(SignUpActivity.this, "Authentication failed. Please try again.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    resolveAndPersistBackendUser(currentUser, "google");
                });
    }

    private void signUp() {
        String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
        String password = passwordInput.getText() != null ? passwordInput.getText().toString() : "";
        String confirmPassword = confirmPasswordInput.getText() != null ? confirmPasswordInput.getText().toString() : "";

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "Please fill all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!TextUtils.equals(password, confirmPassword)) {
            Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(SignUpActivity.this, "Sign up failed.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    FirebaseUser currentUser = auth.getCurrentUser();
                    if (currentUser == null) {
                        Toast.makeText(SignUpActivity.this, "Sign up failed.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    resolveAndPersistBackendUser(currentUser, "email_password");
                });
    }

    private void resolveAndPersistBackendUser(FirebaseUser currentUser, String loginProvider) {
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
                editor.putString("login_provider", loginProvider);
                editor.putString("user_email", currentUser.getEmail());
                editor.putString("user_name", currentUser.getDisplayName());
                editor.apply();

                BackendUserHelper.persistBackendUserId(SignUpActivity.this, response.body().getUserId());

                Intent intent = new Intent(SignUpActivity.this, MainShellActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
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
        if (googleSignInClient != null) {
            googleSignInClient.signOut();
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
