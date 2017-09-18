package zachcheatham.me.rnetremote;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ZoneFragment extends Fragment
{
    private final static String ARG_ZONE = "zone_id";

    private short id;

    public ZoneFragment() {}

    public static ZoneFragment newInstance(short id)
    {
        ZoneFragment fragment = new ZoneFragment();
        Bundle args = new Bundle();
        args.putShort(ARG_ZONE, id);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
        {
            id = getArguments().getShort(ARG_ZONE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_zone, container, false);
    }
}
