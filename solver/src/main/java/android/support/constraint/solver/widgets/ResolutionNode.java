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

import android.support.constraint.solver.LinearSystem;
import android.support.constraint.solver.SolverVariable;

import java.util.HashSet;

/**
 * Implements a mechanism to resolve nodes via a dependency graph
 */
public class ResolutionNode {
    ConstraintAnchor myAnchor;
    float computedValue;
    ResolutionNode target;
    float offset;

    HashSet<ResolutionNode>  dependents = new HashSet<>(4);
    ResolutionNode resolvedTarget;
    float resolvedOffset;

    int type = UNCONNECTED;

    public static final int UNCONNECTED = 0;
    public static final int DIRECT_CONNECTION = 1;
    public static final int CENTER_CONNECTION = 2;
    public static final int MATCH_CONNECTION = 3;
    public static final int CHAIN_CONNECTION = 4;
    public static final int BARRIER_CONNECTION = 5;

    /**
     * A node has two possible states:
     * - unresolved: basic dependency on the direct target
     * - resolved: resolved to the upmost derivable target (best case, root)
     */
    public static final int UNRESOLVED = 0;
    public static final int RESOLVED = 1;

    int state = UNRESOLVED;
    private ResolutionNode opposite;
    private float oppositeOffset;

    public ResolutionNode(ConstraintAnchor anchor) {
        myAnchor = anchor;
    }

    @Override
    public String toString() {
        if (state == RESOLVED) {
            if (resolvedTarget == this) {
                return "[" + myAnchor + ", RESOLVED: " + resolvedOffset + "]";
            }
            return "[" + myAnchor + ", RESOLVED: " + resolvedTarget + ":" + resolvedOffset + "]";
        }
        return "{ " + myAnchor + " UNRESOLVED}";
    }

    public void didResolve() {
        state = RESOLVED;
        if (ConstraintWidgetContainer.DEBUG_GRAPH) {
            System.out.println("  -> did resolve " + this + " type: " + sType(type));
            for (ResolutionNode node : dependents) {
                System.out.println("    * dependent: " + node);
            }
        }

        for (ResolutionNode node : dependents) {
            node.resolve();
        }
    }

    public void resolve(ResolutionNode target, float offset) {
        resolvedTarget = target;
        resolvedOffset = offset;
        didResolve();
    }

    String sType(int type) {
        if (type == 1) {
            return "DIRECT";
        } else if (type == 2) {
            return "CENTER";
        } else if (type == 3) {
            return "MATCH";
        } else if (type == 4) {
            return "CHAIN";
        } else if (type == 5) {
            return "BARRIER";
        }
        return "UNCONNECTED";
    }

    public void resolve() {
        if (ConstraintWidgetContainer.DEBUG_GRAPH) {
            System.out.println("resolve " + this + " type: " + sType(type)
                    + " current target: " + target + " with offset " + offset);
        }
        if (state == RESOLVED) {
            return;
        }
        if (type == CHAIN_CONNECTION) {
            return;
        }
        if (type == DIRECT_CONNECTION
                && ((target == null) || (target.state == RESOLVED))) {

            // Let's solve direct connections...

            if (target == null) {
                resolvedTarget = this;
                resolvedOffset = offset;
            } else {
                resolvedTarget = target.resolvedTarget;
                resolvedOffset = target.resolvedOffset + offset;
            }
            didResolve();
        } else if (type == CENTER_CONNECTION
                && target != null
                && target.state == RESOLVED
                && opposite != null && opposite.target != null
                && opposite.target.state == RESOLVED) {

            // Let's solve center connections...

            if (LinearSystem.getMetrics() != null) {
                LinearSystem.getMetrics().centerConnectionResolved++;
            }
            resolvedTarget = target.resolvedTarget;
            opposite.resolvedTarget = opposite.target.resolvedTarget;

            float distance = 0;
            float percent = 0.5f;

            if (oppositeOffset > 0) {
                // we are right or bottom
                distance = target.resolvedOffset - opposite.target.resolvedOffset;
            } else {
                distance = opposite.target.resolvedOffset - target.resolvedOffset;
            }

            if (myAnchor.mType == ConstraintAnchor.Type.LEFT
                    || myAnchor.mType == ConstraintAnchor.Type.RIGHT) {
                distance -= myAnchor.mOwner.getWidth();
                percent = myAnchor.mOwner.mHorizontalBiasPercent;
            } else {
                distance -= myAnchor.mOwner.getHeight();
                percent = myAnchor.mOwner.mVerticalBiasPercent;
            }
            int margin = myAnchor.getMargin();
            int oppositeMargin = opposite.myAnchor.getMargin();
            if (myAnchor.getTarget() == opposite.myAnchor.getTarget()) {
                percent = 0.5f;
                margin = 0;
                oppositeMargin = 0;
            }

            distance -= margin;
            distance -= oppositeMargin;

            if (oppositeOffset > 0) {
                // we are right or bottom
                opposite.resolvedOffset = opposite.target.resolvedOffset
                        + oppositeMargin + distance * percent;
                resolvedOffset = target.resolvedOffset - margin - (distance * (1 - percent));
            } else {
                resolvedOffset = target.resolvedOffset + margin + distance * percent;
                opposite.resolvedOffset = opposite.target.resolvedOffset
                        - oppositeMargin - (distance * (1 - percent));
            }

            didResolve();
            opposite.didResolve();
        } else if (type == MATCH_CONNECTION
                && target != null
                && target.state == RESOLVED
                && opposite != null && opposite.target != null
                && opposite.target.state == RESOLVED) {

            // Let's solve match connections...

            if (LinearSystem.getMetrics() != null) {
                LinearSystem.getMetrics().matchConnectionResolved++;
            }
            resolvedTarget = target.resolvedTarget;
            opposite.resolvedTarget = opposite.target.resolvedTarget;

            resolvedOffset = target.resolvedOffset + offset;
            opposite.resolvedOffset = opposite.target.resolvedOffset + opposite.offset;

            didResolve();
            opposite.didResolve();
        } else if (type == BARRIER_CONNECTION) {
            myAnchor.mOwner.resolve();
        }
    }

    // First pass we build a graph of ResolutionNode

    public void setType(int type) {
        this.type = type;
    }

    public void addDependent(ResolutionNode node) {
        dependents.add(node);
    }

    public void resetResolution() {
        state = UNRESOLVED;
        resolvedTarget = null;
        resolvedOffset = 0;
    }

    public void reset() {
        target = null;
        offset = 0;
        dependents.clear();
        resolvedTarget = null;
        resolvedOffset = 0;
        computedValue = 0;
        opposite = null;
        oppositeOffset = 0;
        type = UNCONNECTED;
        state = UNRESOLVED;
    }

    public void update() {
        ConstraintAnchor targetAnchor = myAnchor.getTarget();
        if (targetAnchor == null) {
            return;
        }
        if (targetAnchor.getTarget() == myAnchor) {
            type = CHAIN_CONNECTION;
            targetAnchor.getResolutionNode().type = CHAIN_CONNECTION;
        }
        int margin = myAnchor.getMargin();
        if (myAnchor.mType == ConstraintAnchor.Type.RIGHT
                || myAnchor.mType == ConstraintAnchor.Type.BOTTOM) {
            margin = -margin;
        }
        dependsOn(targetAnchor.getResolutionNode(), margin);
    }

    public void useAnchor(ConstraintAnchor anchor) {
        ResolutionNode node = anchor.getResolutionNode();
    }

    public void dependsOn(int type, ResolutionNode node, int offset) {
        this.type  = type;
        target = node;
        this.offset = offset;
        target.addDependent(this);
        if (ConstraintWidgetContainer.DEBUG_GRAPH) {
            System.out.println("a- " + this + " DEPENDS [" + sType(type) + "] ON " + node + " WITH OFFSET " + offset);
        }
    }

    public void dependsOn(ResolutionNode node, int offset) {
        target = node;
        this.offset = offset;
        target.addDependent(this);
        if (ConstraintWidgetContainer.DEBUG_GRAPH) {
            System.out.println("b- " + this + " DEPENDS [" + sType(type) + "] ON " + node + " WITH OFFSET " + offset);
        }
    }

    public void setOpposite(ResolutionNode opposite, float oppositeOffset) {
        this.opposite = opposite;
        this.oppositeOffset = oppositeOffset;
    }

    void addResolvedValue(LinearSystem system) {
        SolverVariable sv = myAnchor.getSolverVariable();

        if (resolvedTarget == null) {
            system.addEquality(sv, (int) resolvedOffset);
        } else {
            SolverVariable v = system.createObjectVariable(resolvedTarget.myAnchor);
            system.addEquality(sv, v, (int) resolvedOffset, SolverVariable.STRENGTH_FIXED);
        }
    }
}
