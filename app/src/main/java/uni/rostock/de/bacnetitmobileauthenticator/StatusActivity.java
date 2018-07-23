package uni.rostock.de.bacnetitmobileauthenticator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class StatusActivity extends AppCompatActivity {

    private static final String TAG = "StatusActivity";
    TextView textView;
    ImageView notOk;
    ImageView ok;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);
        textView = (TextView) findViewById(R.id.text);
        notOk = (ImageView) findViewById(R.id.notOk);
        ok = (ImageView) findViewById(R.id.ok);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(BACnetIntentService.ADD_DEVICE_REQUEST_ACK_ACTION));
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, final Intent intent) {

            Log.d(TAG, "statusActivity received some broadcast signal");
            if (intent.getAction().contentEquals(BACnetIntentService.ADD_DEVICE_REQUEST_ACK_ACTION)) {
                Log.d(TAG, "received AddDeviceRequest ack");
                notOk.setVisibility(View.GONE);
                ok.setVisibility(View.GONE);
                textView.setVisibility(View.VISIBLE);
                textView.setText("Press Push Button on Device!");
                textView.setTextColor(Color.BLACK);
                textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
            }

            if (intent.getAction().contentEquals(BACnetIntentService.ADD_DEVICE_REQUEST_CONFIRM_ACTION)) {
                Log.d(TAG, "received AddDeviceRequest confirm status received");
                boolean status = intent.getBooleanExtra("String", true);
                textView.setVisibility(View.GONE);
                if (status) {
                    ok.setVisibility(View.VISIBLE);
                    ok.bringToFront();
                } else {
                    notOk.setVisibility(View.VISIBLE);
                    notOk.bringToFront();
                }
            }
        }

        ;
    };
}
