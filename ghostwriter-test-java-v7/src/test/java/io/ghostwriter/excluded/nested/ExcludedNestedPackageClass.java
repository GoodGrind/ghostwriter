package io.ghostwriter.excluded.nested;

import io.ghostwriter.annotation.Include;


/**
 * Test class used for verifying that excluding packages works, even in nested packages.
 * This class should not be traced because its parent package is excluded via environment settings
 */
@Include // explicitly @Include, just to make sure this does not meddle with the package exclude mechanism
public class ExcludedNestedPackageClass {

    public int meaningOfLife() {
        return 42;
    }

}
