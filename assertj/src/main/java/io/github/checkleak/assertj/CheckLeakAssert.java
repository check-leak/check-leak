package io.github.checkleak.assertj;

import io.github.checkleak.core.CheckLeak;
import org.assertj.core.api.MapAssert;
import org.assertj.core.api.ObjectArrayAssert;
import org.assertj.core.api.StringAssert;

import java.io.IOException;

public class CheckLeakAssert {

    private final CheckLeak checkLeak;

    public CheckLeakAssert(CheckLeak checkLeak) {
        this.checkLeak = checkLeak;
    }

    public static CheckLeakAssert create() {
        CheckLeak checkLeak = new CheckLeak();
        return new CheckLeakAssert(checkLeak);
    }

    public ObjectArrayAssert<Object> assertThatAllObjectsOf(Class<?> type) {
        return new ObjectArrayAssert<>(checkLeak.getAllObjects(type));
    }

    public ObjectArrayAssert<Object> assertThatAllObjectsOf(String type) {
        return new ObjectArrayAssert<>(checkLeak.getAllObjects(type));
    }

    public ObjectArrayAssert<Class<?>> assertThatAllClassesOf(String className) {
        return new ObjectArrayAssert<>(checkLeak.getAllClasses(className));
    }

    public StringAssert assertThatObjectReferences(int maxLevel, int maxObjects, boolean useToString, Object... references)
            throws Exception {
        String objectReferences = checkLeak.exploreObjectReferences(maxLevel, maxObjects, useToString, references);
        return new StringAssert(objectReferences);
    }

    public MapAssert assertThatInventory() throws IOException {
        return new MapAssert(checkLeak.produceInventory());
    }
}
