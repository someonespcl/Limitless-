package com.limitless.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.content.ContentValues;
import android.provider.Settings;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.limitless.databinding.ActivitySetUpProfileBinding;
import com.limitless.utils.CustomToast;

public class SetUpProfileActivity extends AppCompatActivity {

	private ActivitySetUpProfileBinding binding;
	private static final int STORAGE_PERMISSION_CODE = 143;
    private static final int CAMERA_PERMISSION_CODE = 144;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivitySetUpProfileBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		binding.uploadImageBtn.setOnClickListener(v -> {
            v.setEnabled(false);
			checkPermissions();
		});
	}

	private ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
			new ActivityResultContracts.StartActivityForResult(), result -> {
                binding.uploadImageBtn.setEnabled(true);
				if (result.getResultCode() == RESULT_OK && result.getData() != null) {
					Uri imageUri = result.getData().getData();
					Glide.with(SetUpProfileActivity.this).load(imageUri).into(binding.setUpProfileIv);
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
        binding.uploadImageBtn.setEnabled(true);
		if (requestCode == STORAGE_PERMISSION_CODE) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				openGallery();
			} else {
				if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
						Manifest.permission.READ_EXTERNAL_STORAGE)) {
					showSettingsRedirectDialog();
				} else {
					CustomToast.showToast(SetUpProfileActivity.this, "Permission denied to access storage");
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
                    binding.uploadImageBtn.setEnabled(true);
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
                    binding.uploadImageBtn.setEnabled(true);
                    dialog.dismiss();
                }).show();
	}

}
