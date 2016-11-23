package io.ghostwriter;

import io.ghostwriter.annotation.Exclude;

/**
 * Test class used for verifying that top-level class exclusion support works.
 * It is important that this class is stored in its own source file and is not nested inside another class.
 */
@Exclude
public class ExcludedTopLevelClass {

    public int meaningOfLife() {
        return 42;
    }

}
