/*
 * Copyright (C) 2017 The Android Open Source Project
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

import java.util.ArrayList;
import java.util.Arrays;

/**
 * A Barrier takes multiple widgets
 */
public class Barrier extends Helper {

    public static final int LEFT = 0;
    public static final int RIGHT = 1;
    public static final int TOP = 2;
    public static final int BOTTOM = 3;

    private int mBarrierType = LEFT;
    private ArrayList<ResolutionAnchor> mNodes = new ArrayList<>(4);

    private boolean mAllowsGoneWidget = true;

    @Override
    public boolean allowedInBarrier() {
        return true;
    }

    public void setBarrierType(int barrierType) {
        mBarrierType = barrierType;
    }

    public void setAllowsGoneWidget(boolean allowsGoneWidget) { mAllowsGoneWidget = allowsGoneWidget; }

    public boolean allowsGoneWidget() { return mAllowsGoneWidget; }

    @Override
    public void resetResolutionNodes() {
        super.resetResolutionNodes();
        mNodes.clear();
    }

    /**
     * Graph analysis
     * @param optimizationLevel
     */
    @Override
    public void analyze(int optimizationLevel) {
        if (mParent == null) {
            return;
        }
        if (!((ConstraintWidgetContainer) mParent).optimizeFor(Optimizer.OPTIMIZATION_BARRIER)) {
            return;
        }

        ResolutionAnchor node;
        switch (mBarrierType) {
            case LEFT:
                node = mLeft.getResolutionNode();
            break;
            case RIGHT:
                node = mRight.getResolutionNode();
            break;
            case TOP:
                node = mTop.getResolutionNode();
            break;
            case BOTTOM:
                node = mBottom.getResolutionNode();
            break;
            default:
                return;
        }
        node.setType(ResolutionAnchor.BARRIER_CONNECTION);

        if (mBarrierType == LEFT || mBarrierType == RIGHT) {
            mTop.getResolutionNode().resolve(null, 0);
            mBottom.getResolutionNode().resolve(null, 0);
        } else {
            mLeft.getResolutionNode().resolve(null, 0);
            mRight.getResolutionNode().resolve(null, 0);
        }

        mNodes.clear();
        for (int i = 0; i < mWidgetsCount; i++) {
            ConstraintWidget widget = mWidgets[i];
            if (!mAllowsGoneWidget && !widget.allowedInBarrier()) {
                continue;
            }
            ResolutionAnchor depends = null;
            switch (mBarrierType) {
                case LEFT:
                    depends = widget.mLeft.getResolutionNode();
                break;
                case RIGHT:
                    depends = widget.mRight.getResolutionNode();
                break;
                case TOP:
                    depends = widget.mTop.getResolutionNode();
                break;
                case BOTTOM:
                    depends = widget.mBottom.getResolutionNode();
                break;
            }
            if (depends != null) {
                mNodes.add(depends);
                depends.addDependent(node);
            }
        }
    }

    /**
     * Try resolving the graph analysis
     */
    @Override
    public void resolve() {
        ResolutionAnchor node = null;
        float value = 0;
        switch (mBarrierType) {
            case LEFT: {
                node = mLeft.getResolutionNode();
                value = Float.MAX_VALUE;
            } break;
            case RIGHT: {
                node = mRight.getResolutionNode();
            } break;
            case TOP: {
                node = mTop.getResolutionNode();
                value = Float.MAX_VALUE;
            } break;
            case BOTTOM: {
                node = mBottom.getResolutionNode();
            } break;
            default:
                return;
        }

        final int count = mNodes.size();
        ResolutionAnchor resolvedTarget = null;
        for (int i = 0; i < count; i++) {
            ResolutionAnchor n = mNodes.get(i);
            if (n.state != ResolutionAnchor.RESOLVED) {
                return;
            }
            if (mBarrierType == LEFT || mBarrierType == TOP) {
                if (n.resolvedOffset < value) {
                    value = n.resolvedOffset;
                    resolvedTarget = n.resolvedTarget;
                }
            } else {
                if (n.resolvedOffset > value) {
                    value = n.resolvedOffset;
                    resolvedTarget = n.resolvedTarget;
                }
            }
        }

        if (LinearSystem.getMetrics() != null) {
            LinearSystem.getMetrics().barrierConnectionResolved++;
        }

        if (ConstraintWidgetContainer.DEBUG_GRAPH) {
            System.out.println("  * barrier resolved to " + resolvedTarget + " : " + value);
        }
        node.resolvedTarget = resolvedTarget;
        node.resolvedOffset = value;
        node.didResolve();
        switch (mBarrierType) {
            case LEFT: {
                mRight.getResolutionNode().resolve(resolvedTarget, value);
            } break;
            case RIGHT: {
                mLeft.getResolutionNode().resolve(resolvedTarget, value);
            } break;
            case TOP: {
                mBottom.getResolutionNode().resolve(resolvedTarget, value);
            } break;
            case BOTTOM: {
                mTop.getResolutionNode().resolve(resolvedTarget, value);
            } break;
            default:
                return;
        }
    }

    /**
     * Add this widget to the solver
     *
     * @param system the solver we want to add the widget to
     */
    @Override
    public void addToSolver(LinearSystem system) {
        ConstraintAnchor position;
        mListAnchors[LEFT] = mLeft;
        mListAnchors[TOP] = mTop;
        mListAnchors[RIGHT] = mRight;
        mListAnchors[BOTTOM] = mBottom;
        for (int i = 0; i < mListAnchors.length; i++) {
            mListAnchors[i].mSolverVariable = system.createObjectVariable(mListAnchors[i]);
        }
        if (mBarrierType >= 0 && mBarrierType < 4) {
            position = mListAnchors[mBarrierType];
        } else {
            return;
        }
        // We have to handle the case where some of the elements referenced in the barrier are set as
        // match_constraint; we have to take it in account to set the strength of the barrier.
        boolean hasMatchConstraintWidgets = false;
        for (int i = 0; i < mWidgetsCount; i++) {
            ConstraintWidget widget = mWidgets[i];
            if (!mAllowsGoneWidget && !widget.allowedInBarrier()) {
                continue;
            }
            if ((mBarrierType == LEFT || mBarrierType == RIGHT)
                && widget.getHorizontalDimensionBehaviour() == DimensionBehaviour.MATCH_CONSTRAINT) {
                hasMatchConstraintWidgets = true;
                break;
            } else if ((mBarrierType == TOP || mBarrierType == BOTTOM)
                    && widget.getVerticalDimensionBehaviour() == DimensionBehaviour.MATCH_CONSTRAINT) {
                hasMatchConstraintWidgets = true;
                break;
            }
        }
        if ((mBarrierType == LEFT || mBarrierType == RIGHT)) {
            if (getParent().getHorizontalDimensionBehaviour() == DimensionBehaviour.WRAP_CONTENT) {
                hasMatchConstraintWidgets = false;
            }
        } else {
            if (getParent().getVerticalDimensionBehaviour() == DimensionBehaviour.WRAP_CONTENT) {
                hasMatchConstraintWidgets = false;
            }
        }
        for (int i = 0; i < mWidgetsCount; i++) {
            ConstraintWidget widget = mWidgets[i];
            if (!mAllowsGoneWidget && !widget.allowedInBarrier()) {
                continue;
            }
            SolverVariable target = system.createObjectVariable(widget.mListAnchors[mBarrierType]);
            widget.mListAnchors[mBarrierType].mSolverVariable = target;
            if (mBarrierType == LEFT || mBarrierType == TOP) {
                system.addLowerBarrier(position.mSolverVariable, target, hasMatchConstraintWidgets);
            } else {
                system.addGreaterBarrier(position.mSolverVariable, target, hasMatchConstraintWidgets);
            }
        }

        if (mBarrierType == LEFT) {
            system.addEquality(mRight.mSolverVariable, mLeft.mSolverVariable, 0, SolverVariable.STRENGTH_FIXED);
            if (!hasMatchConstraintWidgets) {
                system.addEquality(mLeft.mSolverVariable, mParent.mRight.mSolverVariable, 0, SolverVariable.STRENGTH_EQUALITY);
            }
        } else if (mBarrierType == RIGHT) {
            system.addEquality(mLeft.mSolverVariable, mRight.mSolverVariable, 0, SolverVariable.STRENGTH_FIXED);
            if (!hasMatchConstraintWidgets) {
                system.addEquality(mLeft.mSolverVariable, mParent.mLeft.mSolverVariable, 0, SolverVariable.STRENGTH_EQUALITY);
            }
        } else if (mBarrierType == TOP) {
            system.addEquality(mBottom.mSolverVariable, mTop.mSolverVariable, 0, SolverVariable.STRENGTH_FIXED);
            if (!hasMatchConstraintWidgets) {
                system.addEquality(mTop.mSolverVariable, mParent.mBottom.mSolverVariable, 0, SolverVariable.STRENGTH_EQUALITY);
            }
        } else if (mBarrierType == BOTTOM) {
            system.addEquality(mTop.mSolverVariable, mBottom.mSolverVariable, 0, SolverVariable.STRENGTH_FIXED);
            if (!hasMatchConstraintWidgets) {
                system.addEquality(mTop.mSolverVariable, mParent.mTop.mSolverVariable, 0, SolverVariable.STRENGTH_EQUALITY);
            }
        }
    }

}
