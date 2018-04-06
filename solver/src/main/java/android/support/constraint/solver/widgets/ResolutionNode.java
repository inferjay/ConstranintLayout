/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.support.constraint.solver.widgets;

import java.util.HashSet;

/**
 * Root class for Resolution nodes used by the direct solver.
 */
public class ResolutionNode {
    HashSet<ResolutionNode> dependents = new HashSet<>(2);

    /**
     * A node has two possible states:
     * - unresolved: basic dependency on the direct target
     * - resolved: resolved to the upmost derivable target (best case, root)
     */
    public static final int UNRESOLVED = 0;
    public static final int RESOLVED = 1;
    public static final int REMOVED = 2;

    int state = UNRESOLVED;

    public void addDependent(ResolutionNode node) {
        dependents.add(node);
    }

    public void reset() {
        state = UNRESOLVED;
        dependents.clear();
    }

    public void invalidate() {
        state = UNRESOLVED;
        for (ResolutionNode node : dependents) {
            node.invalidate();
        }
    }

    public void invalidateAnchors() {
        if (this instanceof ResolutionAnchor) {
            state = UNRESOLVED;
        }
        for (ResolutionNode node : dependents) {
            node.invalidateAnchors();
        }
    }

    public void didResolve() {
        state = RESOLVED;
        if (ConstraintWidgetContainer.DEBUG_GRAPH) {
            System.out.println("  -> did resolve " + this);
            for (ResolutionNode node : dependents) {
                System.out.println("    * dependent: " + node);
            }
        }
        for (ResolutionNode node : dependents) {
            node.resolve();
        }
    }

    public boolean isResolved() {
        return state == RESOLVED;
    }

    public void resolve() {
        // do nothing
    }

    public void remove(ResolutionDimension resolutionDimension) {
        // do nothing
    }
}
