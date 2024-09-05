package com.limitless.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthMultiFactorException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.FirebaseNetworkException;
import com.limitless.R;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.limitless.databinding.ActivityGetStartedBinding;

public class GetStartedActivity extends AppCompatActivity {
    
    private ActivityGetStartedBinding binding;
    private FirebaseAuth mAuth;
    private static final int RC_SIGN_IN = 1411;
    private GoogleSignInClient mGoogleSignInClient;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGetStartedBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        
        mAuth = FirebaseAuth.getInstance();
        
        binding.getStartedBtn.setOnClickListener(v -> {
            startActivity(new Intent(GetStartedActivity.this, LoginActivity.class));
        });
        
        binding.continueWithGoogle.setOnClickListener(v -> {
            binding.loadingAnim.setVisibility(View.VISIBLE);
            binding.continueWithGoogle.setText(null);
            binding.continueWithGoogle.setIcon(null);
            signIn();
        });
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(GetStartedActivity.this, e.getLocalizedMessage().toString(), Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            binding.loadingAnim.setAnimation(R.raw.success_1);
                            binding.loadingAnim.setRepeatCount(0);
                            startActivity(new Intent(GetStartedActivity.this, MainActivity.class));
                            finishAffinity();
                        } else {
                            String errorMessage;
                            try {
                                throw task.getException();
                            } catch (FirebaseAuthInvalidCredentialsException e) {
                                errorMessage = "Invalid credentials. Please try again.";
                            } catch (FirebaseAuthUserCollisionException e) {
                                errorMessage = "An account already exists with the same email.";
                            } catch (FirebaseNetworkException e) {
                                errorMessage = "Network error. Please check your connection.";
                            } catch (FirebaseAuthMultiFactorException e) {
                                errorMessage = "Multi-factor authentication is required.";
                            } catch (Exception e) {
                                errorMessage = "Google sign-in failed. Please try again.";
                            }
                            Toast.makeText(GetStartedActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            binding.loadingAnim.setVisibility(View.GONE);
                            binding.continueWithGoogle.setText(getString(R.string.continue_with_google_hint));
                            binding.continueWithGoogle.setIcon(getDrawable(R.drawable.google));
                        }
                    }
                });
    }
    
    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
}
