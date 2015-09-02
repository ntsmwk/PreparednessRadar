package at.jku.cis.radar.view;


import android.app.Fragment;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.unnamed.b.atv.model.TreeNode;
import com.unnamed.b.atv.view.AndroidTreeView;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import at.jku.cis.radar.R;
import at.jku.cis.radar.model.EventDOMParser;
import at.jku.cis.radar.model.EventTreeBuilder;
import at.jku.cis.radar.model.EventTreeNode;
import at.jku.cis.radar.model.XMLEvent;
import at.jku.cis.radar.view.treeViewHolder.SelectableItemHolder;

public class SelectableTreeFragment extends Fragment {
    private AndroidTreeView tView;
    private boolean selectionModeEnabled = false;
    private EventTreeNode rootEventNode;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_selectable_nodes, null, false);
        ViewGroup containerView = (ViewGroup) rootView.findViewById(R.id.container);
        List<XMLEvent> eventList = null;
        try {
            eventList = initializeEvents();
        } catch (IOException | ParserConfigurationException | SAXException e) {
            System.exit(1);
        }
        rootEventNode = EventTreeBuilder.initializeEventTree(eventList);

        tView = new AndroidTreeView(getActivity(), rootEventNode.getTreeNode());
        tView.setDefaultAnimation(true);

        View selectionModeButton = rootView.findViewById(R.id.btn_toggleSelection);
        selectionModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectionModeEnabled = !selectionModeEnabled;
                tView.setSelectionModeEnabled(selectionModeEnabled);
            }
        });

        View selectAllBtn = rootView.findViewById(R.id.btn_selectAll);
        selectAllBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!selectionModeEnabled) {
                    Toast.makeText(getActivity(), "Enable selection mode first", Toast.LENGTH_SHORT).show();
                }
                tView.selectAll(true);
            }
        });

        View deselectAll = rootView.findViewById(R.id.btn_deselectAll);
        deselectAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!selectionModeEnabled) {
                    Toast.makeText(getActivity(), "Enable selection mode first", Toast.LENGTH_SHORT).show();
                }
                tView.deselectAll();
            }
        });

        View check = rootView.findViewById(R.id.btn_checkSelection);
        check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!selectionModeEnabled) {
                    Toast.makeText(getActivity(), "Enable selection mode first", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), tView.getSelected().size() + " selected", Toast.LENGTH_SHORT).show();
                }
            }
        });


        containerView.addView(tView.getView());

        if (savedInstanceState != null) {
            String state = savedInstanceState.getString("tState");
            if (!TextUtils.isEmpty(state)) {
                tView.restoreState(state);
            }
        }
        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("tState", tView.getSaveState());
    }

    private List<XMLEvent> initializeEvents() throws IOException, ParserConfigurationException, SAXException {
        InputStream in_s = getActivity().getApplicationContext().getAssets().open("eventTree.xml");
        return EventDOMParser.processXML(in_s);
    }


}