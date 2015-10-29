package szoftarch.com.lakogyules;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class MainMenu extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
//        final Intent scanIntent = new Intent(this, ScanActivity.class);
//        final Intent databaseIntent = new Intent(this, DatabaseActivity.class);
//        final Intent aboutIntent = new Intent(this, AboutActivity.class);


        final Button scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
//                startActivity(scanIntent);
            }
        });

        final Button databaseButton = (Button) findViewById(R.id.button_database);

        databaseButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
//                startActivity(databaseIntent);
            }
        });

        final Button aboutButton = (Button) findViewById(R.id.button_about);
        aboutButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
//                startActivity(aboutIntent);

            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
