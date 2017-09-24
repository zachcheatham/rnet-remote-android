package zachcheatham.me.rnetremote;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.net.InetAddress;

public class SelectServerDialogFragment extends DialogFragment
        implements ServersAdapter.ItemClickListener

{
    private SelectServerListener listener;

    private NsdManager nsdManager;
    private NsdManager.DiscoveryListener discoveryListener;

    private ServersAdapter adapter = new ServersAdapter(this);
    private RecyclerView recyclerView;
    private View searchingIndicator;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        nsdManager = (NsdManager) getContext().getSystemService(Context.NSD_SERVICE);
        createNSDListener();
        createResolveListener();
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
        recyclerView.setVisibility(View.GONE);

    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        LayoutInflater inflater = getActivity().getLayoutInflater();

        AlertDialog.Builder builder = new AlertDialog.Builder(
                new ContextThemeWrapper(getContext(), R.style.AppTheme_DialogOverlay));
        builder.setTitle(R.string.dialog_change_server);

        View view = inflater.inflate(R.layout.dialog_fragment_select_server, null);

        recyclerView = view.findViewById(R.id.list_servers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));
        recyclerView.setAdapter(adapter);

        searchingIndicator = view.findViewById(R.id.searching);

        builder.setView(view);

        if (isCancelable())
        {
            builder.setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {}
            });
        }
        else
        {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) recyclerView
                    .getLayoutParams();
            params.setMargins(0, 0, 0, (int) getResources().getDimension(R.dimen.dialog_padding));

            params = (ViewGroup.MarginLayoutParams) searchingIndicator.getLayoutParams();
            params.setMargins(
                    (int) getResources().getDimension(R.dimen.dialog_padding),
                    0,
                    (int) getResources().getDimension(R.dimen.dialog_padding),
                    (int) getResources().getDimension(R.dimen.dialog_padding)
            );
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
                            recyclerView.setVisibility(View.GONE);
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

                if (adapter.getItemCount() < 1)
                {
                    searchingIndicator.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                }
            }
        };
    }

    private NsdManager.ResolveListener createResolveListener()
    {
        NsdManager.ResolveListener resolveListener = new NsdManager.ResolveListener()
        {

            @Override
            public void onResolveFailed(NsdServiceInfo nsdServiceInfo, int i) {}

            @Override
            public void onServiceResolved(final NsdServiceInfo nsdServiceInfo)
            {
                getActivity().runOnUiThread(new Runnable()
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
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                });
            }
        };

        return resolveListener;
    }

    @Override
    public void onItemClick(int position)
    {
        ServersAdapter.RNetServer server = adapter.getServer(position);
        listener.serverSelected(server.name, server.host, server.port);
        dismiss();
    }

    interface SelectServerListener
    {
        void serverSelected(String name, InetAddress address, int port);
    }
}
