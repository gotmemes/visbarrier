package io.github.gotmemes.visbarrier.compat;

public interface ICompat {
    /** Register command, CTM event handler, etc. */
    void init();

    /** Mark chunks for re-render and send the barrier-visibility chat notification. */
    void onBarriersToggled();
}
