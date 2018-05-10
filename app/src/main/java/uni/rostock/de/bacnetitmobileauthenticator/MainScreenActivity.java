package uni.rostock.de.bacnetitmobileauthenticator;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainScreenActivity extends AppCompatActivity {
    Button flashButton;
    Button pushButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);
        flashButton = (Button) findViewById(R.id.flash_auth);
        pushButton = (Button) findViewById(R.id.push_auth);
        pushButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent flashAuthIntent = new Intent(getApplicationContext(), PBAuthActivity.class);
                startActivity(flashAuthIntent);
            }
        });

    }
}
