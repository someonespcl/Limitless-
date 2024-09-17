package com.limitless.utils;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.limitless.R;

public class CustomToast {
    
    public static void showToast(Context context, String message) {
        
        LayoutInflater inflator = LayoutInflater.from(context);
        View layout = inflator.inflate(R.layout.toast_layout, null);
        
        TextView toastMessage = layout.findViewById(R.id.toast_message);
        
        toastMessage.setText(message);
        
        Toast toast = new Toast(context);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        toast.show();
    }
}
