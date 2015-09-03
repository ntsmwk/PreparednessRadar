package at.jku.cis.radar.model;

import java.util.List;

public class Event {
    private String name;
    private int color;
    private List<Event> events;
    private List<Action> actions;

    public Event(String name, int color, List<Event> subEvent, List<Action> actions) {
        this.name = name;
        this.color = color;
        this.events = events;
        this.actions = actions;
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
