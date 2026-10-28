package myau.command.commands;

import myau.Myau;
import myau.command.Command;
import myau.command.commands.devkey.DevKeys;
import myau.util.ChatUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Hidden developer command. Nothing is printed until the session dev key is
 * entered; the unlocked state and enabled debug modes are session-only and
 * reset when the game closes or the client gets uninjected.
 *
 * .debug                     -> nothing
 * .debug <devkey>            -> unlocks debug commands silently
 * .debug enable <mode>       -> Enabled   (KeyBindDebug / RotsDebug / PlacementDebug)
 * .debug disable <mode>      -> Disabled (one mode)
 * .debug disable all         -> Disable all modes
 * .debug clearsession        -> Cleared  (drops the dev key session)
 */
public class DebugCommand extends Command {
    private static final String[] MODES = {"KeyBindDebug", "RotsDebug", "PlacementDebug"};

    private static boolean unlocked = false;
    private static final HashSet<String> enabledModes = new HashSet<String>();

    public DebugCommand() {
        super(new ArrayList<>(Arrays.asList("debug")));
    }

    public static boolean isModeEnabled(String mode) {
        return unlocked && enabledModes.contains(mode);
    }

    private static String canonical(String mode) {
        for (String candidate : MODES) {
            if (candidate.equalsIgnoreCase(mode)) {
                return candidate;
            }
        }
        return null;
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (args.size() < 2) {
            return;
        }
        String sub = args.get(1);
        if (DevKeys.matches(sub)) {
            unlocked = true;
            enabledModes.clear();
            return;
        }
        if (!unlocked) {
            return;
        }
        if (sub.equalsIgnoreCase("clearsession")) {
            unlocked = false;
            enabledModes.clear();
            ChatUtil.sendFormatted(String.format("%sCleared", Myau.clientName));
            return;
        }
        if (sub.equalsIgnoreCase("enable") && args.size() >= 3) {
            String mode = canonical(args.get(2));
            if (mode != null) {
                enabledModes.add(mode);
                ChatUtil.sendFormatted(String.format("%sEnabled", Myau.clientName));
            }
            return;
        }
        if (sub.equalsIgnoreCase("disable") && args.size() >= 3) {
            if (args.get(2).equalsIgnoreCase("all")) {
                enabledModes.clear();
                ChatUtil.sendFormatted(String.format("%sDisable all modes", Myau.clientName));
            } else {
                String mode = canonical(args.get(2));
                if (mode != null) {
                    enabledModes.remove(mode);
                    ChatUtil.sendFormatted(String.format("%sDisabled", Myau.clientName));
                }
            }
        }
    }
}
