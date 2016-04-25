package at.jku.cis.radar.model;

import java.io.Serializable;

public class AuthenticationToken implements Serializable {

    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
