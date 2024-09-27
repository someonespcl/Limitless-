package com.limitless.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.limitless.adapters.UsersAdapter;
import com.limitless.databinding.FragmentUsersBinding;
import com.limitless.models.DisplayUsers;
import java.util.ArrayList;
import java.util.List;

public class UsersFragment extends Fragment {

    private FragmentUsersBinding binding;
    private UsersAdapter usersAdapter;
    private List<DisplayUsers> usersList;
    private DatabaseReference usersRef;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentUsersBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        binding.usersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        usersList = new ArrayList<>();
        usersAdapter = new UsersAdapter(getContext(), usersList);

        binding.usersRecyclerView.setAdapter(usersAdapter);
        usersRef = FirebaseDatabase.getInstance().getReference("Users");

        usersRef.addValueEventListener(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapShot) {
                        usersList.clear();
                        for(DataSnapshot dataSnapshot : snapShot.getChildren()) {
                        	DisplayUsers users = dataSnapshot.getValue(DisplayUsers.class);
                            usersList.add(users);
                        }
                        usersAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), error.getMessage().toString(), Toast.LENGTH_LONG).show();
                    }
                });

        return view;
    }
}
