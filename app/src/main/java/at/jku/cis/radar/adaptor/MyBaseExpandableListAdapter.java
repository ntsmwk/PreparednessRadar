package at.jku.cis.radar.adaptor;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.util.List;

import at.jku.cis.radar.R;
import at.jku.cis.radar.model.XMLEvent;

public class MyBaseExpandableListAdapter extends BaseExpandableListAdapter {

    private Context context;
    private List<XMLEvent> xmlEvents;

    public MyBaseExpandableListAdapter(Context context, List<XMLEvent> xmlEvents) {
        this.context = context;
        this.xmlEvents = xmlEvents;
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
        return xmlEvents.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return xmlEvents.get(groupPosition).getSubEventList().size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return xmlEvents.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return xmlEvents.get(groupPosition).getSubEventList().get(childPosition);
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
        XMLEvent xmlEvent = (XMLEvent) getGroup(groupPosition);
        if (convertView == null) {
            convertView = getLayoutInflater().inflate(R.layout.list_group, null);
        }
        TextView lblListHeader = (TextView) convertView
                .findViewById(R.id.lblListHeader);
        lblListHeader.setTypeface(null, Typeface.BOLD);
        lblListHeader.setText(xmlEvent.getEventName());
        return convertView;
    }


    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        XMLEvent xmlEvent = (XMLEvent) getChild(groupPosition, childPosition);
        if (convertView == null) {
            convertView = getLayoutInflater().inflate(R.layout.list_item, null);
        }

        TextView txtListChild = (TextView) convertView
                .findViewById(R.id.lblListItem);

        txtListChild.setText(xmlEvent.getEventName());
        return convertView;
    }


    private LayoutInflater getLayoutInflater() {
        return (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
}