package at.jku.cis.radar.model;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;

public class XMLEvent {
    private String eventName;
    private List<XMLEvent> subEventList;
    private List<XMLAction> actionList;

    public XMLEvent(String eventName){
        this(eventName, null, null);
    }

    public XMLEvent(String eventName, List<XMLEvent> subEventList, List<XMLAction> actionList) {
        this.eventName = eventName;
        this.subEventList = subEventList;
        this.actionList = actionList;
    }

    public String getEventName() {
        return eventName;
    }

    public List<XMLEvent> getSubEventList() {
        return subEventList;
    }

    public List<XMLAction> getActionList() {
        return actionList;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public void setSubEventList(List<XMLEvent> subEventList) {
        this.subEventList = subEventList;
    }

    public void setActionList(List<XMLAction> actionList) {
        this.actionList = actionList;
    }

}
