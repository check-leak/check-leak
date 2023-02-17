package io.github.checkleak.junitexample;

import io.github.checkleak.assertj.CheckLeakAssert;
import io.github.checkleak.sample.SomeClass;
import org.junit.jupiter.api.Test;

public class AssertJTest {

    @Test
    void testAssertJ() throws Exception {

        // I am keeping a reference live
        SomeClass someObject = new SomeClass();

        // I am starting the CheckLeak assertion API
        CheckLeakAssert assertions = CheckLeakAssert.create();

        // I'm checking if there are references. On this case I know I should have one object live, so I'm checking for 1
        assertions.assertThatAllObjectsOf(SomeClass.class).hasSize(1);

        assertions.assertThatObjectReferences(10, 10, true, someObject).isNotBlank();

        // Now I am clearing the reference
        someObject = null;

        // I'm checking again from JVMTIInterface, if all references are gone. Notice that getAllObjects will force a garbage collection on every call
        assertions.assertThatAllObjectsOf(SomeClass.class).isEmpty();

        // Check that inventory is not empty
        assertions.assertThatInventory().isNotEmpty();
    }
}
