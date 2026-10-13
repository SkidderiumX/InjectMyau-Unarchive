package myau.module.modules;

import myau.Myau;
import myau.module.Module;
import myau.property.properties.ModeProperty;

/**
 * Ported from Raven B4 CommandChat.
 * Selects the chat prefix that triggers client commands.
 * Falls back to "." while disabled so commands keep working.
 */
public class CommandChat extends Module {
    private static final String[] IDENTIFIERS = new String[]{".", "#", "@"};
    public final ModeProperty identifier = new ModeProperty("Identifier", 0, IDENTIFIERS);

    public CommandChat() {
        super("Command Chat", true);
    }

    public static String getIdentifier() {
        try {
            CommandChat module =
                    (CommandChat) Myau.moduleManager.modules.get(CommandChat.class);
            if (module != null && module.isEnabled()) {
                int index = module.identifier.getValue();
                if (index >= 0 && index < IDENTIFIERS.length) {
                    return IDENTIFIERS[index];
                }
            }
        } catch (Throwable ignored) {
        }
        return ".";
    }
}
