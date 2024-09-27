package com.limitless.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.limitless.models.DisplayUsers;
import java.util.List;
import com.limitless.R;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UsersViewHolder> {

    private Context context;
    private List<DisplayUsers> usersList;

    public UsersAdapter(Context context, List<DisplayUsers> usersList) {
        this.context = context;
        this.usersList = usersList;
    }

    @NonNull
    @Override
    public UsersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.user_item, parent, false);
        return new UsersViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UsersViewHolder holder, int position) {
        DisplayUsers users = usersList.get(position);

        holder.usersName.setText(users.getUserName());
        holder.usersEmail.setText(users.getUserEmail());
        
        if(users.getImageUrl() != null && !users.getImageUrl().isEmpty()) {
        	Glide.with(context).load(users.getImageUrl()).into(holder.usersImage);
        } else {
            holder.usersImage.setImageResource(R.drawable.madhav);
        }
    }

    @Override
    public int getItemCount() {
        return usersList.size();
    }
    
    public static class UsersViewHolder extends RecyclerView.ViewHolder {
    	public TextView usersName, usersEmail;
        public ShapeableImageView usersImage;
        
        public UsersViewHolder(@NonNull View itemView) {
            super(itemView);
            
            usersName = itemView.findViewById(R.id.show_user_name);
            usersEmail = itemView.findViewById(R.id.show_user_email);
            usersImage = itemView.findViewById(R.id.show_users_image);
        }
    }
}
