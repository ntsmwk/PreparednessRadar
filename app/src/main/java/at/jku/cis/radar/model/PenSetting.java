package at.jku.cis.radar.model;

import android.graphics.Color;

public class PenSetting {
    private Event event;
    private int color = Color.BLACK;
    private PenMode penMode = PenMode.DRAWING;
    private DrawType drawType = DrawType.POLYGON;

    public PenMode getPenMode() {
        return penMode;
    }

    public void setPenMode(PenMode penMode) {
        this.penMode = penMode;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public DrawType getDrawType() {
        return drawType;
    }

    public void setDrawType(DrawType drawType) {
        this.drawType = drawType;
    }
}
