package zachcheatham.me.rnetremote;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import java.net.InetAddress;

public class MainActivity extends AppCompatActivity implements SelectServerDialogFragment.SelectServerListener
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_remote, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        if (id == R.id.action_change_server)
        {
            SelectServerDialogFragment dialog = new SelectServerDialogFragment();
            dialog.setCancelable(false);
            dialog.show(getSupportFragmentManager(), "SelectServerDialogFragment");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void serverSelected(String name, InetAddress address, int port)
    {

    }
}
