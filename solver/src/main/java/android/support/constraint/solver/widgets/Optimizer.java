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

/**
 * Implements direct resolution without using the solver
 */
public class Optimizer {

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
            count++;
            if (widget.mHorizontalDimensionBehaviour != ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
                widgetSize += widget.getWidth();
                widgetSize += widget.mLeft.mTarget != null ? widget.mLeft.getMargin() : 0;
                widgetSize += widget.mRight.mTarget != null ? widget.mRight.getMargin() : 0;
            } else {
                totalWeights += widget.mHorizontalWeight;
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
            currentPosition += left;
            system.addEquality(widget.mLeft.mSolverVariable, (int) currentPosition);
            if (widget.mHorizontalDimensionBehaviour == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
                if (totalWeights == 0) {
                    currentPosition += split - left - right;
                } else {
                    currentPosition += (spreadSpace * widget.mHorizontalWeight / totalWeights) - left - right;
                }
            } else {
                currentPosition += widget.getWidth();
            }
            system.addEquality(widget.mRight.mSolverVariable, (int) currentPosition);
            if (numMatchConstraints == 0) {
                currentPosition += split;
            }
            currentPosition += right;
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
            count++;
            if (widget.mVerticalDimensionBehaviour != ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
                widgetSize += widget.getHeight();
                widgetSize += widget.mTop.mTarget != null ? widget.mTop.getMargin() : 0;
                widgetSize += widget.mBottom.mTarget != null ? widget.mBottom.getMargin() : 0;
            } else {
                totalWeights += widget.mVerticalWeight;
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
            currentPosition += top;
            system.addEquality(widget.mTop.mSolverVariable, (int) currentPosition);
            if (widget.mVerticalDimensionBehaviour == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
                if (totalWeights == 0) {
                    currentPosition += split - top - bottom;
                } else {
                    currentPosition += (spreadSpace * widget.mVerticalWeight / totalWeights) - top - bottom;
                }
            } else {
                currentPosition += widget.getHeight();
            }
            system.addEquality(widget.mBottom.mSolverVariable, (int) currentPosition);
            if (numMatchConstraints == 0) {
                currentPosition += split;
            }
            currentPosition += bottom;
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

    /**
     * Resolve simple dependency directly, without using the equation solver
     *
     * @param container
     * @param widget
     */
    static void checkHorizontalSimpleDependency(ConstraintWidgetContainer container, LinearSystem system, ConstraintWidget widget) {
        if (widget.mHorizontalDimensionBehaviour == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
            widget.mHorizontalResolution = ConstraintWidget.SOLVER;
            return;
        }
        if (widget.mLeft.mTarget != null && widget.mRight.mTarget != null) {
            if (widget.mLeft.mTarget.mOwner == container && widget.mRight.mTarget.mOwner == container) {
                int left = 0;
                int right = 0;
                int leftMargin = widget.mLeft.getMargin();
                int rightMargin = widget.mRight.getMargin();
                if (container.mHorizontalDimensionBehaviour == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
                    left = leftMargin;
                    right = container.getWidth() - rightMargin;
                } else {
                    int w = widget.getWidth();
                    int dim = container.getWidth() - leftMargin - rightMargin - w;
                    left = leftMargin + (int) (dim * widget.mHorizontalBiasPercent);
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
            int left = (int) (target.computedValue + widget.mLeft.getMargin());
            int right = left + widget.getWidth();
            system.addEquality(widget.mLeft.mSolverVariable, left);
            system.addEquality(widget.mRight.mSolverVariable, right);
            widget.mHorizontalResolution = ConstraintWidget.DIRECT;
            widget.setHorizontalDimension(left, right);
        } else if (widget.mRight.mTarget != null && widget.mRight.mTarget.mOwner.mHorizontalResolution == ConstraintWidget.DIRECT) {
            SolverVariable target = widget.mRight.mTarget.mSolverVariable;
            widget.mLeft.mSolverVariable = system.createObjectVariable(widget.mLeft);
            widget.mRight.mSolverVariable = system.createObjectVariable(widget.mRight);
            int right = (int) (target.computedValue - widget.mRight.getMargin());
            int left = right - widget.getWidth();
            system.addEquality(widget.mLeft.mSolverVariable, left);
            system.addEquality(widget.mRight.mSolverVariable, right);
            widget.mHorizontalResolution = ConstraintWidget.DIRECT;
            widget.setHorizontalDimension(left, right);
        } else {
            boolean hasLeft = widget.mLeft.mTarget != null;
            boolean hasRight = widget.mRight.mTarget != null;
            if (!hasLeft && !hasRight && !(widget instanceof Guideline)) {
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

    /**
     * Resolve simple dependency directly, without using the equation solver
     *
     * @param container
     * @param system
     * @param widget
     */
    static void checkVerticalSimpleDependency(ConstraintWidgetContainer container, LinearSystem system, ConstraintWidget widget) {
        if (widget.mVerticalDimensionBehaviour == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
            widget.mVerticalResolution = ConstraintWidget.SOLVER;
            return;
        }
        if (widget.mTop.mTarget != null && widget.mBottom.mTarget != null) {
            if (widget.mTop.mTarget.mOwner == container && widget.mBottom.mTarget.mOwner == container) {
                int top = 0;
                int bottom = 0;
                int topMargin = widget.mTop.getMargin();
                int bottomMargin = widget.mBottom.getMargin();
                if (container.mVerticalDimensionBehaviour == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
                    top = topMargin;
                    bottom = top + widget.getHeight();
                } else {
                    int h = widget.getHeight();
                    int dim = container.getHeight() - topMargin - bottomMargin - h;
                    top = topMargin + (int) (dim * widget.mVerticalBiasPercent);
                    bottom = top + widget.getHeight();
                }
                widget.mTop.mSolverVariable = system.createObjectVariable(widget.mTop);
                widget.mBottom.mSolverVariable = system.createObjectVariable(widget.mBottom);
                system.addEquality(widget.mTop.mSolverVariable, top);
                system.addEquality(widget.mBottom.mSolverVariable, bottom);
                if (widget.mBaselineDistance > 0) {
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
            if (widget.mBaselineDistance > 0) {
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
            if (widget.mBaselineDistance > 0) {
                widget.mBaseline.mSolverVariable = system.createObjectVariable(widget.mBaseline);
                system.addEquality(widget.mBaseline.mSolverVariable, top + widget.mBaselineDistance);
            }
            widget.mVerticalResolution = ConstraintWidget.DIRECT;
            widget.setVerticalDimension(top, bottom);
        } else if (widget.mTop.mTarget != null && widget.mTop.mTarget.mOwner.mVerticalResolution == ConstraintWidget.DIRECT) {
            SolverVariable target = widget.mTop.mTarget.mSolverVariable;
            widget.mTop.mSolverVariable = system.createObjectVariable(widget.mTop);
            widget.mBottom.mSolverVariable = system.createObjectVariable(widget.mBottom);
            int top = (int) (target.computedValue + widget.mTop.getMargin());
            int bottom = top + widget.getHeight();
            system.addEquality(widget.mTop.mSolverVariable, top);
            system.addEquality(widget.mBottom.mSolverVariable, bottom);
            if (widget.mBaselineDistance > 0) {
                widget.mBaseline.mSolverVariable = system.createObjectVariable(widget.mBaseline);
                system.addEquality(widget.mBaseline.mSolverVariable, top + widget.mBaselineDistance);
            }
            widget.mVerticalResolution = ConstraintWidget.DIRECT;
            widget.setVerticalDimension(top, bottom);
        } else if (widget.mBottom.mTarget != null && widget.mBottom.mTarget.mOwner.mVerticalResolution == ConstraintWidget.DIRECT) {
            SolverVariable target = widget.mBottom.mTarget.mSolverVariable;
            widget.mTop.mSolverVariable = system.createObjectVariable(widget.mTop);
            widget.mBottom.mSolverVariable = system.createObjectVariable(widget.mBottom);
            int bottom = (int) (target.computedValue - widget.mBottom.getMargin());
            int top = bottom - widget.getHeight();
            system.addEquality(widget.mTop.mSolverVariable, top);
            system.addEquality(widget.mBottom.mSolverVariable, bottom);
            if (widget.mBaselineDistance > 0) {
                widget.mBaseline.mSolverVariable = system.createObjectVariable(widget.mBaseline);
                system.addEquality(widget.mBaseline.mSolverVariable, top + widget.mBaselineDistance);
            }
            widget.mVerticalResolution = ConstraintWidget.DIRECT;
            widget.setVerticalDimension(top, bottom);
        } else if (widget.mBaseline.mTarget != null && widget.mBaseline.mTarget.mOwner.mVerticalResolution == ConstraintWidget.DIRECT) {
            SolverVariable target = widget.mBaseline.mTarget.mSolverVariable;
            widget.mTop.mSolverVariable = system.createObjectVariable(widget.mTop);
            widget.mBottom.mSolverVariable = system.createObjectVariable(widget.mBottom);
            int top = (int) (target.computedValue - widget.mBaselineDistance);
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
            if (!hasBaseline && !hasTop && !hasBottom && !(widget instanceof Guideline)) {
                widget.mTop.mSolverVariable = system.createObjectVariable(widget.mTop);
                widget.mBottom.mSolverVariable = system.createObjectVariable(widget.mBottom);
                int top = widget.getY();
                int bottom = top + widget.getHeight();
                system.addEquality(widget.mTop.mSolverVariable, top);
                system.addEquality(widget.mBottom.mSolverVariable, bottom);
                if (widget.mBaselineDistance > 0) {
                    widget.mBaseline.mSolverVariable = system.createObjectVariable(widget.mBaseline);
                    system.addEquality(widget.mBaseline.mSolverVariable, top + widget.mBaselineDistance);
                }
                widget.mVerticalResolution = ConstraintWidget.DIRECT;
            }
        }
    }
}
