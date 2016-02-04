package at.jku.cis.radar.model;

public enum ApplicationMode {
    CREATING("Create Mode"),
    EDITING("Edit Mode"),
    EVOLVING("Evolve Mode"),
    EVOLVE("Evolution Mode");


    private final String name;

    ApplicationMode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
