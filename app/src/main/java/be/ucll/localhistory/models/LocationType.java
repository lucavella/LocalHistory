package be.ucll.localhistory.models;

public enum LocationType {
    OTHER,
    NATURE,
    MEMORIAL,
    MONUMENT,
    BUILDING;

    @Override
    public String toString() {
        String first = this.name().substring(0, 1).toUpperCase();
        String next = this.name().substring(1).toLowerCase();

        return String.format("%s%s", first, next);
    }
}
