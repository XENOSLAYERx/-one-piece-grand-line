package com.nerodev.grandline.command;

import com.nerodev.grandline.permission.PermissionTier;

/**
 * One line of {@code /onepiece help}. Only commands that actually exist get an entry, and players only see the
 * entries their tier can run.
 *
 * @param usage       full usage, e.g. {@code /onepiece debug fruit [id]}
 * @param description one-line explanation
 * @param tier        minimum tier needed to run it
 */
public record HelpEntry(String usage, String description, PermissionTier tier) {
    /** The part of the usage before the first argument placeholder, used for click-to-type. */
    public String commandPrefix() {
        int cut = usage.length();
        for (char marker : new char[] {'<', '['}) {
            int index = usage.indexOf(marker);
            if (index >= 0 && index < cut) {
                cut = index;
            }
        }
        String prefix = usage.substring(0, cut).trim();
        return cut < usage.length() ? prefix + " " : prefix;
    }
}
