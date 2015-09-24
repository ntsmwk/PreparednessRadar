package at.jku.cis.radar.fragment;

import android.app.Fragment;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import at.jku.cis.radar.R;
import at.jku.cis.radar.model.Event;
import at.jku.cis.radar.service.EventDOMParser;

public class SelectableTreeFragment extends Fragment implements ExpandableListView.OnChildClickListener {

    private static final String EVENT_TREE_XML = "eventTree.xml";

    private List<Event> events;
    private List<EventClickListener> eventClickListeners = new ArrayList<>();

    private ExpandableListView expandableListView;

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_selectable_nodes, container, false);
        events = parseEvents(inflater);
        expandableListView = (ExpandableListView) rootView.findViewById(R.id.lvExp);
        expandableListView.setOnChildClickListener(this);
        expandableListView.setAdapter(new EventExpandableListAdapter());
        return rootView;
    }

    private List<Event> parseEvents(LayoutInflater inflater) {
        try {
            return new EventDOMParser().processXML(inflater.getContext().getAssets().open(EVENT_TREE_XML));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void addEventClickListener(EventClickListener eventClickListener) {
        eventClickListeners.add(eventClickListener);
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View view, int groupPosition, int childPosition, long id) {
        Event event = (Event) parent.getExpandableListAdapter().getChild(groupPosition, childPosition);
        if (event.isSelected()) {
            event.setSelected(false);
            fireEventSelectionChanged(event);
        }
        event.setVisible(!event.isVisible());
        fireEventVisiblityChanged(event);
        parent.invalidateViews();
        return true;
    }

    private void fireEventSelectionChanged(Event event) {
        for (EventClickListener listener : eventClickListeners) {
            listener.handleEventSelectionChanged(event);
        }
    }

    private void fireEventVisiblityChanged(Event event) {
        for (EventClickListener listener : eventClickListeners) {
            listener.handleEventVisibleChanged(event);
        }
    }

    public interface EventClickListener {
        void handleEventVisibleChanged(Event event);

        void handleEventSelectionChanged(Event event);
    }

    private class EventExpandableListAdapter extends BaseExpandableListAdapter {

        public static final float ALPHA_VISIBLE = 1.0f;
        public static final float ALPHA_HIDDEN = 0.2f;

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        @Override
        public int getGroupCount() {
            return events.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return events.get(groupPosition).getEvents().size();
        }

        @Override
        public Object getGroup(int groupPosition) {
            return events.get(groupPosition);
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            List<Event> children = events.get(groupPosition).getEvents();
            return children != null ? children.get(childPosition) : null;
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition * getChildrenCount(groupPosition);
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return getGroupId(groupPosition) + childPosition;
        }


        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            Event event = (Event) getGroup(groupPosition);
            convertView = getActivity().getLayoutInflater().inflate(R.layout.list_group, null);
            TextView textView = (TextView) convertView
                    .findViewById(R.id.lblListHeader);
            textView.setTypeface(null, Typeface.BOLD);
            textView.setText(event.getName());
            ((GradientDrawable) textView.getBackground()).setColor(event.getColor());
            return convertView;
        }


        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            Event event = (Event) getChild(groupPosition, childPosition);
            convertView = getActivity().getLayoutInflater().inflate(R.layout.list_item, null);
            CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.lblCheckbox);
            checkBox.setEnabled(event.isVisible());
            checkBox.setChecked(event.isSelected());
            checkBox.setOnCheckedChangeListener(new CheckUpdateListener(event));
            ((TextView) convertView
                    .findViewById(R.id.lblListItem)).setText(event.getName());

            if (event.isVisible()) {
                convertView.setBackgroundColor(Color.WHITE);
                convertView.setAlpha(ALPHA_VISIBLE);
            } else {
                convertView.setBackgroundColor(Color.GRAY);
                convertView.setAlpha(ALPHA_HIDDEN);
            }
            return convertView;
        }
    }

    private final class CheckUpdateListener implements CompoundButton.OnCheckedChangeListener {
        private Event event;

        private CheckUpdateListener(Event event) {
            this.event = event;
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean selected) {
            unselectEvents(events);
            event.setSelected(selected);
            fireEventSelectionChanged(event);
            expandableListView.invalidateViews();
        }

        private void unselectEvents(List<Event> eventList) {
            for (Event event : eventList) {
                event.setSelected(false);
                if (event.getEvents() != null) {
                    unselectEvents(event.getEvents());
                }
            }
        }
    }
}