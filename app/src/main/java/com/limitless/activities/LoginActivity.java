package com.limitless.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.MotionEvent;
import android.text.method.PasswordTransformationMethod;
import android.text.method.HideReturnsTransformationMethod;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.limitless.R;
import androidx.appcompat.app.AppCompatActivity;
import com.limitless.databinding.ActivityLoginBinding;
import java.util.regex.Pattern;

public class LoginActivity extends AppCompatActivity {

	private ActivityLoginBinding binding;
	private Vibrator vibrator;
    private FirebaseAuth mAuth;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityLoginBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
        
        mAuth = FirebaseAuth.getInstance();

		vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

		binding.backBtn.setOnClickListener(v -> {
			onBackPressed();
		});

		binding.registerBtn.setOnClickListener(v -> {
			startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
			finishAffinity();
		});

		binding.enterPassword.setOnTouchListener((v, event) -> {
			if (event.getAction() == MotionEvent.ACTION_UP) {
				if (event.getRawX() >= (binding.enterPassword.getRight()
						- binding.enterPassword.getCompoundDrawables()[2].getBounds().width())) {
					boolean isPasswordVisible = !(binding.enterPassword
							.getTransformationMethod() instanceof PasswordTransformationMethod);
					binding.enterPassword
							.setTransformationMethod(isPasswordVisible ? PasswordTransformationMethod.getInstance()
									: HideReturnsTransformationMethod.getInstance());
					binding.enterPassword.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0,
							isPasswordVisible ? R.drawable.eye_off : R.drawable.eye_on, 0);
					binding.enterPassword.setSelection(binding.enterPassword.length());
					return true;
				}
			}
			return false;
		});

		binding.enterEmail.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int end, int count) {
				String email = s.toString().trim();
				if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
					binding.enterEmail.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.wrong, 0);
					return;
				}
				binding.enterEmail.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.checkmark, 0);
			}

			@Override
			public void afterTextChanged(Editable arg0) {
			}
		});

		binding.loginBtn.setOnClickListener(v -> {
			String email = binding.enterEmail.getText().toString().trim();
			String password = binding.enterPassword.getText().toString().trim();

			if (email.isEmpty()) {
				YoYo.with(Techniques.Shake).duration(512).playOn(binding.enterEmail);
				vibrateOnError();
				binding.enterEmail.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.wrong, 0);
				return;
			}
			if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
				vibrateOnError();
				YoYo.with(Techniques.Shake).duration(700).playOn(binding.enterEmail);
				binding.enterEmail.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.wrong, 0);
				return;
			}
			if (password.isEmpty()) {
				vibrateOnError();
				YoYo.with(Techniques.Shake).duration(700).playOn(binding.enterPassword);
				return;
			}
			if (password.length() < 6) {
				vibrateOnError();
				YoYo.with(Techniques.Shake).duration(700).playOn(binding.enterPassword);
				return;
			}
            binding.loginBtn.setText(null);
            binding.loadingAnim.setVisibility(View.VISIBLE);
			letUserLogin(email, password);
		});
	}

	/*
	    After successful validation of 
	    user details this method is called 
	    to store user details to database.
	*/
	private void letUserLogin(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    FirebaseUser user = mAuth.getCurrentUser();
                    binding.loadingAnim.setAnimation(R.raw.success_1);
                    binding.loadingAnim.setRepeatCount(0);
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                } else {
                    String errorMessage;
                    try {
                        throw task.getException();
                    } catch (FirebaseAuthInvalidUserException e) {
                        errorMessage = "Account does not exist. Please sign up.";
                    } catch (FirebaseAuthInvalidCredentialsException e) {
                        errorMessage = "Invalid email or password. Please try again.";
                    } catch (FirebaseNetworkException e) {
                        errorMessage = "Network error. Please check your connection.";
                    } catch (Exception e) {
                        errorMessage = "Authentication failed. Please try again.";
                    }
                    binding.loginBtn.setText(getString(R.string.login_btn_hint));
                    binding.loadingAnim.setVisibility(View.GONE);
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }
        });
	}

	/*
	    on error the view is vibrated
	    for a better error understanding.
	*/
	private void vibrateOnError() {
		if (vibrator != null && vibrator.hasVibrator()) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				vibrator.vibrate(VibrationEffect.createOneShot(128, VibrationEffect.DEFAULT_AMPLITUDE));
			} else {
				vibrator.vibrate(128);
			}
		}
	}
}
