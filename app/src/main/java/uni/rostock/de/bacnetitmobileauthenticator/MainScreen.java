package uni.rostock.de.bacnetitmobileauthenticator;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);
    }

    public void startCamera(View view){
        Intent intent = new Intent(this,CameraActivity.class);
        startActivity(intent);
    }
}
