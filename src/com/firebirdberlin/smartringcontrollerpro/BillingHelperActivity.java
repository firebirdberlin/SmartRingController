package com.firebirdberlin.smartringcontrollerpro;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.android.vending.billing.IInAppBillingService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BillingHelperActivity extends Activity {
    static final String TAG = "BillingActivity";

    public interface ItemPurchaseListener {
        void onItemPurchased(String sku);
    }

    public static final int REQUEST_CODE_PURCHASE_DONATION = 1001;
    public static final int REQUEST_CODE_PURCHASE_PRO = 1002;
    private static final int PRODUCT_ID_DONATION = 1;
    private static final int PRODUCT_ID_PRO = 2;

    IInAppBillingService mService;
    Map<String, Boolean> purchases;
    BillingHelper billingHelper;
    ItemPurchaseListener itemPurchaseListener = null;

    ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "IIAB service disconnected");
            billingHelper = null;
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "IIAB service connected");
            mService = IInAppBillingService.Stub.asInterface(service);
            billingHelper = new BillingHelper(getApplicationContext(), mService);
            updateAllPurchases();
            onPurchasesInitialized();
        }
    };

    public boolean isPurchased(String sku) {
        Log.i(TAG, "Checking purchase " + sku);
        /*
        if (Utility.isDebuggable(this)) {
            return true;
        }
        */
        if (billingHelper == null) {
            return false;
        }
        Log.i(TAG, " => " + String.valueOf(billingHelper.isPurchased(sku)));
        return billingHelper.isPurchased(sku);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // bind the in-app billing service
        Intent serviceIntent =
                new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateAllPurchases();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConn);
    }

    public void showPurchaseDialog(ItemPurchaseListener listener) {
        itemPurchaseListener = listener;
        Log.i(TAG, "showPurchaseDialog()");
        if (isPurchased(BillingHelper.ITEM_DONATION)) return;
        List<CharSequence> entries = new ArrayList<>();
        final List<Integer> values = new ArrayList<>();
        HashMap<String, String> prices = getPrices();

        boolean purchased_pro = isPurchased(BillingHelper.ITEM_PRO);
        boolean purchased_donation = isPurchased(BillingHelper.ITEM_DONATION);

        if (!purchased_pro) {
            entries.add(
                    getProductWithPrice(prices, R.string.product_name_pro, BillingHelper.ITEM_PRO)
            );
            values.add(PRODUCT_ID_PRO);
        }

        if (!purchased_donation) {
            entries.add(
                    getProductWithPrice(prices, R.string.product_name_donation, BillingHelper.ITEM_DONATION)
            );
            values.add(PRODUCT_ID_DONATION);
        }

        //new AlertDialog.Builder(this, R.style.DialogTheme)
        new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.buy))
                .setItems(
                        entries.toArray(new CharSequence[entries.size()]),
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialogInterface, int which) {
                                Log.i(TAG, String.format("selected %d", which));
                                int selected = values.get(which);
                                switch (selected) {
                                    case PRODUCT_ID_DONATION:
                                        purchaseIntent(BillingHelper.ITEM_DONATION, REQUEST_CODE_PURCHASE_DONATION);
                                        break;
                                    case PRODUCT_ID_PRO:
                                        purchaseIntent(BillingHelper.ITEM_PRO, REQUEST_CODE_PURCHASE_PRO);
                                        break;
                                }
                            }
                        })
                .setNeutralButton(android.R.string.cancel, null)
                .show();
    }

    public void purchaseIntent(String sku, int REQUEST_CODE) {
        if (mService == null) return;
        try {
            String developerPayload = "abcdefghijklmnopqrstuvwxyz";
            Bundle buyIntentBundle = mService.getBuyIntent(
                    3, getPackageName(),
                    sku, "inapp", developerPayload
            );
            PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
            startIntentSenderForResult(pendingIntent.getIntentSender(),
                    REQUEST_CODE, new Intent(), 0, 0, 0);
        } catch (RemoteException | IntentSender.SendIntentException | NullPointerException ignored) {
        }
    }

    private String getProductWithPrice(HashMap<String, String> prices, int resId, String sku) {
        String price = prices.get(sku);
        if (price != null) {
            return String.format("%s (%s)", getResources().getString(resId), price);
        }
        return getResources().getString(resId);
    }

    private HashMap<String, String> getPrices() {
        HashMap<String, String> map = new HashMap<>();
        if (mService == null) return map;

        ArrayList<String> skuList = new ArrayList<String>();
        skuList.add(BillingHelper.ITEM_DONATION);
        skuList.add(BillingHelper.ITEM_PRO);
        Bundle querySkus = new Bundle();
        querySkus.putStringArrayList("ITEM_ID_LIST", skuList);

        Bundle skuDetails;
        try {
            skuDetails = mService.getSkuDetails(3, getPackageName(), "inapp", querySkus);
        } catch (RemoteException e) {
            e.printStackTrace();
            return map;
        }
        final int BILLING_RESPONSE_RESULT_OK = 0;
        int response = skuDetails.getInt("RESPONSE_CODE");
        if (response == BILLING_RESPONSE_RESULT_OK) {
            ArrayList<String> responseList
                    = skuDetails.getStringArrayList("DETAILS_LIST");

            for (String thisResponse : responseList) {
                try {
                    JSONObject object = new JSONObject(thisResponse);
                    String sku = object.getString("productId");
                    String price = object.getString("price");
                    map.put(sku, price);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return map;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK &&
                (requestCode == REQUEST_CODE_PURCHASE_DONATION ||
                        requestCode == REQUEST_CODE_PURCHASE_PRO)) {
            Log.i(TAG, "Purchase request for " + String.valueOf(requestCode));
            int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
            String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
            String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");
            Log.i(TAG, purchaseData);

            updateAllPurchases();
            try {
                JSONObject jo = new JSONObject(purchaseData);
                String sku = jo.getString("productId");
                if (purchases.containsKey(sku)) {
                    showThankYouDialog();
                    if (itemPurchaseListener != null) {
                        itemPurchaseListener.onItemPurchased(sku);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    protected void updateAllPurchases() {
        if (billingHelper != null) {
            purchases = billingHelper.getPurchases();
        }
    }

    protected void onPurchasesInitialized() {

    }

    public void showThankYouDialog() {
        //new AlertDialog.Builder(this, R.style.DialogTheme)
        new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.dialog_title_thank_you))
                .setMessage(R.string.dialog_message_thank_you)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
