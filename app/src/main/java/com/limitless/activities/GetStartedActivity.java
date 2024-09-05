package com.limitless.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
                            Toast.makeText(GetStartedActivity.this, user.getEmail().toString(), Toast.LENGTH_LONG).show();
                            binding.loadingAnim.setAnimation(R.raw.success_1);
                            binding.loadingAnim.setRepeatCount(0);
                        } else {
                            Toast.makeText(GetStartedActivity.this, task.getException().toString(), Toast.LENGTH_LONG).show();
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
