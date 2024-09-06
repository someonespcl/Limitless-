package com.limitless.activities;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.limitless.databinding.ActivityProfileBinding;
import com.limitless.models.User;

public class ProfileActivity extends AppCompatActivity {
    
    private ActivityProfileBinding binding;
    private FirebaseUser currentUser;
    private DatabaseReference databaseRefer;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        databaseRefer = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());
        binding.userNameTv.setText(currentUser.getDisplayName().toString());
        getProfileImage();
    }
    
    private void getProfileImage() {
        databaseRefer.addValueEventListener(new ValueEventListener(){
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                	User user = dataSnapshot.getValue(User.class);
                    if(user != null) {
                    	String imageUri = user.getImageUri();
                        if(imageUri != null && !imageUri.isEmpty()) {
                        	Glide.with(ProfileActivity.this).load(imageUri).into(binding.userImageView);
                        }    
                    }    
                }
            }
            @Override
            public void onCancelled(DatabaseError arg0) {
            }
        });
    }
}
