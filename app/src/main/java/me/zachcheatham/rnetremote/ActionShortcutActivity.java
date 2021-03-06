package me.zachcheatham.rnetremote;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;

import me.zachcheatham.rnetremote.service.ActionService;

public class ActionShortcutActivity extends Activity
{
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(this, ActionService.class);
        intent.setAction(getIntent().getAction());
        startService(intent);

        finish();
    }
}
