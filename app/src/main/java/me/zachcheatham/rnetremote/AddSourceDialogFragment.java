package me.zachcheatham.rnetremote;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

public class AddSourceDialogFragment extends DialogFragment
{
    private AddSourceListener listener;
    private TextInputLayout sourceNameInputLayout;
    private TextInputEditText sourceNameEditText;
    private TextInputLayout sourceIdInputLayout;
    private TextInputEditText sourceIdEditText;

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
        listener = (AddSourceListener) context;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        LayoutInflater inflater = getActivity().getLayoutInflater();

        AlertDialog.Builder builder = new AlertDialog.Builder(
                new ContextThemeWrapper(getContext(), R.style.AppTheme_DialogOverlay));
        builder.setTitle(R.string.action_add_source);

        View view = inflater.inflate(R.layout.dialog_fragment_add_source, null);

        sourceNameInputLayout = view.findViewById(R.id.text_input_layout_source_name);
        sourceNameEditText = view.findViewById(R.id.edit_text_source_name);
        sourceIdInputLayout = view.findViewById(R.id.text_input_layout_source_id);
        sourceIdEditText = view.findViewById(R.id.edit_text_source_id);

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
                            int sourceId =
                                    Integer.parseInt(sourceIdEditText.getText().toString()) - 1;

                            if (!listener.sourceExists(sourceId))
                            {
                                dialog.dismiss();
                                listener.addSource(sourceNameEditText.getText().toString(),
                                        sourceId);
                            }
                            else
                            {
                                Toast.makeText(getContext(), R.string.error_source_exists,
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

        if (sourceNameEditText.length() == 0)
        {
            validated = false;
            sourceNameInputLayout
                    .setError(getResources().getString(R.string.error_empty_source_name));
        }
        else if (sourceNameEditText.length() > 251)
        {
            validated = false;
            sourceNameInputLayout
                    .setError(getResources().getString(R.string.error_long_source_name));
        }
        else
        {
            sourceNameInputLayout.setErrorEnabled(false);
        }

        if (sourceIdEditText.length() == 0)
        {
            validated = false;
            sourceIdInputLayout.setError(getResources().getString(R.string.error_empty_source_id));
        }
        else
        {
            int sourceId = Integer.parseInt(sourceIdEditText.getText().toString());
            if (sourceId < 1 || sourceId > 99)
            {
                validated = false;
                sourceIdInputLayout.setError(getResources().getString(R.string.error_id_range));
            }
            else
            {
                sourceIdInputLayout.setErrorEnabled(false);
            }
        }

        return validated;
    }

    interface AddSourceListener
    {
        void addSource(String sourceName, int sourceId);

        boolean sourceExists(int sourceId);
    }
}
