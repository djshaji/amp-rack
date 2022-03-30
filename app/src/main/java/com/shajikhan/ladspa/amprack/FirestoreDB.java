package com.shajikhan.ladspa.amprack;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Keep
public class FirestoreDB {
    String TAG = getClass().getSimpleName() ;
        private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(2, 4,
            60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    private FirebaseFirestore db;
    Context context ;
    MainActivity mainActivity;

    FirestoreDB (MainActivity _mainActivity) {
        db = FirebaseFirestore.getInstance();
        mainActivity = _mainActivity ;
        context = mainActivity.getApplicationContext();
    }

    FirestoreDB (Context _context) {
        db = FirebaseFirestore.getInstance();
        context = _context ;
    }

    @ServerTimestamp
    public void savePreset (String name, String desc, boolean shared, Map values) {
        FirebaseAuth auth = FirebaseAuth.getInstance() ;
        if (auth == null) {
            Log.e(TAG, "savePreset: uid is null", null);
            return ;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put ("desc", desc);

        data.put ("public", shared);
        data.put ("controls", values);

        data.put ("uid", auth.getUid());
        data.put ("timestamp",  FieldValue.serverTimestamp());

        db.collection("presets")
                .add(data)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "DocumentSnapshot written with ID: " + documentReference.getId());
                        Toast.makeText(context,
                                "Preset saved successfully",
                                Toast.LENGTH_LONG)
                                .show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding document", e);
                        Toast.makeText(context,
                                "Could not save preset: " + e.getMessage(),
                                Toast.LENGTH_LONG)
                                .show();
                    }
                });
    }

    public void loadUserPresets (MyPresetsAdapter presetsAdapter) {
        FirebaseAuth auth = FirebaseAuth.getInstance() ;
        if (auth == null) {
            Log.e(TAG, "savePreset: uid is null", null);
            return ;
        }

        String uid = auth.getUid();
        db.collection("presets")
                .whereEqualTo("uid", uid)
                .get ()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    Log.d(TAG, document.getId() + " => " + document.getData());
                                    Map preset = (Map) document.getData();
                                    preset.put("path", document.getReference().getPath());
                                    presetsAdapter.addPreset(preset);
                                }

                                if (presetsAdapter.progressBar != null) {
                                    presetsAdapter.progressBar.setVisibility(View.GONE);
                                }
                            } else {
                                Log.d(TAG, "Error getting documents: ", task.getException());
                                MainActivity.toast("Error getting presets: " + task.getException().getMessage());
                            }
                        }
                    });

    }

    void deletePreset(Map preset, ArrayList<Map> presets, MyPresetsAdapter myPresetsAdapter, int position) {
        db.document(preset.get("path").toString())
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Preset successfully deleted");
                        MainActivity.toast("Preset successfully deleted");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error deleting Preset", e);
                        MainActivity.toast("Preset successfully deleted");
                        presets.remove(position);
                        myPresetsAdapter.notifyItemRemoved(position);
                    }
                });

    }
}
