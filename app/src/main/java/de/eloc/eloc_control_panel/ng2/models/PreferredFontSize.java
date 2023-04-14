package de.eloc.eloc_control_panel.ng2.models;

public enum PreferredFontSize {
    small(-1),
    medium(0),
    large(1);

    private final int size;

    PreferredFontSize(int size) {
        this.size = size;
    }

    public int getSize() {
        return size;
    }

    public static PreferredFontSize fromInt(int size) {
        if (size < 0) {
            return small;
        } else if (size > 0) {
            return large;
        } else {
            return medium;
        }
    }
}
