package com.shajikhan.ladspa.amprack;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.ServerTimestamp;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.firestore.v1.WriteResult;

import org.json.JSONObject;

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
    int loaded = 0 ;
    DocumentSnapshot last = null;
    String lastStamp = "" ;
    Task<QuerySnapshot> cachedTask = null ;
    QuerySnapshot cachedSnapshot = null ;

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
    public void savePreset(String name, String desc, boolean shared, Map values, AlertDialog dialog, MyPresets myPresets, String path) {
        FirebaseAuth auth = FirebaseAuth.getInstance() ;
        if (auth == null) {
            Log.e(TAG, "savePreset: uid is null", null);
            return ;
        } else if (auth.getUid() == null) {
            Log.e(TAG, "loadUserPresets: uid is null", null);
            return ;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put ("desc", desc);
        data.put("likes", 0);

        data.put ("public", shared);
        data.put ("controls", values);

        data.put ("uid", auth.getUid());
        data.put ("timestamp",  FieldValue.serverTimestamp());

        OnSuccessListener<DocumentReference> successListener = new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {
//                        Log.d(TAG, "DocumentSnapshot written with ID: " + documentReference.getId());
                dialog.dismiss();
                Toast.makeText(context,
                                "Patch saved successfully",
                                Toast.LENGTH_LONG)
                        .show();
                data.put("path", documentReference.getPath());
                data.put("uid", auth.getUid());
                ((MainActivity) context).lastPresetLoadedPath = documentReference.getPath().split("presets")[1];
                ((MainActivity) context).lastPresetLoadedUID = auth.getUid();
                myPresets.myPresetsAdapter.addPreset(data);
            }
        };

        OnFailureListener failureListener = new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.w(TAG, "Error adding document", e);
                Toast.makeText(context,
                                "Could not save patch: " + e.getMessage(),
                                Toast.LENGTH_LONG)
                        .show();
            }
        };

        if (path != null) {
            db.collection("presets")
                    .document(path)
                    .set(data)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Toast.makeText(context, "Preset updated successfully", Toast.LENGTH_SHORT).show();
                            myPresets.myPresetsAdapter.removePreset(data);
                            myPresets.myPresetsAdapter.addPreset(data);
                            dialog.dismiss();
                        }
                    })
                    .addOnFailureListener(failureListener);
        } else {
            db.collection("presets")
                    .add(data)
                    .addOnSuccessListener(successListener)
                    .addOnFailureListener(failureListener);
        }
    }

    public void loadUserPresets (MyPresetsAdapter presetsAdapter, boolean shared, boolean quick) {
        if (presetsAdapter.progressBar != null) {
            presetsAdapter.progressBar.setVisibility(View.VISIBLE);
        }

//        if (presetsAdapter.loadProgress != null) {
//            presetsAdapter.loadProgress.setVisibility(View.VISIBLE);
//        }

        if (quick && mainActivity.rack.quickPatchProgress != null)
            mainActivity.rack.quickPatchProgress.setVisibility(View.GONE);

        FirebaseAuth auth = FirebaseAuth.getInstance() ;
        if (auth == null && !quick) {
            Log.e(TAG, "loadUserPresets: uid is null", null);
            return ;
        } else if (auth.getUid() == null && !quick) {
            Log.e(TAG, "loadUserPresets: uid is null", null);
            return ;
        }
//        else
//            Log.d(TAG, "loadUserPresets: [uid] "+ auth.getUid());

        String uid = auth.getUid();
        OnCompleteListener onCompleteListener = new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    cachedTask = task ;
                    loaded = loaded + 30 ;
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        last = document ;
//                        Log.d(TAG, document.getId() + " => " + document.getData());
                        Map preset = (Map) document.getData();
//                        Log.d(TAG, "onComplete: " + String.format("%s | %s", uid, preset.get("uid")));
//                        Log.d(TAG, "onComplete: " + String.format(
//                                "preset: %s",
//                                preset.toString()
//                        ));
                        if (preset.get("uid") .equals(uid) && shared == true)
                            continue;

                        if (shared && preset.get("controls").toString().equals("{}")) {
                            Log.d(TAG, "onComplete: skipping empty preset " + preset.get("name"));
                            continue;
                        }

                        preset.put("path", document.getReference().getPath());

                        if (shared && presetsAdapter.contains(preset)) {
                            Log.d(TAG, String.format ("[skip duplicate preset]: %s", preset));
                            continue;
                        }

//                        Log.d(TAG, String.format ("[preset]: %s", preset));
                        presetsAdapter.addPreset(preset);
//                        lastStamp = (String) preset.get(presetsAdapter.sortBy);
                    }

                    if (presetsAdapter.progressBar != null) {
                        presetsAdapter.progressBar.setVisibility(View.GONE);
                    }
                } else {
                    Log.d(TAG, "Error getting documents: ", task.getException());
                    MainActivity.toast("Error getting presets: " + task.getException().getMessage());
                }

//                if (presetsAdapter.loadProgress != null) {
//                    presetsAdapter.loadProgress.setVisibility(View.INVISIBLE);
//                }
            }
        } ;

        OnSuccessListener onSuccessListener = new OnSuccessListener() {
            @Override
            public void onSuccess(Object o) {
                cachedSnapshot = (QuerySnapshot) o;
            }
        };

        Query query = null ;

        if (shared == false && quick == false) {
            Log.d(TAG, "loadUserPresets: user presets");
            query = db.collection("presets")
                    .whereEqualTo("uid", uid)
                    .limit(30)
                    .orderBy(presetsAdapter.sortBy, Query.Direction.DESCENDING);
            if (last != null)
                    query = query.startAfter(last);

            query.get()
            .addOnSuccessListener(onSuccessListener)
            .addOnCompleteListener(onCompleteListener);
        } else if (shared){
            Log.d(TAG, "loadUserPresets: shared presets");
            query = db.collection("presets")
                    .whereEqualTo("public", true)
                    .limit(30)
                    .orderBy(presetsAdapter.sortBy, Query.Direction.DESCENDING);
            if (last != null)
                    query = query.startAfter(last);

            query.get()
            .addOnSuccessListener(onSuccessListener)
            .addOnCompleteListener(onCompleteListener);
        } else if (quick){
            Log.d(TAG, "loadUserPresets: quick patches");

            if (cachedTask != null) {
                for (QueryDocumentSnapshot document : cachedTask.getResult()) {
                    Log.d(TAG, "loadUserPresets: using cached quick presets");
                    Log.d(TAG, document.getId() + " => " + document.getData());
                    Map preset = (Map) document.getData();
//                        Log.d(TAG, "onComplete: " + String.format("%s | %s", uid, preset.get("uid")));
                    /*
                    Log.d(TAG, "onComplete: " + String.format(
                            "preset: %s",
                            preset.toString()
                    ));

                     */
                    if (preset.get("uid") .equals(uid) && shared == true)
                        continue;

                    if (shared && preset.get("controls").toString().equals("{}")) {
                        Log.d(TAG, "onComplete: skipping empty preset " + preset.get("name"));
                        continue;
                    }
                    preset.put("path", document.getReference().getPath());
                    presetsAdapter.addPreset(preset);
                }
            }
            else {
                db.collection("presets")
                        .whereEqualTo("uid", "lWDjT6ENhgV9Hs6JHIjFAcacpAo1")
                        .orderBy(presetsAdapter.sortBy, Query.Direction.DESCENDING)
                        .get()
                        .addOnCompleteListener(onCompleteListener);
            }
        }
    }

    void deletePreset(Map preset, ArrayList<Map> presets, MyPresetsAdapter myPresetsAdapter, int position) {
        String __path__ = (String) preset.get("path");
        if (__path__ == null) {
            Log.e(TAG, "deletePreset: preset path is null");
            return;
        }

        db.document(__path__)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Preset successfully deleted");
                        MainActivity.toast("Preset successfully deleted");
                        presets.remove(position);
                        myPresetsAdapter.notifyItemRemoved(position);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error deleting Preset", e);
                        MainActivity.toast("Preset could not be deleted. Try again: " + e.getMessage());
                    }
                });

    }

    boolean checkAuth () {
        FirebaseAuth auth = FirebaseAuth.getInstance() ;
        if (auth == null) {
            return false ;
        } else if (auth.getUid() == null) {
            return false ;
        } else
            return true ;
    }

    void addPresetToCollection (String collection, Map preset) {
        if (! checkAuth())
            return;

        String uid = FirebaseAuth.getInstance().getUid() ;
        Map<String, Object> data = new HashMap<>();
        data.put(preset.get ("path").toString(), preset.get("name"));

        data.put ("uid", uid);
        data.put ("timestamp",  FieldValue.serverTimestamp());

        Log.d(TAG, "addPresetToCollection: writing to " + String.format("%s/%s", collection, uid));
        db.collection(collection).document(uid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        MainActivity.toast("Preset added to favourites");
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

    void likePreset (Map preset) {
        DocumentReference documentReference = db.document(preset.get("path").toString());
        documentReference.update("likes", FieldValue.increment(1))
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "onSuccess: preset liked!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "onFailure: failed to like preset", e);
                    }
                });

    }

    void addAndLike (Map preset) {
        if (! checkAuth())
            return;

        String uid = FirebaseAuth.getInstance().getUid() ;
        Map<String, Object> data = new HashMap<>();
        data.put(preset.get ("path").toString(), preset.get("name"));

        data.put ("uid", uid);
        data.put ("timestamp",  FieldValue.serverTimestamp());

        WriteBatch batch = db.batch();
        DocumentReference favs = db.collection("collections").document(uid) ;
        batch.set(favs, data, SetOptions.merge());
        DocumentReference documentReference = db.document(preset.get("path").toString());
        batch.update(documentReference, "likes", FieldValue.increment(1));

        batch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                MainActivity.toast("Patch added to favorites");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                MainActivity.toast("Patch not added to favorites: " + e.getMessage());
                Log.e(TAG, "onFailure: not saved preset", e);
            }
        });
    }

    void removeAndUnlike (Map preset) {
        if (! checkAuth())
            return;

        String uid = FirebaseAuth.getInstance().getUid() ;

        WriteBatch batch = db.batch();
        DocumentReference favs = db.collection("collections").document(uid) ;
        batch.update(favs, FieldPath.of (preset.get("path").toString()), FieldValue.delete());
        DocumentReference documentReference = db.document(preset.get("path").toString());
        batch.update(documentReference, "likes", FieldValue.increment(-1));

        batch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Log.d(TAG, "onComplete: patch " + preset.get("name") + " removed from favorites");
                MainActivity.toast("Patch removed from favorites");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                MainActivity.toast("Patch not removed from favorites: " + e.getMessage());
                Log.e(TAG, "onFailure: not saved preset", e);
            }
        });
    }

    void getFavorites (MyPresetsAdapter presetsAdapter, boolean shared, boolean quick) {
        if (! checkAuth() && !quick)
            return;

        String uid = FirebaseAuth.getInstance().getUid() ;
        if (uid == null)
                uid = "lWDjT6ENhgV9Hs6JHIjFAcacpAo1";
        DocumentReference documentReference = db.collection("collections").document(uid);
        documentReference
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        presetsAdapter.favoritePresets = documentSnapshot.getData();
                        if (presetsAdapter.favoritePresets == null) {
                            presetsAdapter.favoritePresets = new HashMap<String, Object>();
                        }
                        loadUserPresets(presetsAdapter, shared, quick);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        loadUserPresets(presetsAdapter, shared, quick);
                    }
                });
    }
}
