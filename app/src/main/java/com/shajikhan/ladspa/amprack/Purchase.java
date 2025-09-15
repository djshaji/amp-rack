package com.shajikhan.ladspa.amprack;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.google.android.material.button.MaterialButton;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Purchase extends AppCompatActivity {
    Context context ;
    String TAG = getClass().getSimpleName();
    private PurchasesUpdatedListener purchasesUpdatedListener ;
    private BillingClient billingClient;
    HashMap<String, Button> productDetailsMap = new HashMap<>();
    HashMap<String, TextView> detailText = new HashMap<>();

    AcknowledgePurchaseResponseListener acknowledgePurchaseResponseListener ;
//    String PRODUCT_ID = "amprack_pro";
    public static String PRODUCT_ID = "amp_rack_pro";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchase2);
        TextView priceView = findViewById(R.id.price);
        context = this;

        Button premium = findViewById(R.id.premium);
        Button ultra = findViewById(R.id.ultra);
        Button price = findViewById(R.id.price);

        productDetailsMap.put("Amp Rack Pro (Amp Rack Guitar Effects Pedal)", price);
        productDetailsMap.put("Amp Rack PC Bundle (Amp Rack Guitar Effects Pedal)", premium);
        productDetailsMap.put("Amp Rack PC Bundle with Source Code (Amp Rack Guitar Effects Pedal)", ultra);

        detailText.put("Amp Rack Pro (Amp Rack Guitar Effects Pedal)", findViewById(R.id.pro_t));
        detailText.put("Amp Rack PC Bundle (Amp Rack Guitar Effects Pedal)", findViewById(R.id.premium_t));
        detailText.put("Amp Rack PC Bundle with Source Code (Amp Rack Guitar Effects Pedal)", findViewById(R.id.ultra_t));

        TextView oldPrice = findViewById(R.id.old_price);
        oldPrice.setPaintFlags(oldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        getSupportActionBar().hide();
        MainActivity.applyWallpaper(this, getWindow(),getResources(), findViewById(R.id.buy_bg), getWindowManager().getDefaultDisplay().getWidth(), getWindowManager().getDefaultDisplay().getHeight());
        acknowledgePurchaseResponseListener = new AcknowledgePurchaseResponseListener() {
            @Override
            public void onAcknowledgePurchaseResponse(@NonNull BillingResult billingResult) {
                Log.d(TAG, "onAcknowledgePurchaseResponse: " + billingResult.getDebugMessage());
            }
        };

        purchasesUpdatedListener = new PurchasesUpdatedListener() {

            @Override
            public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<com.android.billingclient.api.Purchase> list) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                        && list != null) {
                    for (com.android.billingclient.api.Purchase purchase : list) {
                        handlePurchase(purchase);
                    }
                } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                    // Handle an error caused by a user cancelling the purchase flow.
                } else {
                    // Handle any other error codes.
                }

            }
        };

        billingClient = BillingClient.newBuilder(context)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases()
                .build();

        MaterialButton materialButton = findViewById(R.id.buy);
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() ==  BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                    Log.d(TAG, "onBillingSetupFinished: billing client ready");
                    List<String> skuList = new ArrayList<>();
                    skuList.add(PRODUCT_ID);
                    skuList.add("amprack_bundle_source");
                    skuList.add("amprack_pc_bundle");
                    skuList.add("amprack_complete");

                    SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
                    params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);

                    billingClient.querySkuDetailsAsync(params.build(),
                            new SkuDetailsResponseListener() {
                                @Override
                                public void onSkuDetailsResponse(BillingResult billingResult,
                                                                 List<SkuDetails> skuDetailsList) {
                                    // Process the result.
                                    if (skuDetailsList == null) {
                                        Log.e(TAG, "onSkuDetailsResponse: details list null", null);
                                        return;
                                    }
                                    if (skuDetailsList.size() == 0) {
                                        Log.d(TAG, "onSkuDetailsResponse: empty list");
                                        return;
                                    }

                                    Log.d(TAG, "onSkuDetailsResponse: all products " + skuDetailsList);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            LinearLayout lt = findViewById(R.id.purchase_lt);
                                            for (SkuDetails details: skuDetailsList) {
                                                String price = details.getPrice();
                                                Log.d(TAG, "onSkuDetailsResponse: " + String.format("[%s] %s", details.getTitle(), price));

                                                LinearLayout ll = new LinearLayout(context);
                                                ll.setOrientation(LinearLayout.VERTICAL);
                                                LinearLayout.LayoutParams ltParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                                                ltParams.setMargins(0, 10, 0, 20);
                                                ll.setLayoutParams(ltParams);
                                                ll.setBackgroundColor(getResources().getColor(R.color.medium_orchid));
                                                Button textView = new Button(context);
                                                textView.setText("Tap to buy: " + price);
                                                textView.setBackgroundColor(getResources().getColor(R.color.dark_orchid));
                                                textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

                                                TextView det = new TextView(context);
                                                det.setLayoutParams(ltParams);
                                                det.setText(details.getDescription());
                                                det.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

                                                ll.addView(det);
                                                ll.addView(textView);
                                                lt.addView(ll);

                                                textView.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View view) {
                                                        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                                                                .setSkuDetails(details)
                                                                .build();
                                                        int responseCode = billingClient.launchBillingFlow((Activity) context, billingFlowParams).getResponseCode();
                                                        if (responseCode == BillingClient.BillingResponseCode.OK) {
                                                            Log.d(TAG, "onClick: billing screen launched ok");
                                                        } else {
                                                            Log.e(TAG, "onClick: unable to launch billing screen with " + responseCode, null);
                                                        }
                                                    }
                                                });

                                                if (details.getTitle().equals("Amp Rack Pro (Amp Rack Guitar Effects Pedal)")) {
                                                    priceView.setText(price);
                                                    materialButton.setOnClickListener(new View.OnClickListener() {
                                                        @Override
                                                        public void onClick(View view) {
                                                            BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                                                                    .setSkuDetails(details)
                                                                    .build();
                                                            int responseCode = billingClient.launchBillingFlow((Activity) context, billingFlowParams).getResponseCode();
                                                            if (responseCode == BillingClient.BillingResponseCode.OK) {
                                                                Log.d(TAG, "onClick: billing screen launched ok");
                                                            } else {
                                                                Log.e(TAG, "onClick: unable to launch billing screen with " + responseCode, null);
                                                            }
                                                        }
                                                    });
                                                }
                                            }
                                        }
                                    });
                                }
                            });
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                Log.e(TAG, "onBillingServiceDisconnected: billing client disconnected", null);
            }
        });

        Button source = findViewById(R.id.source);
        source.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = "https://github.com/djshaji/amp-rack";
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            }
        });
    }

    private void handlePurchase(com.android.billingclient.api.Purchase purchase) {
        if (purchase.getPurchaseState() == com.android.billingclient.api.Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();
                billingClient.acknowledgePurchase(acknowledgePurchaseParams, acknowledgePurchaseResponseListener);
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage("Thank you for supporting the app!")
                        .setTitle("Purchase Successful")
                        .setIcon(R.drawable.logo)
                        .setPositiveButton("You're Welcome!", null);

                AlertDialog dialog = builder.create();
                dialog.show();
                PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("pro", true);
            } else {
                Log.d(TAG, "handlePurchase: purchase already acknowledged");
            }
        }

    }

    void applyWallpaper () {
        SharedPreferences defaultSharedPreferences =  PreferenceManager.getDefaultSharedPreferences(this);
        String resIdString = defaultSharedPreferences.getString("background", "Space");
        Bitmap bitmap = null ;
        switch (resIdString) {
            case "Space":
            default:
                bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bg) ;
                break ;
            case "Water":
                bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.water) ;
                break ;
            case "Fire":
                bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.fire) ;
                break ;
            case "Sky":
                bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.sky) ;
                break ;
            case "Earth":
                bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bg_earth) ;
                break ;
        }

        if (bitmap == null) {
            Log.e(TAG, "applyWallpaper: No suitable bg from settings", null);
            return;
        }

        ImageView imageView = findViewById(R.id.buy_bg);
        imageView.setCropToPadding(true);
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        int width = getWindowManager().getDefaultDisplay().getWidth();
        int height = getWindowManager().getDefaultDisplay().getHeight();
        bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
        imageView.setImageBitmap(bitmap);
    }
}