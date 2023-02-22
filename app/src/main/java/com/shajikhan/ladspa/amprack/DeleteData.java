package com.shajikhan.ladspa.amprack;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
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
    int toDelete = 0 ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout parent = new LinearLayout(this);
        setContentView(parent);

        FirebaseAuth auth = FirebaseAuth.getInstance() ;
        assert auth != null ;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        assert db != null ;
        String uid = auth.getUid();

        DocumentReference fav = db.collection("collections").document(uid);

        LinearLayout linearLayout = new LinearLayout(this);
        parent.addView(linearLayout);

        linearLayout.setBackgroundResource(R.drawable.rounded_corners_transparent);
        parent.setBackgroundResource(R.drawable.bg);
        TextView textView = new TextView(this);
        textView.setText("Delete Account");
        textView.setTextColor(getResources().getColor(R.color.white));
        textView.setTextSize(20);

        textView.setFontFeatureSettings("bold");
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(10,10,10,10);
//        linearLayout.setLayoutParams(layoutParams);
        textView.setLayoutParams(layoutParams);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(textView);
//        textView.setBackgroundColor(getResources().getColor(R.color.white));
//        textView.setBackgroundResource(R.drawable.semi_trans);
        linearLayout.setGravity(Gravity.CENTER);
        linearLayout.setLayoutParams(layoutParams);

        TextView info = new TextView(getApplicationContext());
        info.setLayoutParams(layoutParams);
        info.setText(String.format(
                "Email: %s\nUser ID: %s",
                auth.getCurrentUser().getEmail(),
                auth.getUid()
        ));

        linearLayout.addView(info);
        info.setTextColor(getResources().getColor(R.color.white));

        TextView details = new TextView(this);
        details.setLayoutParams(layoutParams);
        details.setTextColor(getResources().getColor(R.color.white));
        LinearLayout.LayoutParams layoutParams1 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams1.setMargins(30,30,30,30);
        details.setText(R.string.delete_warn);
        linearLayout.addView(details);

        Button materialButton = new Button(this);
        linearLayout.addView(materialButton);
        materialButton.setPadding(30,30,30,30);
        materialButton.setBackgroundColor(getResources().getColor(R.color.indian_red));
        materialButton.setTextColor(getResources().getColor(R.color.white));
        materialButton.setLayoutParams(layoutParams1);
        materialButton.setText("Delete My Account");

        ProgressBar progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);
        progressBar.setLayoutParams(layoutParams1);
        WriteBatch batch = db.batch();

        linearLayout.addView(progressBar);
        materialButton.setEnabled(false);

        fav.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                batch.delete(documentSnapshot.getReference());
                Log.d(TAG, "onSuccess: fav id" + fav.getId());
                progressBar.setVisibility(View.GONE);
                toDelete ++ ;
                materialButton.setEnabled(true);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                materialButton.setEnabled(true);
                Log.d(TAG, "onFailure: " + e.getMessage());
                progressBar.setVisibility(View.GONE);
            }
        });

        materialButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                OnCompleteListener onCompleteListener = new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Map preset = (Map) document.getData();
                                toDelete ++;
                                Log.d(TAG, "onComplete: deleting " + document.getId());
                                batch.delete(db.collection("presets").document(document.getId()));
                            }

                            Log.d(TAG, "onComplete: delete is " + toDelete);
                            if (toDelete == 0) {
                                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                assert user != null ;
                                user.delete()
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    Log.d(TAG, "User account deleted.");
                                                    progressBar.setVisibility(View.GONE);
                                                    details.setText("Your account has been deleted.");
                                                    materialButton.setVisibility(View.GONE);
                                                    MainActivity.alert("Account deleted", "Your account has been deleted.");
                                                }
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.e(TAG, "onFailure: ", e);
                                                details.setText(e.getMessage());
                                                progressBar.setVisibility(View.GONE);
                                            }
                                        });
                                return;
                            }

                            Log.d(TAG, "onComplete: " + batch.toString());
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
                                                        progressBar.setVisibility(View.GONE);
                                                        details.setText("Your account has been deleted.");
                                                        materialButton.setVisibility(View.GONE);
                                                        MainActivity.alert("Account deleted", "Your account has been deleted.");
                                                    }
                                                }
                                            });
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    details.setText(String.format(
                                            "Unable to delete account:\n\n%s", e.getMessage()
                                    ));
                                    progressBar.setVisibility(View.GONE);
                                }
                            });

                        } else {
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            assert user != null ;
                            user.delete()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Log.d(TAG, "User account deleted.");
                                                progressBar.setVisibility(View.GONE);
                                                details.setText("Your account has been deleted.");
                                                materialButton.setVisibility(View.GONE);
                                                MainActivity.alert("Account deleted", "Your account has been deleted.");
                                            }
                                        }
                                    });
                            return;
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