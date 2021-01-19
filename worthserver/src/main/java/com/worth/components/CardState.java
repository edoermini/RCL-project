package com.worth.components;

public enum CardState {
    TODO,
    INPROGRESS,
    TOBEREVISED,
    DONE;

    public static CardState fromString(String s) {
        switch (s.toUpperCase()) {
            case "TODO":
                return TODO;
            case "INPROGRESS":
                return INPROGRESS;
            case "TOBEREVISED":
                return TOBEREVISED;
            case "DONE":
                return DONE;
        }

        return null;
    }
}
