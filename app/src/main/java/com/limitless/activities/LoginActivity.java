package com.limitless.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.text.method.PasswordTransformationMethod;
import android.text.method.HideReturnsTransformationMethod;
import android.view.View;
import androidx.annotation.NonNull;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.limitless.R;
import androidx.appcompat.app.AppCompatActivity;
import com.limitless.databinding.ActivityLoginBinding;
import com.limitless.databinding.ForgotPasswordBottomSheetBinding;
import com.limitless.utils.CustomToast;

public class LoginActivity extends AppCompatActivity {

	private ActivityLoginBinding binding;
	private ForgotPasswordBottomSheetBinding forgotPasswordViewBinding;
    private BottomSheetDialog bottomSheetDialog;
	private Vibrator vibrator;
	private FirebaseAuth mAuth;
	private FirebaseDatabase database;
	private DatabaseReference userRefer;

	private SharedPreferences sharedPreferences;
	private static final String PREFS_NAME = "LoginPrefs";
	private static final String KEY_EMAIL = "useremail";
	private static final String KEY_PASSWORD = "password";
	private static final String KEY_SAVE_CREDENTIALS = "saveCredentials";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityLoginBinding.inflate(getLayoutInflater());
		forgotPasswordViewBinding = ForgotPasswordBottomSheetBinding.inflate(getLayoutInflater());
        bottomSheetDialog = new BottomSheetDialog(this);
		bottomSheetDialog.setContentView(forgotPasswordViewBinding.getRoot());
		setContentView(binding.getRoot());

		mAuth = FirebaseAuth.getInstance();
		database = FirebaseDatabase.getInstance();
		userRefer = database.getReference("Users");

		vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		loadSavedCredentials();

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
			if (binding.saveCredentialsCheckbox.isChecked()) {
				saveCredentials(email, password);
			} else {
				clearSavedCredentials();
			}
			letUserLogin(email, password);
		});

		binding.forgetPasswordBtn.setOnClickListener(v -> {
			showForgotPasswordBottomSheet();
		});
	}

	/*
	    After successful validation of 
	    user details this method is called 
	    to store user details to database.
	*/
	private void letUserLogin(String email, String password) {
		mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this,
				new OnCompleteListener<AuthResult>() {
					@Override
					public void onComplete(@NonNull Task<AuthResult> task) {
						if (task.isSuccessful()) {
							FirebaseUser firebaseUser = mAuth.getCurrentUser();

							long loginTime = System.currentTimeMillis();
							userRefer.child(firebaseUser.getUid()).child("lastLoginTime").setValue(loginTime)
									.addOnCompleteListener(new OnCompleteListener<Void>() {
										@Override
										public void onComplete(@NonNull Task<Void> task) {
											if (task.isSuccessful()) {
												binding.loadingAnim.setAnimation(R.raw.success_1);
												binding.loadingAnim.setRepeatCount(0);
												binding.loadingAnim.addAnimatorListener(new AnimatorListenerAdapter() {
													@Override
													public void onAnimationEnd(Animator animation) {
														super.onAnimationEnd(animation);
														startActivity(new Intent(LoginActivity.this,
																SetUpProfileActivity.class));
														finishAffinity();
													}
												});
											} else {
												CustomToast.showToast(LoginActivity.this, "Failed to save user data.");
											}
										}
									});
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
							CustomToast.showToast(LoginActivity.this, errorMessage);
						}
					}
				});
	}

	private void showForgotPasswordBottomSheet() {
        bottomSheetDialog.setCanceledOnTouchOutside(false);
        
        forgotPasswordViewBinding.closeBtn.setOnClickListener( v -> {
            bottomSheetDialog.dismiss();
        });
        
		forgotPasswordViewBinding.enterForgotEmail.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int end, int count) {
				String email = s.toString().trim();
				if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
					forgotPasswordViewBinding.enterForgotEmail.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0,
							R.drawable.wrong, 0);
					return;
				}
				forgotPasswordViewBinding.enterForgotEmail.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0,
						R.drawable.checkmark, 0);
			}

			@Override
			public void afterTextChanged(Editable arg0) {
			}
		});

		forgotPasswordViewBinding.sendForgotEmail.setOnClickListener(v -> {
			String forgotEmail = forgotPasswordViewBinding.enterForgotEmail.getText().toString().trim();
			if (forgotEmail.isEmpty()) {
				YoYo.with(Techniques.Shake).duration(512).playOn(forgotPasswordViewBinding.enterForgotEmail);
				vibrateOnError();
				forgotPasswordViewBinding.enterForgotEmail.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0,
						R.drawable.wrong, 0);
				return;
			}
			if (!Patterns.EMAIL_ADDRESS.matcher(forgotEmail).matches()) {
				vibrateOnError();
				YoYo.with(Techniques.Shake).duration(700).playOn(forgotPasswordViewBinding.enterForgotEmail);
				forgotPasswordViewBinding.enterForgotEmail.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0,
						R.drawable.wrong, 0);
				return;
			}
			forgotPasswordViewBinding.sendForgotEmail.setText(null);
			forgotPasswordViewBinding.loadingAnim.setVisibility(View.VISIBLE);
			sendPasswordResetEmail(forgotEmail);
		});
        
		bottomSheetDialog.show();
	}

	private void sendPasswordResetEmail(String email) {
		mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
			if (task.isSuccessful()) {

				binding.loadingAnim.setAnimation(R.raw.success_1);
				binding.loadingAnim.setRepeatCount(0);
				binding.loadingAnim.addAnimatorListener(new AnimatorListenerAdapter() {
					@Override
					public void onAnimationEnd(Animator animation) {
						super.onAnimationEnd(animation);
						bottomSheetDialog.dismiss();
					}
				});
			} else {
                String errorMessage;
                try {
                    throw task.getException();
                }    
                catch (FirebaseAuthInvalidUserException e) {
                    errorMessage = "No account found with this email address.";
                } catch (FirebaseAuthInvalidCredentialsException e) {
                    errorMessage = "Invalid email format. Please check and try again.";
                } catch (FirebaseNetworkException e) {
                    errorMessage = "Network error. Please check your connection.";
                } catch (Exception e) {
                    errorMessage = "Error: " + task.getException().getLocalizedMessage();
                }
                forgotPasswordViewBinding.sendForgotEmail.setText("Send Email");
			    forgotPasswordViewBinding.loadingAnim.setVisibility(View.GONE);
                CustomToast.showToast(LoginActivity.this, errorMessage);
                bottomSheetDialog.dismiss();
			}
		});
	}

	private void saveCredentials(String email, String password) {
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putString(KEY_EMAIL, email);
		editor.putString(KEY_PASSWORD, password);
		editor.putBoolean(KEY_SAVE_CREDENTIALS, true);
		editor.apply();
	}

	private void loadSavedCredentials() {
		boolean saveCredentials = sharedPreferences.getBoolean(KEY_SAVE_CREDENTIALS, false);
		if (saveCredentials) {
			String savedEmail = sharedPreferences.getString(KEY_EMAIL, "");
			String savedPassword = sharedPreferences.getString(KEY_PASSWORD, "");

			binding.enterEmail.setText(savedEmail);
			binding.enterPassword.setText(savedPassword);
			binding.saveCredentialsCheckbox.setChecked(true);
		}
	}

	private void clearSavedCredentials() {
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.remove(KEY_EMAIL);
		editor.remove(KEY_PASSWORD);
		editor.putBoolean(KEY_SAVE_CREDENTIALS, false);
		editor.apply();
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
