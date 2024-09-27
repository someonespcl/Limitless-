package com.limitless.activities;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.MotionEvent;
import android.text.method.PasswordTransformationMethod;
import android.text.method.HideReturnsTransformationMethod;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import android.net.Uri;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import com.bumptech.glide.Glide;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthEmailException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.limitless.R;
import com.limitless.databinding.ActivityRegisterBinding;
import com.limitless.databinding.SetupDisplayPictureBottomSheetBinding;
import com.limitless.models.User;
import com.limitless.utils.CustomToast;

public class RegisterActivity extends AppCompatActivity {

	private ActivityRegisterBinding binding;
    private SetupDisplayPictureBottomSheetBinding setupDpBinding;
	private Vibrator vibrator;
    private FirebaseAuth mAuth;
    private FirebaseDatabase database;
    private DatabaseReference userRef;
    private BottomSheetDialog bottomSheetDialog;
    private static final int STORAGE_PERMISSION_CODE = 143;
    
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setupDpBinding = SetupDisplayPictureBottomSheetBinding.inflate(getLayoutInflater());
        bottomSheetDialog = new BottomSheetDialog(RegisterActivity.this);
        bottomSheetDialog.setContentView(setupDpBinding.getRoot());
		setContentView(binding.getRoot());
        
        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        userRef = database.getReference("Users");
        

		vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        showSetupDisplayPictureBottomS();
		binding.backBtn.setOnClickListener(v -> {
			startActivity(new Intent(RegisterActivity.this, GetStartedActivity.class));
            finishAffinity();
		});

		binding.loginBtn.setOnClickListener(v -> {
			startActivity(new Intent(RegisterActivity.this, GetStartedActivity.class));
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
            String phoneNumber = binding.enterPhonenumber.getText().toString().trim(); 
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
                                            showSetupDisplayPictureBottomS();
                                            //startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                                            //finishAffinity();
                                        }
                                    });
                                } else {
                                    CustomToast.showToast(RegisterActivity.this, "Failed to save user data.");
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
                    CustomToast.showToast(RegisterActivity.this, errorMessage);
                }
            }
        });
    }
    
    private void showSetupDisplayPictureBottomS() {
        bottomSheetDialog.show();
        bottomSheetDialog.setCanceledOnTouchOutside(true);
        
        setupDpBinding.galleryImage.setOnClickListener(v -> {
            checkPermissions();
        });
    }
    
    private ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
			new ActivityResultContracts.StartActivityForResult(), result -> {
                setupDpBinding.galleryImage.setEnabled(true);
				if (result.getResultCode() == RESULT_OK && result.getData() != null) {
					Uri imageUri = result.getData().getData();
					Glide.with(RegisterActivity.this).load(imageUri).into(setupDpBinding.galleryImage);
				}
			});
    
    public void openGallery() {
		Intent intent = new Intent(Intent.ACTION_PICK);
		intent.setType("image/*");
		galleryLauncher.launch(intent);
	}
    
    public void checkPermissions() {
		if (ContextCompat.checkSelfPermission(this,
				Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
			if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
				showPermissionDialog();
			} else {
				requestStoragePermission();
			}
		} else {
			openGallery();
		}
	}
    
    private void requestStoragePermission() {
		ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.READ_EXTERNAL_STORAGE },
				STORAGE_PERMISSION_CODE);
	}
    
    @Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
			@NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        setupDpBinding.uploadDisplayPicture.setEnabled(true);
        if (requestCode == STORAGE_PERMISSION_CODE) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				openGallery();
			} else {
				if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
						Manifest.permission.READ_EXTERNAL_STORAGE)) {
					showSettingsRedirectDialog();
				} else {
					CustomToast.showToast(RegisterActivity.this, "Permission denied to access storage");
				}
			}
		}
	}
    
    private void showPermissionDialog() {
		new MaterialAlertDialogBuilder(this).setTitle("Permission Needed")
				.setMessage("This app needs access to your storage to pick images from the gallery.")
				.setPositiveButton("Ok", (dialog, which) -> {
					requestStoragePermission();
				}).setNegativeButton("Cannel", (dialog, which) -> {
                    setupDpBinding.uploadDisplayPicture.setEnabled(true);
					dialog.dismiss();
				}).show();
	}
    
    private void showSettingsRedirectDialog() {
		new MaterialAlertDialogBuilder(this).setTitle("Permission Required").setMessage(
				"Storage permission is needed to pick images from the gallery. Please enable it in the app settings.")
				.setPositiveButton("Go To Settings", (dialog, which) -> {
					Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
					Uri uri = Uri.fromParts("package", getPackageName(), null);
					intent.setData(uri);
					startActivity(intent);
				}).setNegativeButton("Cancel", (dialog, which) -> {
                    setupDpBinding.uploadDisplayPicture.setEnabled(true);
                    dialog.dismiss();
                }).show();
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