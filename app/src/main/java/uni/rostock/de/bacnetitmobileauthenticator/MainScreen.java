package uni.rostock.de.bacnetitmobileauthenticator;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);
        Intent bacnetIntent = new Intent(this, BACnetIntentService.class);
        startService(bacnetIntent);
    }

    public void startCamera(View view) {
        Intent intent = new Intent(this,CameraActivity.class);
        startActivity(intent);
    }
}
