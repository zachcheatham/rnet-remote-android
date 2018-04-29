package me.zachcheatham.rnetremote.adapter;

import android.app.Activity;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import me.zachcheatham.rnetremote.R;
import me.zachcheatham.rnetremotecommon.rnet.RNetServer;
import me.zachcheatham.rnetremotecommon.rnet.Source;

public class SourcesAdapter extends BaseAdapter implements RNetServer.SourcesListener
{
    private final Activity activity;
    private final LayoutInflater inflater;
    private RNetServer server;

    SourcesAdapter(Activity activity)
    {
        this.activity = activity;
        inflater = LayoutInflater.from(new ContextThemeWrapper(activity, R.style.AppTheme_SourceListOverlay));
    }

    @Override
    public int getCount()
    {
        return server.getSources().size();
    }

    @Override
    public Object getItem(int position)
    {
        return null;
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        final View view;

        if (convertView == null)
            view = inflater.inflate(R.layout.item_select_source, parent, false);
        else
            view = convertView;

        if (server != null)
        {
            Source source = server.getSource(server.getSources().keyAt(position));

            ((TextView) view.findViewById(R.id.text_name)).setText(source.getName());
            ((ImageView) view.findViewById(R.id.icon)).setImageResource(source.getTypeDrawable());
        }

        return view;
    }


    public void setServer(RNetServer server)
    {
        if (this.server != null)
        {
            this.server.removeSourcesListener(this);
        }

        this.server = server;

        if (server != null)
        {
            server.addSourcesListener(this);
        }

        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public void sourceAdded(Source source)
    {
        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public void sourceChanged(Source source, boolean setRemotely, RNetServer.SourceChangeType type)
    {
        if (type == RNetServer.SourceChangeType.NAME || type == RNetServer.SourceChangeType.TYPE)
        {
            activity.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    notifyDataSetChanged();
                }
            });
        }
    }

    @Override
    public void descriptiveText(Source source, String text, int length) {}

    @Override
    public void sourceRemoved(int sourceId)
    {
        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public void cleared()
    {
        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                notifyDataSetChanged();
            }
        });
    }
}