package at.jku.cis.radar.model;

import java.util.List;

public class Event {
    private long id;
    private String name;
    private int color;
    private boolean selected;
    private boolean visible = true;
    private List<Event> events;
    private List<Action> actions;

    public Event() {
    }

    public Event(String name, int color, List<Event> events, List<Action> actions) {
        this.name = name;
        this.color = color;
        this.events = events;
        this.actions = actions;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getColor() {
        return this.color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    public List<Action> getActions() {
        return actions;
    }

    public void setActions(List<Action> actions) {
        this.actions = actions;
    }

}
