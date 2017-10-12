/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static android.support.constraint.solver.widgets.ConstraintWidget.*;

/**
 * Implements direct resolution without using the solver
 */
public class Optimizer {

    // Optimization levels
    public static final int OPTIMIZATION_NONE = 1;
    public static final int OPTIMIZATION_ALL = 2;
    public static final int OPTIMIZATION_BASIC = 4;
    public static final int OPTIMIZATION_CHAIN = 8;

    // Internal use.
    static boolean[] flags = new boolean[3];
    static final int FLAG_USE_OPTIMIZE = 0; // simple enough to use optimizer
    static final int FLAG_CHAIN_DANGLING = 1;
    static final int FLAG_RECOMPUTE_BOUNDS = 2;

    private static final boolean DEBUG_OPTIMIZE = false;
    private static final boolean TRACE_OPTIMIZE = false;

    static void checkMatchParent(ConstraintWidgetContainer container, LinearSystem system, ConstraintWidget widget) {
        if (container.mListDimensionBehaviors[DIMENSION_HORIZONTAL] != ConstraintWidget.DimensionBehaviour.WRAP_CONTENT
            && widget.mListDimensionBehaviors[DIMENSION_HORIZONTAL] == ConstraintWidget.DimensionBehaviour.MATCH_PARENT) {
            widget.mLeft.mSolverVariable = system.createObjectVariable(widget.mLeft);
            widget.mRight.mSolverVariable = system.createObjectVariable(widget.mRight);
            int left = widget.mLeft.mMargin;
            int right = container.getWidth() - widget.mRight.mMargin;
            system.addEquality(widget.mLeft.mSolverVariable, left);
            system.addEquality(widget.mRight.mSolverVariable, right);
            widget.setHorizontalDimension(left, right);
            widget.mHorizontalResolution = ConstraintWidget.DIRECT;
        }
        if (container.mListDimensionBehaviors[DIMENSION_VERTICAL] != ConstraintWidget.DimensionBehaviour.WRAP_CONTENT
            && widget.mListDimensionBehaviors[DIMENSION_VERTICAL] == ConstraintWidget.DimensionBehaviour.MATCH_PARENT) {
            widget.mTop.mSolverVariable = system.createObjectVariable(widget.mTop);
            widget.mBottom.mSolverVariable = system.createObjectVariable(widget.mBottom);
            int top = widget.mTop.mMargin;
            int bottom = container.getHeight() - widget.mBottom.mMargin;
            system.addEquality(widget.mTop.mSolverVariable, top);
            system.addEquality(widget.mBottom.mSolverVariable, bottom);
            if (widget.mBaselineDistance > 0 || widget.getVisibility() == ConstraintWidget.GONE) {
                widget.mBaseline.mSolverVariable = system.createObjectVariable(widget.mBaseline);
                system.addEquality(widget.mBaseline.mSolverVariable, top + widget.mBaselineDistance);
            }
            widget.setVerticalDimension(top, bottom);
            widget.mVerticalResolution = ConstraintWidget.DIRECT;
        }
    }

    /**
     * Optimize the given system (try to do direct resolutions)
     *
     * @param system
     * @return
     */
    static boolean optimize(LinearSystem system, ConstraintWidgetContainer container) {
        final int count = container.mChildren.size();

        boolean horizontalParentWrap = container.mListDimensionBehaviors[ConstraintWidget.DIMENSION_HORIZONTAL] == DimensionBehaviour.WRAP_CONTENT;
        boolean verticalParentWrap = container.mListDimensionBehaviors[ConstraintWidget.DIMENSION_VERTICAL] == DimensionBehaviour.WRAP_CONTENT;

        for (int i = 0; i < count; i++) {
            ConstraintWidget widget = container.mChildren.get(i);
            if (horizontalParentWrap) {
                widget.mLeft.resolutionStatus = ConstraintAnchor.SOLVER;
                widget.mRight.resolutionStatus = ConstraintAnchor.SOLVER;
            } else {
                widget.mLeft.resolutionStatus = ConstraintAnchor.UNRESOLVED;
                widget.mRight.resolutionStatus = ConstraintAnchor.UNRESOLVED;
            }
            if (verticalParentWrap) {
                widget.mTop.resolutionStatus = ConstraintAnchor.SOLVER;
                widget.mBottom.resolutionStatus = ConstraintAnchor.SOLVER;
                widget.mBaseline.resolutionStatus = ConstraintAnchor.SOLVER;
            } else {
                widget.mTop.resolutionStatus = ConstraintAnchor.UNRESOLVED;
                widget.mBottom.resolutionStatus = ConstraintAnchor.UNRESOLVED;
                widget.mBaseline.resolutionStatus = ConstraintAnchor.UNRESOLVED;
            }
        }

        for (int i = 0; i < count; i++) {
            ConstraintWidget widget = container.mChildren.get(i);

            // check horizontal

            ConstraintAnchor begin = widget.mListAnchors[0];
            ConstraintAnchor end = widget.mListAnchors[1];

            boolean parentWrap = horizontalParentWrap;

            if (begin.resolutionStatus == ConstraintAnchor.UNRESOLVED && end.resolutionStatus == ConstraintAnchor.UNRESOLVED) {
                if (begin.mTarget != null && end.mTarget != null) {
                    if (!parentWrap && begin.mTarget == container.mLeft && end.mTarget == container.mRight) {
                        if (widget.mListDimensionBehaviors[ConstraintWidget.DIMENSION_HORIZONTAL] == DimensionBehaviour.FIXED) {
                            float space = container.mWidth - widget.mWidth - begin.getMargin() - end.getMargin();
                            int beginSpace = (int) (space * widget.mHorizontalBiasPercent + 0.5f) + begin.getMargin();
                            begin.resolve(system, beginSpace, null);
                            end.resolve(system, beginSpace + widget.mWidth, null);
                        }
                    }
                } else if (begin.mTarget != null && begin.mTarget == container.mLeft) {
                    begin.resolve(system, begin.getMargin(), null);
                } else if (end.mTarget != null && end.mTarget == container.mRight) {
                    end.resolve(system, container.mWidth - end.getMargin(), null);
                }
            }

            begin = widget.mListAnchors[2];
            end = widget.mListAnchors[3];
            parentWrap = verticalParentWrap;

            if (begin.resolutionStatus == ConstraintAnchor.UNRESOLVED && end.resolutionStatus == ConstraintAnchor.UNRESOLVED) {
                if (begin.mTarget != null && end.mTarget != null) {
                    if (!parentWrap && begin.mTarget == container.mTop && end.mTarget == container.mBottom) {
                        if (widget.mListDimensionBehaviors[ConstraintWidget.DIMENSION_VERTICAL] == DimensionBehaviour.FIXED) {
                            float space = container.mHeight - widget.mHeight - begin.getMargin() - end.getMargin();
                            int beginSpace = (int) (space * widget.mVerticalBiasPercent + 0.5f) + begin.getMargin();
                            begin.resolve(system, beginSpace, null);
                            end.resolve(system, beginSpace + widget.mHeight, null);
                        }
                    }
                } else if (begin.mTarget != null && begin.mTarget == container.mTop) {
                    begin.resolve(system, begin.getMargin(), null);
                } else if (end.mTarget != null && end.mTarget == container.mBottom) {
                    end.resolve(system, container.mHeight - end.getMargin(), null);
                }
            }
        }

        if (DEBUG_OPTIMIZE) {
            System.out.println("post root resolution:");
            for (int i = 0; i < count; i++) {
                ConstraintWidget widget = container.mChildren.get(i);
                System.out.println("widget " + widget + " resolution left: " + widget.mLeft.getResolutionStatus() + " -> " + widget.mLeft);
                System.out.println("widget " + widget + " resolution right: " + widget.mRight.getResolutionStatus() + " -> " + widget.mRight);
                System.out.println("widget " + widget + " resolution top: " + widget.mTop.getResolutionStatus() + " -> " + widget.mTop);
                System.out.println("widget " + widget + " resolution bottom: " + widget.mBottom.getResolutionStatus() + " -> " + widget.mBottom);
                System.out.println("widget " + widget + " resolution baseline: " + widget.mBaseline.getResolutionStatus() + " -> " + widget.mBaseline);
            }
            System.out.println("post root resolution done");
        }

        for (int i = 0; i < count; i++) {
            ConstraintWidget widget = container.mChildren.get(i);
            boolean fixedDimension = widget.mListDimensionBehaviors[ConstraintWidget.HORIZONTAL] == DimensionBehaviour.FIXED
                    || widget.mListDimensionBehaviors[ConstraintWidget.HORIZONTAL] == DimensionBehaviour.WRAP_CONTENT;
            if (fixedDimension) {
                ConstraintAnchor left = widget.mListAnchors[ConstraintWidget.ANCHOR_LEFT];
                ConstraintAnchor right = widget.mListAnchors[ConstraintWidget.ANCHOR_RIGHT];
                if (left.resolutionStatus == ConstraintAnchor.RESOLVED && right.resolutionStatus == ConstraintAnchor.UNRESOLVED) {
                    if (left.resolvedAnchor == null) {
                        right.resolve(system, left.resolvedValue + widget.getWidth(), null);
                    } else {
                        right.resolve(system, widget.getWidth(), left);
                    }
                } else if (left.resolutionStatus == ConstraintAnchor.UNRESOLVED && right.resolutionStatus == ConstraintAnchor.RESOLVED) {
                    if (right.resolvedAnchor == null) {
                        left.resolve(system, right.resolvedValue - widget.getWidth(), null);
                    } else {
                        left.resolve(system, -widget.getWidth(), right);
                    }
                }
            }
            fixedDimension = widget.mListDimensionBehaviors[ConstraintWidget.VERTICAL] == DimensionBehaviour.FIXED
                    || widget.mListDimensionBehaviors[ConstraintWidget.VERTICAL] == DimensionBehaviour.WRAP_CONTENT;
            if (fixedDimension) {
                ConstraintAnchor top = widget.mTop;
                ConstraintAnchor bottom = widget.mBottom;
                if (top.resolutionStatus == ConstraintAnchor.RESOLVED && bottom.resolutionStatus == ConstraintAnchor.UNRESOLVED) {
                    if (top.resolvedAnchor == null) {
                        bottom.resolve(system, top.resolvedValue + widget.getHeight(), null);
                    } else {
                        bottom.resolve(system, widget.getHeight(), top);
                    }
                } else if (top.resolutionStatus == ConstraintAnchor.UNRESOLVED && bottom.resolutionStatus == ConstraintAnchor.RESOLVED) {
                    if (bottom.resolvedAnchor == null) {
                        top.resolve(system, bottom.resolvedValue - widget.getHeight(), null);
                    } else {
                        top.resolve(system, -widget.getHeight(), bottom);
                    }
                }
            }
        }

        // TODO: implement resolution for center connections
        // for (int i = 0; i < count; i++) {
        //     ConstraintWidget widget = container.mChildren.get(i);
        //     for (int j = 0; j < 5; j++) {
        //         ConstraintAnchor anchor = widget.mListAnchors[j];
        //         ConstraintAnchor anchorTarget = anchor.getTarget();
        //         if (anchor.resolutionStatus == ConstraintAnchor.UNRESOLVED && anchorTarget != null && anchorTarget.mTarget != anchor) {
        //             // try to solve it
        //             int[] margin = { anchor.getMargin() };
        //             ConstraintAnchor a = widget.mListAnchors[j].findResolvedAnchor(margin);
        //             if (a != null && a.resolutionStatus == ConstraintAnchor.RESOLVED) {
        //                 widget.mListAnchors[j].resolve(system, margin[0], a);
        //             }
        //         }
        //     }
        // }

        if (DEBUG_OPTIMIZE) {
            for (int i = 0; i < count; i++) {
                ConstraintWidget widget = container.mChildren.get(i);
                System.out.println("widget " + widget + " resolution left: " + widget.mLeft.getResolutionStatus() + " -> " + widget.mLeft);
                System.out.println("widget " + widget + " resolution right: " + widget.mRight.getResolutionStatus() + " -> " + widget.mRight);
                System.out.println("widget " + widget + " resolution top: " + widget.mTop.getResolutionStatus() + " -> " + widget.mTop);
                System.out.println("widget " + widget + " resolution bottom: " + widget.mBottom.getResolutionStatus() + " -> " + widget.mBottom);
                System.out.println("widget " + widget + " resolution baseline: " + widget.mBaseline.getResolutionStatus() + " -> " + widget.mBaseline);
            }
        }

        if (TRACE_OPTIMIZE) {
            int solverAnchors = 0;
            int resolvedAnchors = 0;
            for (int i = 0; i < count; i++) {
                ConstraintWidget widget = container.mChildren.get(i);
                for (int j = 0; j < 5; j++) {
                    ConstraintAnchor anchor = widget.mListAnchors[j];
                    if (!anchor.isConnected()) {
                        continue;
                    }
                    if (anchor.resolutionStatus == ConstraintAnchor.RESOLVED) {
                        resolvedAnchors++;
                    } else {
                        solverAnchors++;
                    }
                }
            }
            System.out.println("We had " + resolvedAnchors + " / " + solverAnchors);
        }

        for (int i = 0; i < count; i++) {
            ConstraintWidget widget = container.mChildren.get(i);
            if (widget.mLeft.resolutionStatus != ConstraintAnchor.RESOLVED) {
                return false;
            }
            if (widget.mRight.resolutionStatus != ConstraintAnchor.RESOLVED) {
                return false;
            }
            if (widget.mTop.resolutionStatus != ConstraintAnchor.RESOLVED) {
                return false;
            }
            if (widget.mBottom.resolutionStatus != ConstraintAnchor.RESOLVED) {
                return false;
            }
        }

        if (DEBUG_OPTIMIZE) {
            System.out.println("\n=> All widgets have been directly resolved, go to the solver\n");
        }
        for (int i = 0; i < count; i++) {
            ConstraintWidget widget = container.mChildren.get(i);
            int left = widget.mLeft.resolvedValue;
            int top = widget.mTop.resolvedValue;
            int right = widget.mRight.resolvedValue;
            int bottom = widget.mBottom.resolvedValue;
            widget.setFrame(left, top, right, bottom);
        }
        return true;
    }
}
