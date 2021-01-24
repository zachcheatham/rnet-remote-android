package me.zachcheatham.rnetremote;

import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class LicensesActivity extends AppCompatActivity
{
    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_licenses);

        TextView textView = findViewById(R.id.license_text);
        textView.setMovementMethod(LinkMovementMethod.getInstance());

        StringBuilder sb = new StringBuilder();
        try
        {
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader((getResources().openRawResource(R.raw.licenses))));
            String line;
            while ((line = reader.readLine()) != null)
            {
                sb.append(line);
                sb.append("<br>");
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            {
                textView.setText(Html.fromHtml(sb.toString(), 0));
            }
            else
            {
                textView.setText(Html.fromHtml(sb.toString()));
            }
        }
        catch (IOException ignored)
        {
            textView.setText(R.string.error_licenses);
        }
    }

    @Override
    public void finish()
    {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.slide_right);
    }
}
