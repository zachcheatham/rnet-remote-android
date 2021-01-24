package me.zachcheatham.rnetremote;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public class SelectServerDialogFragment extends DialogFragment
        implements ServersAdapter.ItemClickListener
{
    private SelectServerListener listener;

    private NsdManager nsdManager;
    private NsdManager.DiscoveryListener discoveryListener;

    private final ServersAdapter adapter = new ServersAdapter(this);
    private View searchingIndicator;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Context c = getContext();
        if (c != null)
        {
            nsdManager = (NsdManager) getContext().getSystemService(Context.NSD_SERVICE);
            createNSDListener();
        }
    }

    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);
        listener = (SelectServerListener) context;
    }

    @Override
    public void onStart()
    {
        super.onStart();
        nsdManager.discoverServices("_rnet._tcp", NsdManager.PROTOCOL_DNS_SD, discoveryListener);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        nsdManager.stopServiceDiscovery(discoveryListener);

        adapter.clearServers();
        searchingIndicator.setVisibility(View.VISIBLE);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        Activity activity = getActivity();
        assert activity != null;
        LayoutInflater inflater = getActivity().getLayoutInflater();

        AlertDialog.Builder builder = new AlertDialog.Builder(
                new ContextThemeWrapper(getContext(), R.style.AppTheme_DialogOverlay));
        builder.setTitle(R.string.dialog_change_server);

        @SuppressLint("InflateParams") View view = inflater
                .inflate(R.layout.dialog_fragment_select_server, null);

        RecyclerView recyclerView = view.findViewById(R.id.list_servers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));
        recyclerView.setAdapter(adapter);

        searchingIndicator = view.findViewById(R.id.searching);

        builder.setView(view);

        if (isCancelable())
        {
            builder.setNegativeButton(android.R.string.cancel, null);
        }
        else
        {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) recyclerView
                    .getLayoutParams();
            params.setMargins(0, 0, 0, (int) getResources().getDimension(R.dimen.dialog_padding));
        }

        builder.setCancelable(false);

        return builder.create();
    }

    private void createNSDListener()
    {
        discoveryListener = new NsdManager.DiscoveryListener()
        {
            @Override
            public void onStartDiscoveryFailed(String s, int i) {}

            @Override
            public void onStopDiscoveryFailed(String s, int i) {}

            @Override
            public void onDiscoveryStarted(String s) {}

            @Override
            public void onDiscoveryStopped(String s)
            {
                if (getActivity() != null)
                {
                    getActivity().runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            adapter.clearServers();
                            searchingIndicator.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }

            @Override
            public void onServiceFound(NsdServiceInfo nsdServiceInfo)
            {
                nsdManager.resolveService(nsdServiceInfo, createResolveListener());
            }

            @Override
            public void onServiceLost(NsdServiceInfo nsdServiceInfo)
            {
                adapter.removeServer(nsdServiceInfo.getHost(), nsdServiceInfo.getPort());

                if (adapter.getItemCount() < 2)
                {
                    searchingIndicator.setVisibility(View.VISIBLE);
                }
            }
        };
    }

    private NsdManager.ResolveListener createResolveListener()
    {

        return new NsdManager.ResolveListener()
        {

            @Override
            public void onResolveFailed(NsdServiceInfo nsdServiceInfo, int i) {}

            @Override
            public void onServiceResolved(final NsdServiceInfo nsdServiceInfo)
            {
                Activity activity = getActivity();
                if (activity != null)
                    activity.runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            adapter.addServer(
                                    nsdServiceInfo.getServiceName(),
                                    nsdServiceInfo.getHost(),
                                    nsdServiceInfo.getPort()
                            );

                            searchingIndicator.setVisibility(View.GONE);
                        }
                    });
            }
        };
    }

    @Override
    public void onItemClick(int position)
    {
        if (position == (adapter.getItemCount() - 1))
        {
            FragmentManager fm = getFragmentManager();
            if (fm != null)
            {
                EnterServerDialogFragment dialog = new EnterServerDialogFragment();
                dialog.setCancelable(true);
                dialog.show(fm, "EnterServerDialogFragment");
            }
        }
        else
        {
            ServersAdapter.RNetServer server = adapter.getServer(position);
            listener.serverSelected(server.name, server.host, server.port);
            dismiss();
        }
    }
}
