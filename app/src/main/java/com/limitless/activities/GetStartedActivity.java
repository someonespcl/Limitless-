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
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Patterns;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthMultiFactorException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.limitless.R;
import com.limitless.databinding.ActivityGetStartedBinding;
import com.limitless.databinding.ForgotPasswordBottomSheetBinding;
import com.limitless.databinding.LoginBottomSheetBinding;
import com.limitless.models.User;
import com.limitless.utils.CustomToast;

public class GetStartedActivity extends AppCompatActivity {

	private ActivityGetStartedBinding binding;
	private FirebaseAuth mAuth;
	private static final int RC_SIGN_IN = 1411;
	private GoogleSignInClient mGoogleSignInClient;
	private FirebaseDatabase database;
	private DatabaseReference userRefer;
	private LoginBottomSheetBinding loginBinding;
	private BottomSheetDialog loginDialog, forgotPasswordDialog;
	private Vibrator vibrator;
	private ForgotPasswordBottomSheetBinding forgotPasswordBinding;

	private SharedPreferences sharedPreferences;
	private static final String PREFS_NAME = "LoginPrefs";
	private static final String KEY_EMAIL = "useremail";
	private static final String KEY_PASSWORD = "password";
	private static final String KEY_SAVE_CREDENTIALS = "saveCredentials";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityGetStartedBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
		loginDialog = new BottomSheetDialog(GetStartedActivity.this);
		loginBinding = LoginBottomSheetBinding.inflate(getLayoutInflater());
		loginDialog.setContentView(loginBinding.getRoot());
		forgotPasswordDialog = new BottomSheetDialog(GetStartedActivity.this);
		forgotPasswordBinding = ForgotPasswordBottomSheetBinding.inflate(getLayoutInflater());
		forgotPasswordDialog.setContentView(forgotPasswordBinding.getRoot());

		mAuth = FirebaseAuth.getInstance();
		database = FirebaseDatabase.getInstance();
		userRefer = database.getReference("Users");

		vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		loadSavedCredentials();

		GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
				.requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build();

		mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

		binding.getStartedBtn.setOnClickListener(v -> {
			showLoginDialog();
		});

		binding.continueWithGoogle.setOnClickListener(v -> {
			binding.loadingAnim.setVisibility(View.VISIBLE);
			binding.continueWithGoogle.setText(null);
			binding.continueWithGoogle.setIcon(null);
			signIn();
		});
	}

	private void showLoginDialog() {
		loginDialog.setCanceledOnTouchOutside(false);
		loginDialog.show();

		loginBinding.closeButton.setOnClickListener(close -> {
			loginDialog.dismiss();
		});

		loginBinding.enterEmail.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int end, int count) {
				String email = s.toString().trim();
				if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
					loginBinding.enterEmail.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.wrong, 0);
					return;
				}
				loginBinding.enterEmail.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.checkmark, 0);
			}

			@Override
			public void afterTextChanged(Editable arg0) {
			}
		});

		loginBinding.enterPassword.setOnTouchListener((v, event) -> {
			if (event.getAction() == MotionEvent.ACTION_UP) {
				if (event.getRawX() >= (loginBinding.enterPassword.getRight()
						- loginBinding.enterPassword.getCompoundDrawables()[2].getBounds().width())) {
					boolean isPasswordVisible = !(loginBinding.enterPassword
							.getTransformationMethod() instanceof PasswordTransformationMethod);
					loginBinding.enterPassword
							.setTransformationMethod(isPasswordVisible ? PasswordTransformationMethod.getInstance()
									: HideReturnsTransformationMethod.getInstance());
					loginBinding.enterPassword.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0,
							isPasswordVisible ? R.drawable.eye_off : R.drawable.eye_on, 0);
					loginBinding.enterPassword.setSelection(loginBinding.enterPassword.length());
					return true;
				}
			}
			return false;
		});

		loginBinding.loginBtn.setOnClickListener(letUserLogin -> {
			validateDetails();
		});

		loginBinding.forgetPasswordBtn.setOnClickListener(v -> {
			showForgotPasswordBottomSheet();
		});
	}

	private void validateDetails() {
		String email = loginBinding.enterEmail.getText().toString().trim();
		String password = loginBinding.enterPassword.getText().toString().trim();

		if (email.isEmpty()) {
			vibrateOnError(loginBinding.enterEmail);
			loginBinding.enterEmail.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.wrong, 0);
			return;
		}
		if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
			vibrateOnError(loginBinding.enterEmail);
			loginBinding.enterEmail.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.wrong, 0);
			return;
		}
		if (password.isEmpty()) {
			vibrateOnError(loginBinding.enterPassword);
			return;
		}
		if (password.length() < 6) {
			vibrateOnError(loginBinding.enterPassword);
			return;
		}
		if (loginBinding.saveCredentialsCheckbox.isChecked()) {
			saveCredentials(email, password);
		} else {
			clearSavedCredentials();
		}
		letUserLogin(email, password);
	}

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

											} else {
												CustomToast.showToast(GetStartedActivity.this,
														"Failed to save user data.");
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
							CustomToast.showToast(GetStartedActivity.this, errorMessage);
						}
					}
				});
	}

	private void showForgotPasswordBottomSheet() {
		forgotPasswordDialog.setCanceledOnTouchOutside(false);

		forgotPasswordBinding.closeBtn.setOnClickListener(v -> {
			forgotPasswordDialog.dismiss();
		});

		forgotPasswordBinding.enterForgotEmail.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int end, int count) {
				String email = s.toString().trim();
				if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
					forgotPasswordBinding.enterForgotEmail.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0,
							R.drawable.wrong, 0);
					return;
				}
				forgotPasswordBinding.enterForgotEmail.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0,
						R.drawable.checkmark, 0);
			}

			@Override
			public void afterTextChanged(Editable arg0) {
			}
		});

		forgotPasswordBinding.sendForgotEmail.setOnClickListener(v -> {
			String forgotEmail = forgotPasswordBinding.enterForgotEmail.getText().toString().trim();
			if (forgotEmail.isEmpty()) {
				vibrateOnError(forgotPasswordBinding.enterForgotEmail);
				forgotPasswordBinding.enterForgotEmail.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0,
						R.drawable.wrong, 0);
				return;
			}
			if (!Patterns.EMAIL_ADDRESS.matcher(forgotEmail).matches()) {
				vibrateOnError(forgotPasswordBinding.enterForgotEmail);
				forgotPasswordBinding.enterForgotEmail.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0,
						R.drawable.wrong, 0);
				return;
			}
			forgotPasswordBinding.sendForgotEmail.setText(null);
			forgotPasswordBinding.loadingAnim.setVisibility(View.VISIBLE);
			sendPasswordResetEmail(forgotEmail);
		});

		forgotPasswordDialog.show();
	}

	private void sendPasswordResetEmail(String email) {
		mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
			if (task.isSuccessful()) {

				forgotPasswordBinding.loadingAnim.setAnimation(R.raw.success_1);
				forgotPasswordBinding.loadingAnim.setRepeatCount(0);
				forgotPasswordBinding.loadingAnim.addAnimatorListener(new AnimatorListenerAdapter() {
					@Override
					public void onAnimationEnd(Animator animation) {
						super.onAnimationEnd(animation);
						forgotPasswordDialog.dismiss();
					}
				});
			} else {
				String errorMessage;
				try {
					throw task.getException();
				} catch (FirebaseAuthInvalidUserException e) {
					errorMessage = "No account found with this email address.";
				} catch (FirebaseAuthInvalidCredentialsException e) {
					errorMessage = "Invalid email format. Please check and try again.";
				} catch (FirebaseNetworkException e) {
					errorMessage = "Network error. Please check your connection.";
				} catch (Exception e) {
					errorMessage = "Error: " + task.getException().getLocalizedMessage();
				}
				forgotPasswordBinding.sendForgotEmail.setText("Send Email");
				forgotPasswordBinding.loadingAnim.setVisibility(View.GONE);
				CustomToast.showToast(GetStartedActivity.this, errorMessage);
				forgotPasswordDialog.dismiss();
			}
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
		mAuth.signInWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
			@Override
			public void onComplete(@NonNull Task<AuthResult> task) {
				if (task.isSuccessful()) {
					FirebaseUser firebaseUser = mAuth.getCurrentUser();
					String userType = "Google";
					long timeStamp = System.currentTimeMillis();
					User user = new User(firebaseUser.getEmail(), userType, firebaseUser.getPhotoUrl().toString(),
							timeStamp);
					userRefer.child(firebaseUser.getUid()).setValue(user)
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
												startActivity(new Intent(GetStartedActivity.this, MainActivity.class));
												finishAffinity();
											}
										});
									} else {
										Toast.makeText(GetStartedActivity.this, "Failed to save user data.",
												Toast.LENGTH_SHORT).show();
									}
								}
							});

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

			loginBinding.enterEmail.setText(savedEmail);
			loginBinding.enterPassword.setText(savedPassword);
			loginBinding.saveCredentialsCheckbox.setChecked(true);
		}
	}

	private void clearSavedCredentials() {
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.remove(KEY_EMAIL);
		editor.remove(KEY_PASSWORD);
		editor.putBoolean(KEY_SAVE_CREDENTIALS, false);
		editor.apply();
	}

	private void vibrateOnError(View obj) {
		if (vibrator != null && vibrator.hasVibrator()) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				YoYo.with(Techniques.Shake).duration(700).playOn(obj);
				vibrator.vibrate(VibrationEffect.createOneShot(128, VibrationEffect.DEFAULT_AMPLITUDE));
			} else {
				YoYo.with(Techniques.Shake).duration(700).playOn(obj);
				vibrator.vibrate(128);
			}
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		final FirebaseUser user = mAuth.getCurrentUser();
		if (user != null) {
			startActivity(new Intent(GetStartedActivity.this, MainActivity.class));
			finishAffinity();
		} else {

		}
	}
}
