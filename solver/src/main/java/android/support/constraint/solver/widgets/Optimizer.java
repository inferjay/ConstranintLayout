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
    private static final boolean NEW_OPTIMIZER = true;

    /**
     * Implements a direct resolution of chain constraints without using the solver
     *
     * @param container
     * @param system
     * @param numMatchConstraints
     * @param widget
     */
    static void applyDirectResolutionHorizontalChain(ConstraintWidgetContainer container, LinearSystem system, int numMatchConstraints, ConstraintWidget widget) {
        ConstraintWidget firstWidget = widget;
        // loop through the widgets
        int widgetSize = 0;
        int firstPosition = 0; // parent
        ConstraintWidget previous = null;
        int count = 0;
        float totalWeights = 0;

        // Let's first get the size occupied by all widgets not 0dp
        while (widget != null) {
            boolean isGone = widget.getVisibility() == ConstraintWidget.GONE;
            if (!isGone) {
                count++;
                if (widget.mListDimensionBehaviors[DIMENSION_HORIZONTAL] != ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
                    widgetSize += widget.getWidth();
                    widgetSize += widget.mLeft.mTarget != null ? widget.mLeft.getMargin() : 0;
                    widgetSize += widget.mRight.mTarget != null ? widget.mRight.getMargin() : 0;
                } else {
                    totalWeights += widget.mWeight[DIMENSION_HORIZONTAL];
                }
            }
            previous = widget;
            widget = widget.mRight.mTarget != null ? widget.mRight.mTarget.mOwner : null;
            if (widget != null && (widget.mLeft.mTarget == null
                    || (widget.mLeft.mTarget != null && widget.mLeft.mTarget.mOwner != previous))) {
                // end of chain
                widget = null;
            }
        }

        // Then, let's get the position of the last element
        int lastPosition = 0;
        if (previous != null) {
            lastPosition = previous.mRight.mTarget != null ? previous.mRight.mTarget.mOwner.getX() : 0;
            if (previous.mRight.mTarget != null) {
                ConstraintWidget endTarget = previous.mRight.mTarget.mOwner;
                if (endTarget == container) {
                    lastPosition = container.getRight();
                }
            }
        }
        float total = lastPosition - firstPosition;
        float spreadSpace = total - widgetSize;
        float split = spreadSpace / (count + 1);
        widget = firstWidget;
        float currentPosition = 0;
        if (numMatchConstraints == 0) {
            currentPosition = split;
        } else {
            split = spreadSpace / numMatchConstraints;
        }

        // Now, let's iterate on the widgets in the chain and set their position and size
        while (widget != null) {
            int left = widget.mLeft.mTarget != null ? widget.mLeft.getMargin() : 0;
            int right = widget.mRight.mTarget != null ? widget.mRight.getMargin() : 0;
            if (widget.getVisibility() != ConstraintWidget.GONE) {
                currentPosition += left;
                system.addEquality(widget.mLeft.mSolverVariable, (int) (currentPosition + 0.5f));
                if (widget.mListDimensionBehaviors[DIMENSION_HORIZONTAL] == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
                    if (totalWeights == 0) {
                        currentPosition += split - left - right;
                    } else {
                        currentPosition += (spreadSpace * widget.mWeight[DIMENSION_HORIZONTAL] / totalWeights) - left - right;
                    }
                } else {
                    currentPosition += widget.getWidth();
                }
                system.addEquality(widget.mRight.mSolverVariable, (int) (currentPosition + 0.5f));
                if (numMatchConstraints == 0) {
                    currentPosition += split;
                }
                currentPosition += right;
            } else {
                float position = currentPosition - split / 2;
                system.addEquality(widget.mLeft.mSolverVariable, (int) (position + 0.5f));
                system.addEquality(widget.mRight.mSolverVariable, (int) (position + 0.5f));
            }
            previous = widget;
            widget = widget.mRight.mTarget != null ? widget.mRight.mTarget.mOwner : null;
            if (widget != null && widget.mLeft.mTarget != null && widget.mLeft.mTarget.mOwner != previous) {
                // end of chain
                widget = null;
            }
            if (widget == container) {
                widget = null;
            }
        }
    }

    /**
     * Implements a direct resolution of chain constraints without using the solver
     *
     * @param container
     * @param system
     * @param numMatchConstraints
     * @param widget
     */
    static void applyDirectResolutionVerticalChain(ConstraintWidgetContainer container, LinearSystem system, int numMatchConstraints, ConstraintWidget widget) {
        ConstraintWidget firstWidget = widget;
        // loop through the widgets
        int widgetSize = 0;
        int firstPosition = 0; // parent
        ConstraintWidget previous = null;
        int count = 0;
        float totalWeights = 0;

        // Let's first get the size occupied by all widgets not 0dp
        while (widget != null) {
            boolean isGone = widget.getVisibility() == ConstraintWidget.GONE;
            if (!isGone) {
                count++;
                if (widget.mListDimensionBehaviors[DIMENSION_VERTICAL] != ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
                    widgetSize += widget.getHeight();
                    widgetSize += widget.mTop.mTarget != null ? widget.mTop.getMargin() : 0;
                    widgetSize += widget.mBottom.mTarget != null ? widget.mBottom.getMargin() : 0;
                } else {
                    totalWeights += widget.mWeight[DIMENSION_VERTICAL];
                }
            }
            previous = widget;
            widget = widget.mBottom.mTarget != null ? widget.mBottom.mTarget.mOwner : null;
            if (widget != null && (widget.mTop.mTarget == null
                    || (widget.mTop.mTarget != null && widget.mTop.mTarget.mOwner != previous))) {
                // end of chain
                widget = null;
            }
        }

        // Then, let's get the position of the last element
        int lastPosition = 0;
        if (previous != null) {
            lastPosition = previous.mBottom.mTarget != null ? previous.mBottom.mTarget.mOwner.getX() : 0;
            if (previous.mBottom.mTarget != null) {
                ConstraintWidget endTarget = previous.mBottom.mTarget.mOwner;
                if (endTarget == container) {
                    lastPosition = container.getBottom();
                }
            }
        }
        float total = lastPosition - firstPosition;
        float spreadSpace = total - widgetSize;
        float split = spreadSpace / (count + 1);
        widget = firstWidget;
        float currentPosition = 0;
        if (numMatchConstraints == 0) {
            currentPosition = split;
        } else {
            split = spreadSpace / numMatchConstraints;
        }

        // Now, let's iterate on the widgets in the chain and set their position and size
        while (widget != null) {
            int top = widget.mTop.mTarget != null ? widget.mTop.getMargin() : 0;
            int bottom = widget.mBottom.mTarget != null ? widget.mBottom.getMargin() : 0;
            if (widget.getVisibility() != ConstraintWidget.GONE) {
                currentPosition += top;
                system.addEquality(widget.mTop.mSolverVariable, (int) (currentPosition + 0.5f));
                if (widget.mListDimensionBehaviors[DIMENSION_VERTICAL] == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
                    if (totalWeights == 0) {
                        currentPosition += split - top - bottom;
                    } else {
                        currentPosition += (spreadSpace * widget.mWeight[DIMENSION_VERTICAL] / totalWeights) - top - bottom;
                    }
                } else {
                    currentPosition += widget.getHeight();
                }
                system.addEquality(widget.mBottom.mSolverVariable, (int) (currentPosition + 0.5f));
                if (numMatchConstraints == 0) {
                    currentPosition += split;
                }
                currentPosition += bottom;
            } else {
                float position = currentPosition - split / 2;
                system.addEquality(widget.mTop.mSolverVariable, (int) (position + 0.5f));
                system.addEquality(widget.mBottom.mSolverVariable, (int) (position + 0.5f));
            }
            previous = widget;
            widget = widget.mBottom.mTarget != null ? widget.mBottom.mTarget.mOwner : null;
            if (widget != null && widget.mTop.mTarget != null && widget.mTop.mTarget.mOwner != previous) {
                // end of chain
                widget = null;
            }
            if (widget == container) {
                widget = null;
            }
        }
    }

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
     * Resolve simple dependency directly, without using the equation solver
     *
     * @param container
     * @param widget
     */
    static void checkHorizontalSimpleDependency(ConstraintWidgetContainer container, LinearSystem system, ConstraintWidget widget) {
        if (widget.mHorizontalResolution == SOLVER) {
            return;
        }
        if (widget.mListDimensionBehaviors[DIMENSION_HORIZONTAL] == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
            widget.mHorizontalResolution = ConstraintWidget.SOLVER;
            return;
        }
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
            return;
        }
        if (widget.mLeft.mTarget != null && widget.mRight.mTarget != null) {
            if (widget.mLeft.mTarget.mOwner == container && widget.mRight.mTarget.mOwner == container) {
                int left = 0;
                int right = 0;
                int leftMargin = widget.mLeft.getMargin();
                int rightMargin = widget.mRight.getMargin();
                if (container.mListDimensionBehaviors[DIMENSION_HORIZONTAL] == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
                    left = leftMargin;
                    right = container.getWidth() - rightMargin;
                } else {
                    int w = widget.getWidth();
                    int dim = container.getWidth() - leftMargin - rightMargin - w;
                    left = leftMargin + (int) ((dim * widget.mHorizontalBiasPercent) + 0.5f);
                    right = left + widget.getWidth();
                }
                widget.mLeft.mSolverVariable = system.createObjectVariable(widget.mLeft);
                widget.mRight.mSolverVariable = system.createObjectVariable(widget.mRight);
                system.addEquality(widget.mLeft.mSolverVariable, left);
                system.addEquality(widget.mRight.mSolverVariable, right);
                widget.mHorizontalResolution = ConstraintWidget.DIRECT;
                widget.setHorizontalDimension(left, right);
                return;
            }
            widget.mHorizontalResolution = ConstraintWidget.SOLVER;
            return;
        }
        if (widget.mLeft.mTarget != null
                && widget.mLeft.mTarget.mOwner == container) {
            int left = widget.mLeft.getMargin();
            int right = left + widget.getWidth();
            widget.mLeft.mSolverVariable = system.createObjectVariable(widget.mLeft);
            widget.mRight.mSolverVariable = system.createObjectVariable(widget.mRight);
            system.addEquality(widget.mLeft.mSolverVariable, left);
            system.addEquality(widget.mRight.mSolverVariable, right);
            widget.mHorizontalResolution = ConstraintWidget.DIRECT;
            widget.setHorizontalDimension(left, right);
        } else if (widget.mRight.mTarget != null
                && widget.mRight.mTarget.mOwner == container) {
            widget.mLeft.mSolverVariable = system.createObjectVariable(widget.mLeft);
            widget.mRight.mSolverVariable = system.createObjectVariable(widget.mRight);
            int right = container.getWidth() - widget.mRight.getMargin();
            int left = right - widget.getWidth();
            system.addEquality(widget.mLeft.mSolverVariable, left);
            system.addEquality(widget.mRight.mSolverVariable, right);
            widget.mHorizontalResolution = ConstraintWidget.DIRECT;
            widget.setHorizontalDimension(left, right);
        } else if (widget.mLeft.mTarget != null && widget.mLeft.mTarget.mOwner.mHorizontalResolution == ConstraintWidget.DIRECT) {
            SolverVariable target = widget.mLeft.mTarget.mSolverVariable;
            widget.mLeft.mSolverVariable = system.createObjectVariable(widget.mLeft);
            widget.mRight.mSolverVariable = system.createObjectVariable(widget.mRight);
            int left = (int) (target.computedValue + widget.mLeft.getMargin() + 0.5f);
            int right = left + widget.getWidth();
            system.addEquality(widget.mLeft.mSolverVariable, left);
            system.addEquality(widget.mRight.mSolverVariable, right);
            widget.mHorizontalResolution = ConstraintWidget.DIRECT;
            widget.setHorizontalDimension(left, right);
        } else if (widget.mRight.mTarget != null && widget.mRight.mTarget.mOwner.mHorizontalResolution == ConstraintWidget.DIRECT) {
            SolverVariable target = widget.mRight.mTarget.mSolverVariable;
            widget.mLeft.mSolverVariable = system.createObjectVariable(widget.mLeft);
            widget.mRight.mSolverVariable = system.createObjectVariable(widget.mRight);
            int right = (int) (target.computedValue - widget.mRight.getMargin() + 0.5f);
            int left = right - widget.getWidth();
            system.addEquality(widget.mLeft.mSolverVariable, left);
            system.addEquality(widget.mRight.mSolverVariable, right);
            widget.mHorizontalResolution = ConstraintWidget.DIRECT;
            widget.setHorizontalDimension(left, right);
        } else {
            boolean hasLeft = widget.mLeft.mTarget != null;
            boolean hasRight = widget.mRight.mTarget != null;
            if (!hasLeft && !hasRight) {
                if (widget instanceof Guideline) {
                    Guideline guideline = (Guideline) widget;
                    if (guideline.getOrientation() == ConstraintWidget.VERTICAL) {
                        widget.mLeft.mSolverVariable = system.createObjectVariable(widget.mLeft);
                        widget.mRight.mSolverVariable = system.createObjectVariable(widget.mRight);
                        float position = 0;
                        if (guideline.getRelativeBegin() != -1) {
                            position = guideline.getRelativeBegin();
                        } else if (guideline.getRelativeEnd() != -1) {
                            position = container.getWidth() - guideline.getRelativeEnd();
                        } else {
                            position = container.getWidth() * guideline.getRelativePercent();
                        }
                        int value = (int) (position + 0.5f);
                        system.addEquality(widget.mLeft.mSolverVariable, value);
                        system.addEquality(widget.mRight.mSolverVariable, value);
                        widget.mHorizontalResolution = ConstraintWidget.DIRECT;
                        widget.mVerticalResolution = ConstraintWidget.DIRECT;
                        widget.setHorizontalDimension(value, value);
                        widget.setVerticalDimension(0, container.getHeight());
                    }
                } else {
                    widget.mLeft.mSolverVariable = system.createObjectVariable(widget.mLeft);
                    widget.mRight.mSolverVariable = system.createObjectVariable(widget.mRight);
                    int left = widget.getX();
                    int right = left + widget.getWidth();
                    system.addEquality(widget.mLeft.mSolverVariable, left);
                    system.addEquality(widget.mRight.mSolverVariable, right);
                    widget.mHorizontalResolution = ConstraintWidget.DIRECT;
                }
            }
        }
    }

    /**
     * Resolve simple dependency directly, without using the equation solver
     *
     * @param container
     * @param system
     * @param widget
     */
    static void checkVerticalSimpleDependency(ConstraintWidgetContainer container, LinearSystem system, ConstraintWidget widget) {
        if (widget.mVerticalResolution == SOLVER) {
            return;
        }
        if (widget.mListDimensionBehaviors[DIMENSION_VERTICAL] == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
            widget.mVerticalResolution = ConstraintWidget.SOLVER;
            return;
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
            return;
        }
        if (widget.mTop.mTarget != null && widget.mBottom.mTarget != null) {
            if (widget.mTop.mTarget.mOwner == container && widget.mBottom.mTarget.mOwner == container) {
                int top = 0;
                int bottom = 0;
                int topMargin = widget.mTop.getMargin();
                int bottomMargin = widget.mBottom.getMargin();
                if (container.mListDimensionBehaviors[DIMENSION_VERTICAL] == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
                    top = topMargin;
                    bottom = top + widget.getHeight();
                } else {
                    int h = widget.getHeight();
                    int dim = container.getHeight() - topMargin - bottomMargin - h;
                    top = (int) (topMargin + (dim * widget.mVerticalBiasPercent) + 0.5f);
                    bottom = top + widget.getHeight();
                }
                widget.mTop.mSolverVariable = system.createObjectVariable(widget.mTop);
                widget.mBottom.mSolverVariable = system.createObjectVariable(widget.mBottom);
                system.addEquality(widget.mTop.mSolverVariable, top);
                system.addEquality(widget.mBottom.mSolverVariable, bottom);
                if (widget.mBaselineDistance > 0 || widget.getVisibility() == ConstraintWidget.GONE) {
                    widget.mBaseline.mSolverVariable = system.createObjectVariable(widget.mBaseline);
                    system.addEquality(widget.mBaseline.mSolverVariable, top + widget.mBaselineDistance);
                }
                widget.mVerticalResolution = ConstraintWidget.DIRECT;
                widget.setVerticalDimension(top, bottom);
                return;
            }
            widget.mVerticalResolution = ConstraintWidget.SOLVER;
            return;
        }
        if (widget.mTop.mTarget != null
                && widget.mTop.mTarget.mOwner == container) {
            int top = widget.mTop.getMargin();
            int bottom = top + widget.getHeight();
            widget.mTop.mSolverVariable = system.createObjectVariable(widget.mTop);
            widget.mBottom.mSolverVariable = system.createObjectVariable(widget.mBottom);
            system.addEquality(widget.mTop.mSolverVariable, top);
            system.addEquality(widget.mBottom.mSolverVariable, bottom);
            if (widget.mBaselineDistance > 0 || widget.getVisibility() == ConstraintWidget.GONE) {
                widget.mBaseline.mSolverVariable = system.createObjectVariable(widget.mBaseline);
                system.addEquality(widget.mBaseline.mSolverVariable, top + widget.mBaselineDistance);
            }
            widget.mVerticalResolution = ConstraintWidget.DIRECT;
            widget.setVerticalDimension(top, bottom);
        } else if (widget.mBottom.mTarget != null
                && widget.mBottom.mTarget.mOwner == container) {
            widget.mTop.mSolverVariable = system.createObjectVariable(widget.mTop);
            widget.mBottom.mSolverVariable = system.createObjectVariable(widget.mBottom);
            int bottom = container.getHeight() - widget.mBottom.getMargin();
            int top = bottom - widget.getHeight();
            system.addEquality(widget.mTop.mSolverVariable, top);
            system.addEquality(widget.mBottom.mSolverVariable, bottom);
            if (widget.mBaselineDistance > 0 || widget.getVisibility() == ConstraintWidget.GONE) {
                widget.mBaseline.mSolverVariable = system.createObjectVariable(widget.mBaseline);
                system.addEquality(widget.mBaseline.mSolverVariable, top + widget.mBaselineDistance);
            }
            widget.mVerticalResolution = ConstraintWidget.DIRECT;
            widget.setVerticalDimension(top, bottom);
        } else if (widget.mTop.mTarget != null && widget.mTop.mTarget.mOwner.mVerticalResolution == ConstraintWidget.DIRECT) {
            SolverVariable target = widget.mTop.mTarget.mSolverVariable;
            widget.mTop.mSolverVariable = system.createObjectVariable(widget.mTop);
            widget.mBottom.mSolverVariable = system.createObjectVariable(widget.mBottom);
            int top = (int) (target.computedValue + widget.mTop.getMargin() + 0.5f);
            int bottom = top + widget.getHeight();
            system.addEquality(widget.mTop.mSolverVariable, top);
            system.addEquality(widget.mBottom.mSolverVariable, bottom);
            if (widget.mBaselineDistance > 0 || widget.getVisibility() == ConstraintWidget.GONE) {
                widget.mBaseline.mSolverVariable = system.createObjectVariable(widget.mBaseline);
                system.addEquality(widget.mBaseline.mSolverVariable, top + widget.mBaselineDistance);
            }
            widget.mVerticalResolution = ConstraintWidget.DIRECT;
            widget.setVerticalDimension(top, bottom);
        } else if (widget.mBottom.mTarget != null && widget.mBottom.mTarget.mOwner.mVerticalResolution == ConstraintWidget.DIRECT) {
            SolverVariable target = widget.mBottom.mTarget.mSolverVariable;
            widget.mTop.mSolverVariable = system.createObjectVariable(widget.mTop);
            widget.mBottom.mSolverVariable = system.createObjectVariable(widget.mBottom);
            int bottom = (int) (target.computedValue - widget.mBottom.getMargin() + 0.5f);
            int top = bottom - widget.getHeight();
            system.addEquality(widget.mTop.mSolverVariable, top);
            system.addEquality(widget.mBottom.mSolverVariable, bottom);
            if (widget.mBaselineDistance > 0 || widget.getVisibility() == ConstraintWidget.GONE) {
                widget.mBaseline.mSolverVariable = system.createObjectVariable(widget.mBaseline);
                system.addEquality(widget.mBaseline.mSolverVariable, top + widget.mBaselineDistance);
            }
            widget.mVerticalResolution = ConstraintWidget.DIRECT;
            widget.setVerticalDimension(top, bottom);
        } else if (widget.mBaseline.mTarget != null && widget.mBaseline.mTarget.mOwner.mVerticalResolution == ConstraintWidget.DIRECT) {
            SolverVariable target = widget.mBaseline.mTarget.mSolverVariable;
            widget.mTop.mSolverVariable = system.createObjectVariable(widget.mTop);
            widget.mBottom.mSolverVariable = system.createObjectVariable(widget.mBottom);
            int top = (int) (target.computedValue - widget.mBaselineDistance + 0.5f);
            int bottom = top + widget.getHeight();
            system.addEquality(widget.mTop.mSolverVariable, top);
            system.addEquality(widget.mBottom.mSolverVariable, bottom);
            widget.mBaseline.mSolverVariable = system.createObjectVariable(widget.mBaseline);
            system.addEquality(widget.mBaseline.mSolverVariable, top + widget.mBaselineDistance);
            widget.mVerticalResolution = ConstraintWidget.DIRECT;
            widget.setVerticalDimension(top, bottom);
        } else {
            boolean hasBaseline = widget.mBaseline.mTarget != null;
            boolean hasTop = widget.mTop.mTarget != null;
            boolean hasBottom = widget.mBottom.mTarget != null;
            if (!hasBaseline && !hasTop && !hasBottom) {
                if (widget instanceof Guideline) {
                    Guideline guideline = (Guideline) widget;
                    if (guideline.getOrientation() == ConstraintWidget.HORIZONTAL) {
                        widget.mTop.mSolverVariable = system.createObjectVariable(widget.mTop);
                        widget.mBottom.mSolverVariable = system.createObjectVariable(widget.mBottom);
                        float position = 0;
                        if (guideline.getRelativeBegin() != -1) {
                            position = guideline.getRelativeBegin();
                        } else if (guideline.getRelativeEnd() != -1) {
                            position = container.getHeight() - guideline.getRelativeEnd();
                        } else {
                            position = container.getHeight() * guideline.getRelativePercent();
                        }
                        int value = (int) (position + 0.5f);
                        system.addEquality(widget.mTop.mSolverVariable, value);
                        system.addEquality(widget.mBottom.mSolverVariable, value);
                        widget.mVerticalResolution = ConstraintWidget.DIRECT;
                        widget.mHorizontalResolution = ConstraintWidget.DIRECT;
                        widget.setVerticalDimension(value, value);
                        widget.setHorizontalDimension(0, container.getWidth());
                    }
                } else {
                    widget.mTop.mSolverVariable = system.createObjectVariable(widget.mTop);
                    widget.mBottom.mSolverVariable = system.createObjectVariable(widget.mBottom);
                    int top = widget.getY();
                    int bottom = top + widget.getHeight();
                    system.addEquality(widget.mTop.mSolverVariable, top);
                    system.addEquality(widget.mBottom.mSolverVariable, bottom);
                    if (widget.mBaselineDistance > 0 || widget.getVisibility() == ConstraintWidget.GONE) {
                        widget.mBaseline.mSolverVariable = system.createObjectVariable(widget.mBaseline);
                        system.addEquality(widget.mBaseline.mSolverVariable, top + widget.mBaselineDistance);
                    }
                    widget.mVerticalResolution = ConstraintWidget.DIRECT;
                }
            }
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
        boolean done = false;
        int dv = 0;
        int dh = 0;
        int n = 0;

        if (NEW_OPTIMIZER) {
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

        for (int i = 0; i < count; i++) {
            ConstraintWidget widget = container.mChildren.get(i);
            // TODO: we should try to cache some of that
            widget.mHorizontalResolution = ConstraintWidget.UNKNOWN;
            widget.mVerticalResolution = ConstraintWidget.UNKNOWN;
            if (widget.mListDimensionBehaviors[DIMENSION_HORIZONTAL] == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT
                    || widget.mListDimensionBehaviors[DIMENSION_VERTICAL] == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
                widget.mHorizontalResolution = ConstraintWidget.SOLVER;
                widget.mVerticalResolution = ConstraintWidget.SOLVER;
            }
            if (widget.mListDimensionBehaviors[DIMENSION_HORIZONTAL] == ConstraintWidget.DimensionBehaviour.MATCH_PARENT
                    || widget.mListDimensionBehaviors[DIMENSION_VERTICAL] == ConstraintWidget.DimensionBehaviour.MATCH_PARENT) {
                widget.mHorizontalResolution = ConstraintWidget.SOLVER;
                widget.mVerticalResolution = ConstraintWidget.SOLVER;
            }
            if ((container.mListDimensionBehaviors[DIMENSION_HORIZONTAL] != ConstraintWidget.DimensionBehaviour.FIXED)
                || (container.mListDimensionBehaviors[DIMENSION_VERTICAL] != ConstraintWidget.DimensionBehaviour.FIXED)) {
                widget.mHorizontalResolution = ConstraintWidget.SOLVER;
                widget.mVerticalResolution = ConstraintWidget.SOLVER;
            }
            if (widget instanceof Barrier) {
                widget.mHorizontalResolution = ConstraintWidget.SOLVER;
                widget.mVerticalResolution = ConstraintWidget.SOLVER;
            }
        }
        while (!done) {
            int prev = dv;
            int preh = dh;
            dv = 0;
            dh = 0;
            n++;
            if (DEBUG_OPTIMIZE) {
                System.out.println("Iteration " + n);
            }
            for (int i = 0; i < count; i++) {
                ConstraintWidget widget = container.mChildren.get(i);
                if (widget.mHorizontalResolution == ConstraintWidget.UNKNOWN) {
                    if (container.mListDimensionBehaviors[DIMENSION_HORIZONTAL] == ConstraintWidget.DimensionBehaviour.WRAP_CONTENT) {
                        widget.mHorizontalResolution = ConstraintWidget.SOLVER;
                    } else {
                        checkHorizontalSimpleDependency(container, system, widget);
                    }
                }
                if (widget.mVerticalResolution == ConstraintWidget.UNKNOWN) {
                    if (container.mListDimensionBehaviors[DIMENSION_VERTICAL] == ConstraintWidget.DimensionBehaviour.WRAP_CONTENT) {
                        widget.mVerticalResolution = ConstraintWidget.SOLVER;
                    } else {
                        checkVerticalSimpleDependency(container, system, widget);
                    }
                }
                if (DEBUG_OPTIMIZE) {
                    System.out.println("[" + i + "]" + widget
                            + " H: " + widget.mHorizontalResolution
                            + " V: " + widget.mVerticalResolution);
                }
                if (widget.mVerticalResolution == ConstraintWidget.UNKNOWN) {
                    dv++;
                }
                if (widget.mHorizontalResolution == ConstraintWidget.UNKNOWN) {
                    dh++;
                }
            }
            if (DEBUG_OPTIMIZE) {
                System.out.println("dv: " + dv + " dh: " + dh);
            }
            if (dv == 0 && dh == 0) {
                done = true;
            } else if (prev == dv && preh == dh) {
                done = true;
                if (DEBUG_OPTIMIZE) {
                    System.out.println("Escape clause");
                }
            }
        }

        int sh = 0;
        int sv = 0;
        for (int i = 0; i < count; i++) {
            ConstraintWidget widget = container.mChildren.get(i);
            if (widget.mHorizontalResolution == ConstraintWidget.SOLVER
                    || widget.mHorizontalResolution == ConstraintWidget.UNKNOWN) {
                sh++;
            }
            if (widget.mVerticalResolution == ConstraintWidget.SOLVER
                    || widget.mVerticalResolution == ConstraintWidget.UNKNOWN) {
                sv++;
            }
        }
        if (sh == 0 && sv == 0) {
            return true;
        }
        return false;
    }

    /**
     * This recursively walks the tree of connected components
     * calculating there distance to the left,right,top and bottom
     *
     * @param widget
     */
    public static void findHorizontalWrapRecursive(ConstraintWidget widget, boolean[] flags) {
        if (widget.mListDimensionBehaviors[DIMENSION_HORIZONTAL] == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT
                && widget.mListDimensionBehaviors[DIMENSION_VERTICAL] == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT
                && widget.mDimensionRatio > 0) {
            flags[FLAG_USE_OPTIMIZE] = false;
            return;
        }
        if (widget.mListDimensionBehaviors[DIMENSION_HORIZONTAL] == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT
                && widget.mMatchConstraintDefaultWidth == ConstraintWidget.MATCH_CONSTRAINT_PERCENT) {
            flags[FLAG_USE_OPTIMIZE] = false;
            return;
        }
        int w = widget.getOptimizerWrapWidth();

        if (widget.mListDimensionBehaviors[DIMENSION_HORIZONTAL] == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
            if (widget.mListDimensionBehaviors[DIMENSION_VERTICAL] != ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT
                    && widget.mDimensionRatio > 0) {
                flags[FLAG_USE_OPTIMIZE] = false;
                return;
                // TODO: support ratio
                // w = (int) (widget.mDimensionRatio * h);
                // widget.setWidth(w);
            }
        }
        int distToRight = w;
        int distToLeft = w;
        ConstraintWidget leftWidget = null;
        ConstraintWidget rightWidget = null;
        widget.mHorizontalWrapVisited = true;

        if (widget instanceof Guideline) {
            Guideline guideline = (Guideline) widget;
            if (guideline.getOrientation() == ConstraintWidget.VERTICAL) {
                distToLeft = 0;
                distToRight = 0;
                if (guideline.getRelativeBegin() != Guideline.UNKNOWN) {
                    distToLeft = guideline.getRelativeBegin();
                } else if (guideline.getRelativeEnd() != Guideline.UNKNOWN) {
                    distToRight = guideline.getRelativeEnd();
                } else if (guideline.getRelativePercent() != Guideline.UNKNOWN) {
                    flags[FLAG_USE_OPTIMIZE] = false;
                    return;
                }
            }
        } else if (!(widget.mRight.isConnected() || (widget.mLeft.isConnected()))) {
            distToLeft += widget.getX();
        } else {
            if (widget.mRight.mTarget != null && widget.mLeft.mTarget != null
                && widget.mIsWidthWrapContent
                && widget.mListDimensionBehaviors[DIMENSION_VERTICAL] == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
                flags[FLAG_USE_OPTIMIZE] = false;
                return;
            }
            if (widget.mRight.mTarget != null && widget.mLeft.mTarget != null
                    && ((widget.mRight.mTarget == widget.mLeft.mTarget)
                        || ((widget.mRight.mTarget.mOwner == widget.mLeft.mTarget.mOwner)
                            && (widget.mRight.mTarget.mOwner != widget.mParent)))) {
                flags[FLAG_USE_OPTIMIZE] = false;
                return;
            }
            if (widget.mRight.mTarget != null) {
                rightWidget = widget.mRight.mTarget.mOwner;
                distToRight += widget.mRight.getMargin();
                if (!rightWidget.isRoot() && !rightWidget.mHorizontalWrapVisited) {
                    findHorizontalWrapRecursive(rightWidget, flags);
                }
            }
            if (widget.mLeft.mTarget != null) {
                leftWidget = widget.mLeft.mTarget.mOwner;
                distToLeft += widget.mLeft.getMargin();
                if (!leftWidget.isRoot() && !leftWidget.mHorizontalWrapVisited) {
                    findHorizontalWrapRecursive(leftWidget, flags);
                }
            }

            if (widget.mRight.mTarget != null && !rightWidget.isRoot()) {
                if (widget.mRight.mTarget.mType == ConstraintAnchor.Type.RIGHT) {
                    distToRight += rightWidget.mDistToRight - rightWidget.getOptimizerWrapWidth();
                } else if (widget.mRight.mTarget.getType() == ConstraintAnchor.Type.LEFT) {
                    distToRight += rightWidget.mDistToRight;
                }
                // Center connection
                widget.mRightHasCentered = rightWidget.mRightHasCentered
                        || (rightWidget.mLeft.mTarget != null && rightWidget.mRight.mTarget != null
                        && rightWidget.mListDimensionBehaviors[DIMENSION_HORIZONTAL] != ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
                if (widget.mRightHasCentered
                        && (rightWidget.mLeft.mTarget == null ? true : rightWidget.mLeft.mTarget.mOwner != widget)) {
                    distToRight += distToRight - rightWidget.mDistToRight;
                }
            }

            if (widget.mLeft.mTarget != null && !leftWidget.isRoot()) {
                if (widget.mLeft.mTarget.getType() == ConstraintAnchor.Type.LEFT) {
                    distToLeft += leftWidget.mDistToLeft - leftWidget.getOptimizerWrapWidth();
                } else if (widget.mLeft.mTarget.getType() == ConstraintAnchor.Type.RIGHT) {
                    distToLeft += leftWidget.mDistToLeft;
                }
                // Center connection
                widget.mLeftHasCentered = leftWidget.mLeftHasCentered
                        || (leftWidget.mLeft.mTarget != null && leftWidget.mRight.mTarget != null
                            && leftWidget.mListDimensionBehaviors[DIMENSION_HORIZONTAL] != ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
                if (widget.mLeftHasCentered
                        && (leftWidget.mRight.mTarget == null ? true : leftWidget.mRight.mTarget.mOwner != widget)) {
                    distToLeft += distToLeft - leftWidget.mDistToLeft;
                }
            }
        }
        if (widget.getVisibility() == ConstraintWidget.GONE) {
            distToLeft -= widget.mWidth;
            distToRight -= widget.mWidth;
        }
        widget.mDistToLeft = distToLeft;
        widget.mDistToRight = distToRight;
    }

    public static void findVerticalWrapRecursive(ConstraintWidget widget, boolean[] flags) {
        if (widget.mListDimensionBehaviors[DIMENSION_VERTICAL] == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
            if (widget.mListDimensionBehaviors[DIMENSION_HORIZONTAL] != ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT
                    && widget.mDimensionRatio > 0) {
                flags[FLAG_USE_OPTIMIZE] = false;
                return;
                // TODO: support ratio
                // h = (int) (w / widget.mDimensionRatio);
                // widget.setHeight(h);
            }
        }
        if (widget.mListDimensionBehaviors[DIMENSION_VERTICAL] == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT
                && widget.mMatchConstraintDefaultHeight == ConstraintWidget.MATCH_CONSTRAINT_PERCENT) {
            flags[FLAG_USE_OPTIMIZE] = false;
            return;
        }

        int h = widget.getOptimizerWrapHeight();

        int distToTop = h;
        int distToBottom = h;
        ConstraintWidget topWidget = null;
        ConstraintWidget bottomWidget = null;
        widget.mVerticalWrapVisited = true;

        if (widget instanceof Guideline) {
            Guideline guideline = (Guideline) widget;
            if (guideline.getOrientation() == ConstraintWidget.HORIZONTAL) {
                distToTop = 0;
                distToBottom = 0;
                if (guideline.getRelativeBegin() != Guideline.UNKNOWN) {
                    distToTop = guideline.getRelativeBegin();
                } else if (guideline.getRelativeEnd() != Guideline.UNKNOWN) {
                    distToBottom = guideline.getRelativeEnd();
                } else if (guideline.getRelativePercent() != Guideline.UNKNOWN) {
                    flags[FLAG_USE_OPTIMIZE] = false;
                    return;
                }
            }
        } else if (!(widget.mBaseline.mTarget != null || widget.mTop.mTarget != null || widget.mBottom.mTarget != null)) {
            distToTop += widget.getY();
        } else {
            if (widget.mBottom.mTarget != null && widget.mTop.mTarget != null
                && widget.mIsHeightWrapContent
                && widget.mListDimensionBehaviors[DIMENSION_HORIZONTAL] == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
                flags[FLAG_USE_OPTIMIZE] = false;
                return;
            }
            if (widget.mBottom.mTarget != null && widget.mTop.mTarget != null
                    && ((widget.mBottom.mTarget == widget.mTop.mTarget)
                        || ((widget.mBottom.mTarget.mOwner == widget.mTop.mTarget.mOwner)
                            && (widget.mBottom.mTarget.mOwner != widget.mParent)))) {
                flags[FLAG_USE_OPTIMIZE] = false;
                return;
            }
            if (widget.mBaseline.isConnected()) {
                ConstraintWidget baseLineWidget = widget.mBaseline.mTarget.getOwner();
                if (!baseLineWidget.mVerticalWrapVisited) {
                    findVerticalWrapRecursive(baseLineWidget, flags);
                }
                distToTop = Math.max(baseLineWidget.mDistToTop - baseLineWidget.mHeight + h, h);
                distToBottom = Math.max(baseLineWidget.mDistToBottom - baseLineWidget.mHeight + h, h);
                if (widget.getVisibility() == ConstraintWidget.GONE) {
                    distToTop -= widget.mHeight;
                    distToBottom -= widget.mHeight;
                }
                widget.mDistToTop = distToTop;
                widget.mDistToBottom = distToBottom;
                return; // if baseline connected no need to look at top or bottom
            }
            if (widget.mTop.isConnected()) {
                topWidget = widget.mTop.mTarget.getOwner();
                distToTop += widget.mTop.getMargin();
                if (!topWidget.isRoot() && !topWidget.mVerticalWrapVisited) {
                    findVerticalWrapRecursive(topWidget, flags);
                }
            }
            if (widget.mBottom.isConnected()) {
                bottomWidget = widget.mBottom.mTarget.getOwner();
                distToBottom += widget.mBottom.getMargin();
                if (!bottomWidget.isRoot() && !bottomWidget.mVerticalWrapVisited) {
                    findVerticalWrapRecursive(bottomWidget, flags);
                }
            }

            if (widget.mTop.mTarget != null && !topWidget.isRoot()) {
                if (widget.mTop.mTarget.getType() == ConstraintAnchor.Type.TOP) {
                    distToTop += topWidget.mDistToTop - topWidget.getOptimizerWrapHeight();
                } else if (widget.mTop.mTarget.getType() == ConstraintAnchor.Type.BOTTOM) {
                    distToTop += topWidget.mDistToTop;
                }
                // Center connection
                widget.mTopHasCentered = topWidget.mTopHasCentered
                        || (topWidget.mTop.mTarget != null && topWidget.mTop.mTarget.mOwner != widget
                        && topWidget.mBottom.mTarget != null
                        && topWidget.mBottom.mTarget.mOwner != widget
                        && topWidget.mListDimensionBehaviors[DIMENSION_VERTICAL] != ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
                if (widget.mTopHasCentered
                        && (topWidget.mBottom.mTarget == null ? true : topWidget.mBottom.mTarget.mOwner != widget)) {
                    distToTop += distToTop - topWidget.mDistToTop;
                }
            }
            if (widget.mBottom.mTarget != null && !bottomWidget.isRoot()) {
                if (widget.mBottom.mTarget.getType() == ConstraintAnchor.Type.BOTTOM) {
                    distToBottom += bottomWidget.mDistToBottom - bottomWidget.getOptimizerWrapHeight();
                } else if (widget.mBottom.mTarget.getType() == ConstraintAnchor.Type.TOP) {
                    distToBottom += bottomWidget.mDistToBottom;
                }
                // Center connection
                widget.mBottomHasCentered = bottomWidget.mBottomHasCentered
                        || (bottomWidget.mTop.mTarget != null && bottomWidget.mTop.mTarget.mOwner != widget
                        && bottomWidget.mBottom.mTarget != null
                        && bottomWidget.mBottom.mTarget.mOwner != widget
                        && bottomWidget.mListDimensionBehaviors[DIMENSION_VERTICAL] != ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
                if (widget.mBottomHasCentered
                        && (bottomWidget.mTop.mTarget == null ? true : bottomWidget.mTop.mTarget.mOwner != widget)) {
                    distToBottom += distToBottom - bottomWidget.mDistToBottom;
                }
            }
        }
        if (widget.getVisibility() == ConstraintWidget.GONE) {
            distToTop -= widget.mHeight;
            distToBottom -= widget.mHeight;
        }

        widget.mDistToTop = distToTop;
        widget.mDistToBottom = distToBottom;
    }

    /**
     * calculates the wrapContent size.
     *
     * @param constraintWidgetContainer
     * @param children
     */
    public static void findWrapSize(ConstraintWidgetContainer constraintWidgetContainer, ArrayList<ConstraintWidget> children, boolean[] flags) {
        int maxTopDist = 0;
        int maxLeftDist = 0;
        int maxRightDist = 0;
        int maxBottomDist = 0;

        int maxConnectWidth = 0;
        int maxConnectHeight = 0;
        final int size = children.size();
        flags[FLAG_USE_OPTIMIZE] = true;

        try {
            for (int j = 0; j < size; j++) {
                ConstraintWidget widget = children.get(j);
                if (widget.isRoot()) {
                    continue;
                }
                if (!widget.mHorizontalWrapVisited) {
                    findHorizontalWrapRecursive(widget, flags);
                }
                if (!flags[FLAG_USE_OPTIMIZE]) {
                    return;
                }
                if (!widget.mVerticalWrapVisited) {
                    findVerticalWrapRecursive(widget, flags);
                }
                if (!flags[FLAG_USE_OPTIMIZE]) {
                    return;
                }
                int connectWidth = widget.mDistToLeft + widget.mDistToRight - widget.getWidth();
                int connectHeight = widget.mDistToTop + widget.mDistToBottom - widget.getHeight();
                if (widget.mListDimensionBehaviors[DIMENSION_HORIZONTAL] == ConstraintWidget.DimensionBehaviour.MATCH_PARENT) {
                    connectWidth = widget.getWidth() + widget.mLeft.mMargin + widget.mRight.mMargin;
                }
                if (widget.mListDimensionBehaviors[DIMENSION_VERTICAL] == ConstraintWidget.DimensionBehaviour.MATCH_PARENT) {
                    connectHeight =
                        widget.getHeight() + widget.mTop.mMargin + widget.mBottom.mMargin;
                }
                if (widget.getVisibility() == ConstraintWidget.GONE) {
                    connectWidth = 0;
                    connectHeight = 0;
                }
                maxLeftDist = Math.max(maxLeftDist, widget.mDistToLeft);
                maxRightDist = Math.max(maxRightDist, widget.mDistToRight);
                maxBottomDist = Math.max(maxBottomDist, widget.mDistToBottom);
                maxTopDist = Math.max(maxTopDist, widget.mDistToTop);
                maxConnectWidth = Math.max(maxConnectWidth, connectWidth);
                maxConnectHeight = Math.max(maxConnectHeight, connectHeight);
            }
            int max = Math.max(maxLeftDist, maxRightDist);
            constraintWidgetContainer.mWrapWidth = Math.max(constraintWidgetContainer.mMinWidth, Math.max(max, maxConnectWidth));
            max = Math.max(maxTopDist, maxBottomDist);
            constraintWidgetContainer.mWrapHeight = Math.max(constraintWidgetContainer.mMinHeight, Math.max(max, maxConnectHeight));
        } finally {
            for (int j = 0; j < size; j++) {
                ConstraintWidget child = children.get(j);
                child.mHorizontalWrapVisited = false;
                child.mVerticalWrapVisited = false;
                child.mLeftHasCentered = false;
                child.mRightHasCentered = false;
                child.mTopHasCentered = false;
                child.mBottomHasCentered = false;
            }
        }
    }
}
