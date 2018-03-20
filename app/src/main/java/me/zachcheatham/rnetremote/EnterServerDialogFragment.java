package me.zachcheatham.rnetremote;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.UnknownHostException;

import me.zachcheatham.rnetremote.rnet.RNetServer;

public class EnterServerDialogFragment extends DialogFragment implements RNetServer.StateListener
{
    private final static String IP_REGEX = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9" +
                                           "]|25[0-5])\\.){3}" +
                                           "([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])" +
                                           ":[0-9]{1,5}$";
    private final static String HOST_REGEX = "(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9" +
                                             "\\-]*[a-zA-Z0-9])\\.)*" +
                                             "([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])" +
                                             ":[0-9]{1,5}$";
    private final RNetServer server = new RNetServer(RNetServer.INTENT_SUBSCRIBE);

    private SelectServerListener listener;

    private TextInputLayout addressInputLayout;
    private View verifying;
    private boolean cancelable;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        server.addStateListener(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        Window window = getDialog().getWindow();
        assert window != null;
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }


    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);
        listener = (SelectServerListener) context;
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
        builder.setTitle(R.string.dialog_enter_server);

        @SuppressLint("InflateParams") View view = inflater
                .inflate(R.layout.dialog_fragment_enter_server, null);

        addressInputLayout = view.findViewById(R.id.input_layout_address);
        verifying = view.findViewById(R.id.verifying);

        builder.setView(view);
        builder.setPositiveButton(android.R.string.ok, null);

        if (isCancelable())
        {
            builder.setNegativeButton(android.R.string.cancel, null);
            cancelable = true;
        }
        builder.setCancelable(false);

        final EnterServerDialogFragment self = this;

        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener()
        {
            @Override
            public void onShow(DialogInterface dialogInterface)
            {
                final Button positiveButton = ((AlertDialog) dialogInterface)
                        .getButton(AlertDialog.BUTTON_POSITIVE);
                final Button negativeButton = ((AlertDialog) dialogInterface)
                        .getButton(AlertDialog.BUTTON_NEGATIVE);
                positiveButton.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {

                        ValidateAddressResult addressResult = validate(
                                addressInputLayout.getEditText().getText().toString());

                        if (addressResult != ValidateAddressResult.OK)
                        {
                            switch (addressResult)
                            {
                            case HOSTNAME:
                                addressInputLayout
                                        .setError(getString(R.string.error_invalid_hostname_port));
                                break;
                            case PORT:
                                addressInputLayout.setError(getString(R.string.error_invalid_port));
                                break;
                            }
                        }
                        else
                        {
                            addressInputLayout.setVisibility(View.GONE);
                            verifying.setVisibility(View.VISIBLE);
                            if (cancelable)
                                setCancelable(false);
                            positiveButton.setEnabled(false);
                            if (negativeButton != null)
                                negativeButton.setEnabled(false);

                            new TestServerTask(self).execute();
                        }
                    }
                });
            }
        });

        return dialog;
    }

    @Override
    public void onDestroy()
    {
        server.removeStateListener(this);
        super.onDestroy();
    }

    private ValidateAddressResult validate(String address)
    {
        if (!address.matches(IP_REGEX) && !address.matches(HOST_REGEX))
            return ValidateAddressResult.HOSTNAME;

        String[] sep = address.split(":", 2);
        int port = Integer.parseInt(sep[1]);

        if (port > 65535 || port < 1)
            return ValidateAddressResult.PORT;

        return ValidateAddressResult.OK;
    }

    @Override
    public void connectionInitiated() {}

    @Override
    public void connectError()
    {
        Activity activity = getActivity();
        if (activity != null)
        {
            activity.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    addressInputLayout.setError(getString(R.string.error_connect));
                    verifying.setVisibility(View.GONE);
                    addressInputLayout.setVisibility(View.VISIBLE);
                    ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    Button button = ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_NEGATIVE);
                    if (button != null)
                        button.setEnabled(true);
                    if (cancelable)
                        setCancelable(true);
                }
            });
        }
    }

    @Override
    public void ready()
    {
        server.disconnect();

        Activity activity = getActivity();
        if (activity != null)
        {
            activity.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    listener.serverSelected(getString(R.string.app_name), server.getAddress(),
                            server.getPort());

                    dismiss();

                    FragmentManager fm = getFragmentManager();
                    if (fm != null)
                    {
                        DialogFragment parent = (DialogFragment) fm
                                .findFragmentByTag("SelectServerDialogFragment");
                        if (parent != null)
                            parent.dismiss();
                    }
                }
            });
        }
    }

    @Override
    public void serialStateChanged(boolean connected) {}

    @Override
    public void updateAvailable() {}

    @Override
    public void disconnected(boolean unexpected) {}

    private enum ValidateAddressResult
    {
        HOSTNAME, PORT, OK
    }

    private static class TestServerTask extends AsyncTask<Void, Void, Void>
    {
        private final WeakReference<EnterServerDialogFragment> dialogReference;

        TestServerTask(EnterServerDialogFragment dialog)
        {
            dialogReference = new WeakReference<>(dialog);
        }

        @Override
        protected Void doInBackground(Void... voids)
        {
            final EnterServerDialogFragment dialog = dialogReference.get();
            if (dialog != null)
            {
                EditText editText = dialog.addressInputLayout.getEditText();
                assert editText != null;
                String[] sep = editText.getText().toString().split(":", 2);

                InetAddress host;
                int port;

                try
                {
                    host = InetAddress.getByName(sep[0]);
                    port = Integer.parseInt(sep[1]);
                }
                catch (UnknownHostException e)
                {
                    Activity activity = dialog.getActivity();
                    assert activity != null;
                    activity.runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            dialog.addressInputLayout
                                    .setError(dialog.getString(R.string.error_invalid_hostname));
                            dialog.verifying.setVisibility(View.GONE);
                            dialog.addressInputLayout.setVisibility(View.VISIBLE);
                            ((AlertDialog) dialog.getDialog()).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                            Button button = ((AlertDialog) dialog.getDialog()).getButton(AlertDialog.BUTTON_NEGATIVE);
                            if (button != null)
                                button.setEnabled(true);
                            if (dialog.cancelable)
                                dialog.setCancelable(true);
                        }
                    });

                    return null;
                }

                dialog.server.setConnectionInfo(host, port);
                new Thread(dialog.server.new ServerRunnable()).start();
            }

            return null;
        }
    }
}
