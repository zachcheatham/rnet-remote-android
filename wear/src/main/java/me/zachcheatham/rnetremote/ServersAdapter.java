package me.zachcheatham.rnetremote;

import android.support.wear.widget.WearableRecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

class ServersAdapter extends WearableRecyclerView.Adapter<WearableRecyclerView.ViewHolder>
{
    private static final int VIEWTYPE_ITEM = 0;
    private static final int VIEWTYPE_HEADER = 1;
    private static final int VIEWTYPE_FOOTER = 2;

    private final List<RNetServer> servers = new ArrayList<>();
    private final ItemClickListener clickListener;
    private final String headerText;

    ServersAdapter(String headerText, ItemClickListener clickListener)
    {
        this.headerText = headerText;
        this.clickListener = clickListener;
    }

    @Override
    public int getItemViewType(int position)
    {
        if (position == 0)
            return VIEWTYPE_HEADER;
        else if (position <= servers.size())
            return VIEWTYPE_ITEM;
        else
            return VIEWTYPE_FOOTER;
    }

    @Override
    public WearableRecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        switch (viewType)
        {
        case VIEWTYPE_ITEM:
        default:
        {
            View view = LayoutInflater.from(parent.getContext())
                                      .inflate(R.layout.item_server, parent, false);
            return new ViewHolder(view);
        }
        case VIEWTYPE_HEADER:
        {
            View view = LayoutInflater.from(parent.getContext())
                                      .inflate(R.layout.item_list_header, parent, false);
            return new HeaderViewHolder(view);
        }
        case VIEWTYPE_FOOTER:
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_list_footer, parent, false);
            return new FooterViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(WearableRecyclerView.ViewHolder viewHolder, int position)
    {
        switch (viewHolder.getItemViewType())
        {
        case VIEWTYPE_HEADER:
        {
            HeaderViewHolder holder = (HeaderViewHolder) viewHolder;
            holder.textView.setText(headerText);
            break;
        }
        case VIEWTYPE_ITEM:
        {
            ViewHolder holder = (ViewHolder) viewHolder;
            RNetServer rNetServer = servers.get(position - 1);
            holder.textView.setText(rNetServer.name);
            break;
        }
        }
    }

    @Override
    public int getItemCount()
    {
        return servers.size() + 2;
    }

    void addServer(String name, InetAddress host, int port)
    {
        RNetServer rNetServer = new RNetServer();
        rNetServer.name = name;
        rNetServer.port = port;
        rNetServer.host = host;

        servers.add(rNetServer);

        if (getItemCount() > 2)
            notifyItemInserted(servers.size());
        else
            notifyDataSetChanged();
    }

    void removeServer(InetAddress host, int port)
    {
        for (int i = 0; i < servers.size(); i++)
        {
            RNetServer r = servers.get(i);
            if (r.host.equals(host) && r.port == port)
            {
                servers.remove(i);

                if (getItemCount() < 3)
                    notifyDataSetChanged();
                else
                    notifyItemRemoved(i);
            }
        }
    }

    void clearServers()
    {
        servers.clear();
        notifyDataSetChanged();
    }

    RNetServer getServer(int position)
    {
        return servers.get(position);
    }

    interface ItemClickListener
    {
        void onItemClick(int position);
    }

    static class RNetServer
    {
        public String name;
        int port;
        InetAddress host;
    }

    class HeaderViewHolder extends WearableRecyclerView.ViewHolder
    {
        TextView textView;

        HeaderViewHolder(View itemView)
        {
            super(itemView);
            textView = itemView.findViewById(R.id.text);
        }
    }

    class ViewHolder extends WearableRecyclerView.ViewHolder implements View.OnClickListener
    {
        TextView textView;

        ViewHolder(View itemView)
        {
            super(itemView);
            textView = itemView.findViewById(R.id.name);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v)
        {
            clickListener.onItemClick(getAdapterPosition() - 1);
        }
    }

    class FooterViewHolder extends WearableRecyclerView.ViewHolder
    {
        FooterViewHolder(View itemView)
        {
            super(itemView);
        }
    }
}
