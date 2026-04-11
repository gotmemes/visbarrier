package io.github.gotmemes.visbarrier.compat;

// Stub — root copy wins at jar merge via DuplicatesStrategy.EXCLUDE.
// Exists only so compat/v1_12 code can compile against this FQN.
public interface ICompat {
    void init();
    void onBarriersToggled();
}
