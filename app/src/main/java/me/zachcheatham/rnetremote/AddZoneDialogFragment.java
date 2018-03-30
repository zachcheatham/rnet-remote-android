package me.zachcheatham.rnetremote;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

public class AddZoneDialogFragment extends DialogFragment
{
    private AddZoneListener listener;
    private TextInputLayout zoneNameInputLayout;
    private TextInputEditText zoneNameEditText;
    private TextInputLayout controllerIdInputLayout;
    private TextInputEditText controllerIdEditText;
    private TextInputLayout zoneIdInputLayout;
    private TextInputEditText zoneIdEditText;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        getDialog().getWindow()
                   .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);
        listener = (AddZoneListener) context;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        Activity activity = getActivity();
        assert activity != null;
        LayoutInflater inflater = activity.getLayoutInflater();

        AlertDialog.Builder builder = new AlertDialog.Builder(
                new ContextThemeWrapper(getContext(), R.style.AppTheme_DialogOverlay));
        builder.setTitle(R.string.action_add_zone);

        @SuppressLint("InflateParams") View view = inflater
                .inflate(R.layout.dialog_fragment_add_zone, null);

        zoneNameInputLayout = view.findViewById(R.id.text_input_layout_zone_name);
        zoneNameEditText = view.findViewById(R.id.edit_text_zone_name);
        controllerIdInputLayout = view.findViewById(R.id.text_input_layout_controller_id);
        controllerIdEditText = view.findViewById(R.id.edit_text_controller_id);
        zoneIdInputLayout = view.findViewById(R.id.text_input_layout_zone_id);
        zoneIdEditText = view.findViewById(R.id.edit_text_zone_id);

        builder.setView(view);
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setPositiveButton(R.string.action_add, null);

        final AlertDialog dialog = builder.create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener()
        {
            @Override
            public void onShow(DialogInterface dialogInterface)
            {
                Button button = ((AlertDialog) dialogInterface)
                        .getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        if (validate())
                        {
                            int controllerId =
                                    Integer.parseInt(controllerIdEditText.getText().toString()) - 1;
                            int zoneId = Integer.parseInt(zoneIdEditText.getText().toString()) - 1;

                            if (!listener.zoneExists(controllerId, zoneId))
                            {
                                dialog.dismiss();
                                listener.addZone(zoneNameEditText.getText().toString(),
                                        controllerId, zoneId);
                            }
                            else
                            {
                                Toast.makeText(getContext(), R.string.error_zone_exists,
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
            }
        });

        return dialog;
    }

    private boolean validate()
    {
        boolean validated = true;

        if (zoneNameEditText.length() == 0)
        {
            validated = false;
            zoneNameInputLayout.setError(getResources().getString(R.string.error_empty_zone_name));
        }
        else if (zoneNameEditText.length() > 251)
        {
            validated = false;
            zoneNameInputLayout.setError(getResources().getString(R.string.error_long_zone_name));
        }
        else
        {
            zoneNameInputLayout.setErrorEnabled(false);
        }

        if (controllerIdEditText.length() == 0)
        {
            validated = false;
            controllerIdInputLayout
                    .setError(getResources().getString(R.string.error_empty_controller_id));
        }
        else
        {
            int controllerId = Integer.parseInt(controllerIdEditText.getText().toString());
            if (controllerId < 1 || controllerId > 99)
            {
                validated = false;
                controllerIdInputLayout.setError(getResources().getString(R.string.error_id_range));
            }
            else
            {
                controllerIdInputLayout.setErrorEnabled(false);
            }
        }

        if (zoneIdEditText.length() == 0)
        {
            validated = false;
            zoneIdInputLayout.setError(getResources().getString(R.string.error_empty_zone_id));
        }
        else
        {
            int zoneId = Integer.parseInt(zoneIdEditText.getText().toString());
            if (zoneId < 1 || zoneId > 99)
            {
                validated = false;
                zoneIdInputLayout.setError(getResources().getString(R.string.error_id_range));
            }
            else
            {
                zoneIdInputLayout.setErrorEnabled(false);
            }
        }

        return validated;
    }

    interface AddZoneListener
    {
        void addZone(String zoneName, int controllerId, int zoneId);

        boolean zoneExists(int controllerId, int zoneId);
    }
}
