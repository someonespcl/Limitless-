package com.limitless.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.limitless.databinding.ActivitySetUpProfileBinding;

public class SetUpProfileActivity extends AppCompatActivity {
    
    private ActivitySetUpProfileBinding binding;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySetUpProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }
}
