package dev.idlesign.fp30xcompanion;

public enum ToneFamily {
    ALL,
    FAVORITES,
    ACOUSTIC_PIANOS,
    ELECTRIC_PIANOS,
    HARPSICHORDS_CLAVINETS,
    MALLETS_BELLS,
    ORGANS_ACCORDIONS,
    GUITARS,
    BASSES,
    SOLO_STRINGS_HARPS,
    ENSEMBLES_CHOIRS_ORCHESTRA,
    BRASS,
    SAX_REEDS,
    FLUTES_PIPES,
    SYNTH_LEADS,
    SYNTH_PADS,
    ATMOSPHERES_TEXTURES,
    WORLD_INSTRUMENTS,
    PERCUSSION,
    DRUM_KITS,
    ALL_SOUND_EFFECTS,
    INSTRUMENT_PERFORMANCE_NOISES,
    NATURE,
    ANIMALS,
    HOUSEHOLD_SIGNALS,
    TRANSPORT_MACHINERY,
    PEOPLE_BODY,
    SHOTS_EXPLOSIONS;

    public boolean matches(Tone tone) {
        if (this == ALL) {
            return true;
        }
        if (this == FAVORITES) {
            return false;
        }
        if (this == DRUM_KITS) {
            return "Drums".equals(tone.category);
        }
        if ("Drums".equals(tone.category)) {
            return false;
        }
        int program = tone.program;
        return switch (this) {
            case ACOUSTIC_PIANOS -> in(program, 1, 4);
            case ELECTRIC_PIANOS -> in(program, 5, 6);
            case HARPSICHORDS_CLAVINETS -> in(program, 7, 8);
            case MALLETS_BELLS -> in(program, 9, 16);
            case ORGANS_ACCORDIONS -> in(program, 17, 24);
            case GUITARS -> in(program, 25, 32);
            case BASSES -> in(program, 33, 40);
            case SOLO_STRINGS_HARPS -> in(program, 41, 48);
            case ENSEMBLES_CHOIRS_ORCHESTRA -> in(program, 49, 56);
            case BRASS -> in(program, 57, 64);
            case SAX_REEDS -> in(program, 65, 72);
            case FLUTES_PIPES -> in(program, 73, 80);
            case SYNTH_LEADS -> in(program, 81, 88);
            case SYNTH_PADS -> in(program, 89, 96);
            case ATMOSPHERES_TEXTURES -> in(program, 97, 104);
            case WORLD_INSTRUMENTS -> in(program, 105, 112);
            case PERCUSSION -> in(program, 113, 120);
            case ALL_SOUND_EFFECTS -> in(program, 121, 128);
            case INSTRUMENT_PERFORMANCE_NOISES -> in(program, 121, 122);
            case NATURE -> program == 123;
            case ANIMALS -> program == 124;
            case HOUSEHOLD_SIGNALS -> program == 125;
            case TRANSPORT_MACHINERY -> program == 126;
            case PEOPLE_BODY -> program == 127;
            case SHOTS_EXPLOSIONS -> program == 128;
            default -> false;
        };
    }

    private static boolean in(int value, int minimum, int maximum) {
        return value >= minimum && value <= maximum;
    }
}
