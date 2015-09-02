package at.jku.cis.radar.fragment;


import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.Toast;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import at.jku.cis.radar.R;
import at.jku.cis.radar.adaptor.MyBaseExpandableListAdapter;
import at.jku.cis.radar.model.EventDOMParser;
import at.jku.cis.radar.model.XMLEvent;

public class SelectableTreeFragment extends Fragment {

    private ExpandableListView expandableListView;

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_selectable_nodes, container, false);
        expandableListView = (ExpandableListView) rootView.findViewById(R.id.lvExp);
        expandableListView.setAdapter(new MyBaseExpandableListAdapter(inflater.getContext(), getXMLEvents(inflater)));
        return rootView;
    }

    private List<XMLEvent> getXMLEvents(LayoutInflater inflater) {
        List<XMLEvent> xmlEvents = new ArrayList<>();
        try {
            xmlEvents = EventDOMParser.processXML(inflater.getContext().getAssets().open("eventTree.xml"));
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new RuntimeException(e);
        }
        return xmlEvents;
    }


}