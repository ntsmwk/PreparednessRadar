package at.jku.cis.radar.fragment;


import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import at.jku.cis.radar.R;
import at.jku.cis.radar.adaptor.EventExpandableListAdapter;
import at.jku.cis.radar.model.Event;
import at.jku.cis.radar.service.EventDOMParser;

public class SelectableTreeFragment extends Fragment implements ExpandableListView.OnChildClickListener {

    private static final String EVENT_TREE_XML = "eventTree.xml";

    private EventExpandableListAdapter eventExpandableListAdapter;

    private List<EventClickListener> eventClickListeners = new ArrayList<>();

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_selectable_nodes, container, false);
        ExpandableListView expandableListView = (ExpandableListView) rootView.findViewById(R.id.lvExp);
        eventExpandableListAdapter = new EventExpandableListAdapter(inflater.getContext(), getEvents(inflater));
        expandableListView.setOnChildClickListener(this);
        expandableListView.setAdapter(eventExpandableListAdapter);
        return rootView;
    }

    public void addEventClickListener(EventClickListener eventClickListener) {
        eventClickListeners.add(eventClickListener);
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        for (EventClickListener listener : eventClickListeners) {
            listener.handleEventClick((Event) eventExpandableListAdapter.getChild(groupPosition, childPosition));
        }
        return true;
    }

    private List<Event> getEvents(LayoutInflater inflater) {
        try {
            return new EventDOMParser().processXML(inflater.getContext().getAssets().open(EVENT_TREE_XML));
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

    public interface EventClickListener {
        void handleEventClick(Event event);
    }
}