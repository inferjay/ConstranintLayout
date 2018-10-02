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
import android.support.constraint.solver.widgets.ConstraintAnchor.Type;

/**
 * Implements a mechanism to resolve nodes via a dependency graph
 */
public class ResolutionAnchor extends ResolutionNode {
    ConstraintAnchor myAnchor;
    float computedValue;
    ResolutionAnchor target;
    float offset;

    ResolutionAnchor resolvedTarget;
    float resolvedOffset;

    int type = UNCONNECTED;

    public static final int UNCONNECTED = 0;
    public static final int DIRECT_CONNECTION = 1;
    public static final int CENTER_CONNECTION = 2;
    public static final int MATCH_CONNECTION = 3;
    public static final int CHAIN_CONNECTION = 4;
    public static final int BARRIER_CONNECTION = 5;

    private ResolutionAnchor opposite;
    private float oppositeOffset;

    private ResolutionDimension dimension = null;
    private int dimensionMultiplier = 1;
    private ResolutionDimension oppositeDimension = null;
    private int oppositeDimensionMultiplier = 1;

    public ResolutionAnchor(ConstraintAnchor anchor) {
        myAnchor = anchor;
    }

    public void remove(ResolutionDimension resolutionDimension) {
        if (dimension == resolutionDimension) {
            dimension = null;
            offset = dimensionMultiplier;
        } else if (dimension == oppositeDimension) {
            oppositeDimension = null;
            oppositeOffset = oppositeDimensionMultiplier;
        }
        resolve();
    }

    @Override
    public String toString() {
        if (state == RESOLVED) {
            if (resolvedTarget == this) {
                return "[" + myAnchor + ", RESOLVED: " + resolvedOffset + "] " +  " type: " + sType(type);
            }
            return "[" + myAnchor + ", RESOLVED: " + resolvedTarget + ":" + resolvedOffset + "]"
                    + " type: " + sType(type);
        }
        return "{ " + myAnchor + " UNRESOLVED} type: " + sType(type);
    }

    public void resolve(ResolutionAnchor target, float offset) {
        if (state == UNRESOLVED || (resolvedTarget != target && resolvedOffset != offset)) {
            resolvedTarget = target;
            resolvedOffset = offset;
            if (state == RESOLVED) {
                invalidate();
            }
            didResolve();
        }
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

    @Override
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
        if (dimension != null) {
            if (dimension.state != RESOLVED) {
                return;
            }
            offset = dimensionMultiplier * dimension.value;
        }
        if (oppositeDimension != null) {
            if (oppositeDimension.state != RESOLVED) {
                return;
            }
            oppositeOffset = oppositeDimensionMultiplier * oppositeDimension.value;
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

            boolean isEndAnchor = myAnchor.mType == Type.RIGHT || myAnchor.mType == Type.BOTTOM;

            if (isEndAnchor) {
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

            if (isEndAnchor) {
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

    public void setType(int type) {
        this.type = type;
    }

    @Override
    public void reset() {
        super.reset();
        target = null;
        offset = 0;
        dimension = null;
        dimensionMultiplier = 1;
        oppositeDimension = null;
        oppositeDimensionMultiplier = 1;
        resolvedTarget = null;
        resolvedOffset = 0;
        computedValue = 0;
        opposite = null;
        oppositeOffset = 0;
        type = UNCONNECTED;
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

    public void dependsOn(int type, ResolutionAnchor node, int offset) {
        this.type  = type;
        target = node;
        this.offset = offset;
        target.addDependent(this);
        if (ConstraintWidgetContainer.DEBUG_GRAPH) {
            System.out.println("a- " + this + " DEPENDS [" + sType(type) + "] ON " + node + " WITH OFFSET " + offset);
        }
    }

    public void dependsOn(ResolutionAnchor node, int offset) {
        target = node;
        this.offset = offset;
        target.addDependent(this);
        if (ConstraintWidgetContainer.DEBUG_GRAPH) {
            System.out.println("b- " + this + " DEPENDS [" + sType(type) + "] ON " + node + " WITH OFFSET " + offset);
        }
    }

    public void dependsOn(ResolutionAnchor node, int multiplier, ResolutionDimension dimension) {
        target = node;
        target.addDependent(this);
        this.dimension = dimension;
        this.dimensionMultiplier = multiplier;
        this.dimension.addDependent(this);

        if (ConstraintWidgetContainer.DEBUG_GRAPH) {
            System.out.println("c- " + this + " DEPENDS [" + sType(type) + "] ON " + node + " WITH DIMENSION " + dimension);
        }
    }

    public void setOpposite(ResolutionAnchor opposite, float oppositeOffset) {
        this.opposite = opposite;
        this.oppositeOffset = oppositeOffset;
    }

    public void setOpposite(ResolutionAnchor opposite, int multiplier, ResolutionDimension dimension) {
        this.opposite = opposite;
        this.oppositeDimension = dimension;
        this.oppositeDimensionMultiplier = multiplier;
    }

    void addResolvedValue(LinearSystem system) {
        SolverVariable sv = myAnchor.getSolverVariable();

        if (resolvedTarget == null) {
            system.addEquality(sv, (int) (resolvedOffset + 0.5f));
        } else {
            SolverVariable v = system.createObjectVariable(resolvedTarget.myAnchor);
            system.addEquality(sv, v, (int) (resolvedOffset + 0.5f), SolverVariable.STRENGTH_FIXED);
        }
    }

    public float getResolvedValue() {
        return resolvedOffset;
    }
}
