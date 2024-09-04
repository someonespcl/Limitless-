package com.limitless.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.limitless.databinding.ActivityGetStartedBinding;

public class GetStartedActivity extends AppCompatActivity {
    
    private ActivityGetStartedBinding binding;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGetStartedBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        binding.getStartedBtn.setOnClickListener(v -> {
            startActivity(new Intent(GetStartedActivity.this, LoginActivity.class));
        });
    }
}
