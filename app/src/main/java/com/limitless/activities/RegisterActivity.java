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
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.limitless.R;
import com.limitless.databinding.ActivityRegisterBinding;

public class RegisterActivity extends AppCompatActivity {

	private ActivityRegisterBinding binding;
	private Vibrator vibrator;
    private FirebaseAuth mAuth;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityRegisterBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
        
        mAuth = FirebaseAuth.getInstance();

		vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

		binding.backBtn.setOnClickListener(v -> {
			startActivity(new Intent(RegisterActivity.this, GetStartedActivity.class));
            finishAffinity();
		});

		binding.loginBtn.setOnClickListener(v -> {
			startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
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

		binding.registerBtn.setOnClickListener(v -> {
			String email = binding.enterEmail.getText().toString().trim();
			String password = binding.enterPassword.getText().toString().trim();
			if (email.isEmpty()) {
				vibrateOnError();
				YoYo.with(Techniques.Shake).duration(700).playOn(binding.enterEmail);
				binding.enterEmail.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.wrong, 0);
				return;
			}
			if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
				vibrateOnError();
				YoYo.with(Techniques.Shake).duration(700).playOn(binding.enterEmail);
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
            registerUser(email, password);
		});
	}
    
    private void registerUser(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    FirebaseUser user = mAuth.getCurrentUser();
                    Toast.makeText(RegisterActivity.this, "successful", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(RegisterActivity.this, "Authentication failed.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

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