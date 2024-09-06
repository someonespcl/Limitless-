package com.limitless.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
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
import androidx.appcompat.app.AppCompatActivity;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthEmailException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.limitless.R;
import com.limitless.databinding.ActivityRegisterBinding;
import com.limitless.models.User;

public class RegisterActivity extends AppCompatActivity {

	private ActivityRegisterBinding binding;
	private Vibrator vibrator;
    private FirebaseAuth mAuth;
    private FirebaseDatabase database;
    private DatabaseReference userRef;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityRegisterBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
        
        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        userRef = database.getReference("Users");

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
            binding.registerBtn.setText(null);
            binding.loadingAnim.setVisibility(View.VISIBLE);
            registerUser(email, password);
		});
	}
    
    private void registerUser(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    FirebaseUser firebaseUser = mAuth.getCurrentUser();
                    
                    long timeStamp = System.currentTimeMillis();
                    String userType = "Email/Password";
                    
                    User user = new User(email, userType, null, timeStamp);
                    userRef.child(firebaseUser.getUid()).setValue(user)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    binding.loadingAnim.setAnimation(R.raw.success_1);
                                    binding.loadingAnim.setRepeatCount(0);
                                    binding.loadingAnim.addAnimatorListener(new AnimatorListenerAdapter(){
                                        @Override
                                        public void onAnimationEnd(Animator animation) {
                                            super.onAnimationEnd(animation);
                                            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                                            finishAffinity();
                                        }
                                    });
                                } else {
                                    Toast.makeText(RegisterActivity.this, "Failed to save user data.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                        
                } else {
                    String errorMessage;
                    try {
                        throw task.getException();
                    } catch (FirebaseAuthInvalidCredentialsException e) {
                        errorMessage = "The email address is malformed. Please check and try again.";
                    } catch (FirebaseAuthUserCollisionException e) {
                        errorMessage = "An account already exists with this email.";
                    } catch (FirebaseNetworkException e) {
                        errorMessage = "Network error. Please check your connection.";
                    } catch (FirebaseAuthEmailException e) {
                        errorMessage = "There was a problem with your email action. Please try again.";
                    } catch (Exception e) {
                        errorMessage = "Authentication failed. Please try again.";
                    }
                    binding.registerBtn.setText(getString(R.string.register_btn_hint));
                    binding.loadingAnim.setVisibility(View.GONE);    
                    Toast.makeText(RegisterActivity.this, errorMessage,
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