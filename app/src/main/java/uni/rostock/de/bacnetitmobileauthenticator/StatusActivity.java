package uni.rostock.de.bacnetitmobileauthenticator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class StatusActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            /*
            Listens for error messages if BACnetService fails,
            starts status intent early inform user about the error.
            */
            if (intent.hasExtra(BACnetIntentService.SERVICE_AUTH_FAILURE_PAYLOAD)) {
                //TODO: display failure symbol in image view
            } else if (intent.hasExtra(BACnetIntentService.SERVICE_AUTH_SUCCESS_PAYLOAD)) {
                //TODO: display success symbol in imageview
            }
        }
    };
}
