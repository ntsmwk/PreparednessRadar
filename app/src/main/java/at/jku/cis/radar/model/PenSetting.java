package at.jku.cis.radar.model;

import android.graphics.Color;

public class PenSetting {
    private PenMode penMode = PenMode.DRAWING;
    private DrawMode drawMode = DrawMode.LINE;
    private int color = Color.BLACK;
    private String paintingEvent;

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

    public String getPaintingEvent() {
        return paintingEvent;
    }

    public void setPaintingEvent(String paintingEvent) {
        this.paintingEvent = paintingEvent;
    }

    public DrawMode getDrawMode() {
        return drawMode;
    }

    public void setDrawMode(DrawMode drawMode) {
        this.drawMode = drawMode;
    }
}
