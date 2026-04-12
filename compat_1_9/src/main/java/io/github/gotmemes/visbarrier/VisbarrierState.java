package io.github.gotmemes.visbarrier;

// Stub — root copy wins at jar merge via DuplicatesStrategy.EXCLUDE.
// Exists only so compat/v1_9 code can compile against this FQN.
public final class VisbarrierState {
    private VisbarrierState() {}

    public static volatile boolean barriersVisible      = false;
    public static volatile boolean connectedTextures    = true;
    public static volatile boolean keybindNotifications = true;
}
