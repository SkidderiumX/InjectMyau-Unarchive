package myau.command.commands.devkey;

/**
 * Developer keys for the hidden debug command. The keys live here and stay
 * private; callers can only ask whether an entered key matches one of them,
 * so the compiled command never embeds the key it compares against.
 */
public final class DevKeys {
    private static final String[] KEYS = {
            "devkey_aTF0MnozYTRwNXA2bDdlOA======",
            "devkey_JF2HUQLQOBWGKMJSGM2DKNRXHA======",
            "devkey_pXIjx9dBpgPvgCtqjBCJ2n5EB9======="
    };

    private DevKeys() {
    }

    /**
     * True when the entered text matches one of the dev keys.
     */
    public static boolean matches(String entered) {
        for (String key : KEYS) {
            if (key.equals(entered)) {
                return true;
            }
        }
        return false;
    }
}
