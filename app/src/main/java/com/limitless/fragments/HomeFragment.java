package com.limitless.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.limitless.databinding.FragmentHomeBinding;
import com.limitless.models.User;

public class HomeFragment extends Fragment {
    
    private FragmentHomeBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference databaseRefer;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        
        databaseRefer = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());
        getProfileImage();
        
        binding.notifications.setOnClickListener(v -> {
            binding.notifications.playAnimation();
        });
        
        
        return view;
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
                        	Glide.with(getContext()).load(imageUri).into(binding.profileView);
                        }    
                    }    
                }
            }
            @Override
            public void onCancelled(DatabaseError arg0) {
            }
        });
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        this.binding = null;
    }
}
