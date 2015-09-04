package at.jku.cis.radar.model;

import android.graphics.Color;

public class PenSetting {
    private PenMode penMode = PenMode.DRAWING;
    private int color = Color.BLACK;

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
}
