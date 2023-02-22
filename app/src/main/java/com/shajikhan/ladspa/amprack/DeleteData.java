package com.shajikhan.ladspa.amprack;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Map;

public class DeleteData extends Activity {

    String TAG = getClass().getSimpleName();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout linearLayout = new LinearLayout(this);
        setContentView(linearLayout);
        TextView textView = new TextView(this);
        textView.setText("Delete Account");
        textView.setTextColor(getResources().getColor(R.color.indian_red));
        textView.setTextSize(20);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(10,10,10,10);
//        linearLayout.setLayoutParams(layoutParams);
        textView.setLayoutParams(layoutParams);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(textView);

        TextView details = new TextView(this);
        details.setLayoutParams(layoutParams);
        details.setTextColor(getResources().getColor(R.color.white));
        LinearLayout.LayoutParams layoutParams1 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams1.setMargins(10,30,10,10);
        details.setText(R.string.delete_warn);
        linearLayout.addView(details);

        Button materialButton = new Button(this);
        linearLayout.addView(materialButton);
        materialButton.setBackgroundColor(getResources().getColor(R.color.indian_red));
        materialButton.setTextColor(getResources().getColor(R.color.white));
        materialButton.setLayoutParams(layoutParams1);
        materialButton.setText("Delete My Account");
        materialButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth auth = FirebaseAuth.getInstance() ;
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                assert db != null ;

                assert auth != null ;
                String uid = auth.getUid();
                OnCompleteListener onCompleteListener = new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        WriteBatch batch = db.batch();

                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Map preset = (Map) document.getData();
                                batch.delete(db.collection("presets").document(document.getId()));
                            }

                            batch.delete(db.collection("collections").document(uid));
                            batch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                    assert user != null ;
//                                    auth.signOut();
                                    user.delete()
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        Log.d(TAG, "User account deleted.");
                                                        MainActivity.alert("Account deleted", "Your account has been deleted.");
                                                    }
                                                }
                                            });
                                }
                            });

                        }
                    }
                };

                db.collection("presets")
                        .whereEqualTo("uid", uid)
                        .get()
                        .addOnCompleteListener(onCompleteListener);


            }
        });
    }
}