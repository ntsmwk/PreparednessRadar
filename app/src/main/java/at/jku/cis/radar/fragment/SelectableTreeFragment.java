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

public class SelectableTreeFragment extends Fragment implements ExpandableListView.OnGroupExpandListener, ExpandableListView.OnChildClickListener, ExpandableListView.OnGroupCollapseListener {

    private static final String EVENT_TREE_XML = "eventTree.xml";

    private List<EventClickListener> eventClickListeners = new ArrayList<>();
    private ExpandableListView expandableListView;

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_selectable_nodes, container, false);
        expandableListView = (ExpandableListView) rootView.findViewById(R.id.lvExp);
        expandableListView.setOnChildClickListener(this);
        expandableListView.setOnGroupExpandListener(this);
        expandableListView.setOnGroupCollapseListener(this);
        expandableListView.setAdapter(new EventExpandableListAdapter(inflater.getContext(), getEvents(inflater)));
        return rootView;
    }

    public void addEventClickListener(EventClickListener eventClickListener) {
        eventClickListeners.add(eventClickListener);
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View view, int groupPosition, int childPosition, long id) {
        Event event = getEvent(parent, groupPosition, childPosition);
        event.setVisible(!event.isVisible());
        expandableListView.invalidateViews();
        notifyListeners(event);
        return true;
    }

    private Event getEvent(ExpandableListView parent, int groupPosition, int childPosition) {
        return (Event) parent.getExpandableListAdapter().getChild(groupPosition, childPosition);
    }

    private void notifyListeners(Event event) {
        for (EventClickListener listener : eventClickListeners) {
            listener.handleEventClick(event);
        }
    }

    private List<Event> getEvents(LayoutInflater inflater) {
        try {
            return new EventDOMParser().processXML(inflater.getContext().getAssets().open(EVENT_TREE_XML));
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onGroupExpand(int groupPosition) {
    }

    @Override
    public void onGroupCollapse(int groupPosition) {

    }

    public interface EventClickListener {
        void handleEventClick(Event event);
    }
}