package com.family.schedule.e2e;

public enum Members {
    DAD      (1, "お父さん"),
    MOM      (2, "お母さん"),
    DAUGHTER1(3, "長女"),
    DAUGHTER2(4, "次女"),
    SON1     (5, "長男");

    public final int id;
    public final String name;

    Members(int id, String name) {
        this.id = id;
        this.name = name;
    }
}
