package io.ghostwriter.excluded;

import io.ghostwriter.annotation.Include;


/**
 * Test class used for verifying that excluding packages works.
 * This class should not be traced because its package is excluded via environment settings
 */
@Include // explicitly @Include, just to make sure this does not meddle with the package exclude mechanism
public class ExcludedPackageClass {

    public int meaningOfLife() {
        return 42;
    }

}
