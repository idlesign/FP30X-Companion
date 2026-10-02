package dev.idlesign.fp30xcompanion;

import java.util.Objects;

public final class Tone {
    public enum Kind {
        INTERNAL,
        BANK_PROGRAM
    }

    public final int number;
    public final String category;
    public final String name;
    public final int bankMsb;
    public final int bankLsb;
    public final int program;
    public final Kind kind;
    public final int sysExCategory;
    public final int sysExNumber;

    public Tone(
            int number,
            String category,
            String name,
            int bankMsb,
            int bankLsb,
            int program,
            Kind kind,
            int sysExCategory,
            int sysExNumber
    ) {
        this.number = number;
        this.category = Objects.requireNonNull(category);
        this.name = Objects.requireNonNull(name);
        this.bankMsb = bankMsb;
        this.bankLsb = bankLsb;
        this.program = program;
        this.kind = Objects.requireNonNull(kind);
        this.sysExCategory = sysExCategory;
        this.sysExNumber = sysExNumber;
    }

    public String stableId() {
        return bankMsb + ":" + bankLsb + ":" + program + ":" + name;
    }
}
