package docSharing.entities.permission;

public enum Permission {
    OWNER("own"),
    EDITOR("edit"),
    VIEWER("view");

    private final String text;

    Permission(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
