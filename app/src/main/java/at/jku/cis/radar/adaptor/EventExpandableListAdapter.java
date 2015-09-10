package at.jku.cis.radar.adaptor;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.util.List;

import at.jku.cis.radar.R;
import at.jku.cis.radar.model.Event;

public class EventExpandableListAdapter extends BaseExpandableListAdapter {

    private Context context;
    private List<Event> events;

    public EventExpandableListAdapter(Context context, List<Event> events) {
        this.context = context;
        this.events = events;
    }

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
        final Event event = (Event) getGroup(groupPosition);
        if (convertView == null) {
            convertView = getLayoutInflater().inflate(R.layout.list_group, null);
        }
        TextView textView = (TextView) convertView
                .findViewById(R.id.lblListHeader);
        textView.setTypeface(null, Typeface.BOLD);
        textView.setText(event.getName());
        ((GradientDrawable) textView.getBackground()).setColor(event.getColor());
        return convertView;
    }


    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        final Event event = (Event) getChild(groupPosition, childPosition);
        if (convertView == null) {
            convertView = getLayoutInflater().inflate(R.layout.list_item, null);
        }
        if (event.isVisible()) {
            convertView.setBackgroundColor(Color.WHITE);
            convertView.setAlpha(1.0f);
        } else {
            convertView.setBackgroundColor(Color.GRAY);
            convertView.setAlpha(0.2f);
        }
        TextView textView = (TextView) convertView
                .findViewById(R.id.lblListItem);
        textView.setText(event.getName());
        return convertView;
    }


    private LayoutInflater getLayoutInflater() {
        return (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    }
}