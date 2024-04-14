package com.shajikhan.ladspa.amprack;

import static android.app.Activity.RESULT_OK;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.tabs.TabItem;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public class Presets extends Fragment {
    View view ;
    ViewPager2 viewPager;
    LinearLayout loginNotice ;
    TabLayout tabLayout ;
    PresetAdapter fragmentStateAdapter ;
    private static final String TAG = "GoogleActivity";
    private static final int RC_SIGN_IN = 9001;
    MainActivity mainActivity;
    FirestoreDB db ;
    ProgressBar progressPreset ;
    LinearLayout progressLayout ;
    // [START declare_auth]
    private FirebaseAuth mAuth;
    // [END declare_auth]

    private GoogleSignInClient mGoogleSignInClient;
    // See: https://developer.android.com/training/basics/intents/result
    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new FirebaseAuthUIActivityResultContract(),
            new ActivityResultCallback<FirebaseAuthUIAuthenticationResult>() {
                @Override
                public void onActivityResult(FirebaseAuthUIAuthenticationResult result) {
                    onSignInResult(result);
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//        setRetainInstance(true);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("983863263684-6ggjm8spjvvftm5noqtpl97v0le5laft.apps.googleusercontent.com")
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(getContext(), gso);
        mGoogleSignInClient.signOut();
        mAuth = FirebaseAuth.getInstance();

        Log.d(TAG, "onCreateView: view created");
        view = inflater.inflate(R.layout.activity_presets,null) ;
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mainActivity = (MainActivity) getActivity();
        db = new FirestoreDB (getContext());

        fragmentStateAdapter = new PresetAdapter(this);
        ConstraintLayout constraintLayout = (ConstraintLayout) view  ;
        loginNotice = (LinearLayout) constraintLayout.getChildAt(4);
        tabLayout = (TabLayout) constraintLayout.getChildAt(0);
        progressLayout = (LinearLayout) constraintLayout.getChildAt(2);
        progressPreset = (ProgressBar) progressLayout.getChildAt(0);
        viewPager = view.findViewById(R.id.presets_pager);

        viewPager.setSaveEnabled(false);
        fragmentStateAdapter.createFragment(0);
        if (! mainActivity.tabletMode)
            fragmentStateAdapter.createFragment(1);
        else {
            fragmentStateAdapter.libraryPresets = new MyPresets(true);
            fragmentStateAdapter.libraryPresets.mainActivity = mainActivity ;
            fragmentStateAdapter.libraryPresets.progressBar = progressPreset;

            LinearLayout ll = (LinearLayout) fragmentStateAdapter.libraryPresets.onCreateView(mainActivity.getLayoutInflater(), null, null);
            fragmentStateAdapter.libraryPresets.onViewCreated(ll, null);

            ConstraintLayout.LayoutParams layoutParams = new ConstraintLayout.LayoutParams(mainActivity.deviceWidth/2, ViewGroup.LayoutParams.WRAP_CONTENT);
            ll.setLayoutParams(layoutParams);
            viewPager.setLayoutParams(layoutParams);

            ConstraintLayout root = mainActivity.findViewById(R.id.presets_constraint);
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(constraintLayout);
            constraintSet.connect(R.id.presets_library_layout, ConstraintSet.LEFT,R.id.presets_pager,ConstraintSet.RIGHT,0);
            constraintSet.connect(R.id.presets_pager, ConstraintSet.TOP,R.id.my_preset_tab_layout,ConstraintSet.BOTTOM,0);

            constraintSet.applyTo(constraintLayout);

//            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(mainActivity.deviceWidth/2,mainActivity.deviceHeight, 1.0f);
            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(mainActivity.deviceWidth/2, ViewGroup.LayoutParams.WRAP_CONTENT);

            LinearLayout libLayout = view.findViewById(R.id.presets_library_layout);
            libLayout.addView(ll);
        }

        viewPager.setAdapter(fragmentStateAdapter);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (! mainActivity.tabletMode)
                    viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        MaterialButton login = view.findViewById(R.id.login_btn);
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*
                Intent intent = new Intent(getContext(), FirebaseUI.class);
                startActivity(intent);

                 */
                signIn();
            }
        });
        if (mAuth.getCurrentUser() == null) {
            Log.d(TAG, "onCreateView: no user logged in");
            loginNotice.setVisibility(View.VISIBLE);
            tabLayout.setVisibility(View.GONE);
        } else {
            Log.d(TAG, "onCreateView: user logged in: " + mAuth.getCurrentUser().getEmail());
            loginNotice.setVisibility(View.GONE);
            tabLayout.setVisibility(View.VISIBLE);
            progressLayout.setVisibility(View.VISIBLE);
        }

        LinearLayout layout = (LinearLayout) constraintLayout.getChildAt(3);
        ExtendedFloatingActionButton presetFab = (ExtendedFloatingActionButton) layout.getChildAt(0);
        presetFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: showing preset");
                Map preset ;
                preset = mainActivity.presetToMap();

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                // Get the layout inflater
                LayoutInflater inflater = requireActivity().getLayoutInflater();

                // Inflate and set the layout for the dialog
                // Pass null as the parent view because its going in the dialog layout
                LinearLayout linearLayout = (LinearLayout) inflater.inflate(R.layout.save_preset_dialog, null);
                builder.setView(linearLayout)
                        // Add action buttons
                        .setCancelable(false);

                LinearLayout linearLayout1 = (LinearLayout) linearLayout.getChildAt(3);
                MaterialButton save = (MaterialButton) linearLayout1.getChildAt(1);
                ProgressBar progressBar = (ProgressBar) linearLayout.getChildAt(4);
                AlertDialog dialog = builder.create() ;
                save.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        EditText name = (EditText) linearLayout.getChildAt(1);
                        EditText desc = (EditText) linearLayout.getChildAt(2);
                        LinearLayout layout = (LinearLayout) linearLayout.getChildAt(0);
                        SwitchMaterial switchMaterial = (SwitchMaterial) layout.getChildAt(1);

                        if (name.getText().toString().length() == 0) {
                            mainActivity.toast("Name of preset is required.");
                            return;
                        }

                        progressBar.setVisibility(View.VISIBLE);
                        db.savePreset(name.getText().toString(), desc.getText().toString(), switchMaterial.isChecked(), preset, dialog, fragmentStateAdapter.getMyPresets());

                    }
                });

                MaterialButton cancel = (MaterialButton) linearLayout1.getChildAt(0);
                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss ();
                    }
                });

                dialog.show();

            }
        });

        if (mainActivity.useTheme) {
            mainActivity.skinEngine.fab(presetFab, SkinEngine.Resize.Width,1);
        }
    }

    public class PresetAdapter extends FragmentStateAdapter {
        public ArrayList<Fragment> arrayList = new ArrayList<>();
        public MyPresets libraryPresets ;
        public MyPresets myPresets;

        public MyPresets getMyPresets() {
            return myPresets;
        }

        public PresetAdapter(@NonNull Presets fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                myPresets = new MyPresets(progressPreset);
                arrayList.add(myPresets);
                return myPresets;
            } else {// if (position == 1) {
                libraryPresets = new MyPresets(true);
                libraryPresets.progressBar = progressPreset;
                arrayList.add(libraryPresets);
                return libraryPresets;
            }

//            Log.e(TAG, "createFragment: unknown position " + position, null);
//            return null ;
        }

        @Override
        public int getItemCount() {
            return 2;
        }


    }

    public static class PresetFragment extends Fragment {
        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.my_presets, container, false);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e);
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener((Executor) this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            updateUI(null);
                        }
                    }
                });
    }

    private void signIn() {
//        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
//        startActivityForResult(signInIntent, RC_SIGN_IN);
        // Choose authentication providers
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
//                new AuthUI.IdpConfig.PhoneBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build());
//                new AuthUI.IdpConfig.FacebookBuilder().build(),
//                new AuthUI.IdpConfig.TwitterBuilder().build());

        // Create and launch sign-in intent
        Intent signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setAlwaysShowSignInMethodScreen(true)
                .setIsSmartLockEnabled(false)
                .setTheme(R.style.Theme_AmpRack)
                .build();
        signInLauncher.launch(signInIntent);
    }

    private void updateUI(FirebaseUser user) {
        fragmentStateAdapter.myPresets.db.loadUserPresets(fragmentStateAdapter.myPresets.myPresetsAdapter, false, false);
    }
    private void onSignInResult(FirebaseAuthUIAuthenticationResult result) {
        IdpResponse response = result.getIdpResponse();
        if (result.getResultCode() == RESULT_OK) {
            // Successfully signed in
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            Log.d(TAG, "onSignInResult: user logged in: " + user.getEmail());
            Toast.makeText(getContext(),
                    "Logged in as " + user.getEmail(),
                    Toast.LENGTH_LONG)
                    .show();
            loginNotice.setVisibility(View.GONE);
            tabLayout.setVisibility(View.VISIBLE);
            progressPreset.setVisibility(View.VISIBLE);
            updateUI(user);
            // ...
        } else {
            // Sign in failed. If response is null the user canceled the
            // sign-in flow using the back button. Otherwise check
            // response.getError().getErrorCode() and handle the error.
            // ...
            Toast.makeText(getContext(),
                    "Login was unsuccessful. Try again.",
                    Toast.LENGTH_LONG)
                    .show();


            if (response == null) {
                Log.d(TAG, "onSignInResult: user cancelled the dialog");
            } else {
                Log.e(TAG, "onSignInResult: " +response.getError().getErrorCode(), null);
            }
        }
    }

    void setupUI () {
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            Log.d(TAG, "onCreateView: no user logged in");
            loginNotice.setVisibility(View.VISIBLE);
            tabLayout.setVisibility(View.GONE);
        } else {
            Log.d(TAG, "onCreateView: user logged in: " + mAuth.getCurrentUser().getEmail());
            loginNotice.setVisibility(View.GONE);
            tabLayout.setVisibility(View.VISIBLE);
            progressPreset.setVisibility(View.VISIBLE);
        }
    }
}
