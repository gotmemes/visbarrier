package io.github.gotmemes.visbarrier;

// Stub — root copy wins at jar merge via DuplicatesStrategy.EXCLUDE.
// Exists only so compat/v1_12 code can compile against this FQN.
public final class VisbarrierState {
    private VisbarrierState() {}

    public static boolean barriersVisible      = false;
    public static boolean connectedTextures    = true;
    public static boolean keybindNotifications = true;
}
