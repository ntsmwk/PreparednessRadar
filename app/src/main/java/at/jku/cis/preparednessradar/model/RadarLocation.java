package at.jku.cis.preparednessradar.model;

import at.jku.cis.preparednessradar.entry.RadarLocationEntry;

public class RadarLocation implements RadarLocationEntry {
    private int id;
    private String title;

    public RadarLocation() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public int hashCode() {
        return id * 31;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RadarLocation)) {
            return false;
        }
        return ((RadarLocation) o).id == id;
    }
}
