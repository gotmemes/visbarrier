package io.github.gotmemes.visbarrier;

public final class VisbarrierState {
    private VisbarrierState() {}

    // volatile — written on the client input thread, read on the render thread.
    public static volatile boolean barriersVisible      = false;
    public static volatile boolean connectedTextures    = true;
    public static volatile boolean keybindNotifications = true;
}
