package io.ghostwriter;

import io.ghostwriter.annotation.Include;

/**
 * Test class used for verifying that excluding classes via environment works.
 * This class should not be traced because it is excluded via environment settings
 */
@Include // explicitly @Include, just to make sure this does not meddle with the env exclude mechanism
public class ExcludedByEnvironmentClass {

    public int meaningOfLife() {
        return 42;
    }

}