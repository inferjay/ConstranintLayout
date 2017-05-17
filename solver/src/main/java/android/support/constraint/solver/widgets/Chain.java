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

import android.support.constraint.solver.ArrayRow;
import android.support.constraint.solver.LinearSystem;
import android.support.constraint.solver.SolverVariable;

import java.util.Arrays;

/**
 * Chain management and constraints creation
 */
class Chain {

    static final int CHAIN_FIRST = 0;
    static final int CHAIN_LAST = 1;
    static final int CHAIN_FIRST_VISIBLE = 2;
    static final int CHAIN_LAST_VISIBLE = 3;

    /**
     * Apply specific rules for dealing with chains of widgets.
     * Chains are defined as a list of widget linked together with bi-directional connections
     *
     * @param constraintWidgetContainer
     * @param system
     * @param orientation HORIZONTAL or VERTICAL
     */
    static void applyChainConstraints(ConstraintWidgetContainer constraintWidgetContainer, LinearSystem system, int orientation) {
        ConstraintWidget first = null;
        ConstraintWidget[] chainsArray = null;
        int numMatchConstraints = 0;
        int offset = 0;
        int dimensionOffset = 0;
        int chainsSize = 0;
        boolean isChainSpread;
        boolean isChainPacked;
        boolean isWrapContent;
        if (orientation == ConstraintWidget.HORIZONTAL) {
            offset = 0;
            dimensionOffset = 0;
            chainsSize = constraintWidgetContainer.mHorizontalChainsSize;
            chainsArray = constraintWidgetContainer.mHorizontalChainsArray;
        } else {
            offset = 2;
            dimensionOffset = 1;
            chainsSize = constraintWidgetContainer.mVerticalChainsSize;
            chainsArray = constraintWidgetContainer.mVerticalChainsArray;
        }

        for (int i = 0; i < chainsSize; i++) {
            first = chainsArray[i];
            numMatchConstraints = countMatchConstraintsChainedWidgets(constraintWidgetContainer, system, constraintWidgetContainer.mChainEnds, chainsArray[i], orientation, Optimizer.flags);
            // First, let's get to the first visible widget...
            ConstraintWidget currentWidget = constraintWidgetContainer.mChainEnds[CHAIN_FIRST_VISIBLE];
            if (currentWidget == null) {
                // entire chain is gone...
                continue;
            }
            if (Optimizer.flags[Optimizer.FLAG_CHAIN_DANGLING]) {
                int value = 0;
                if (orientation == ConstraintWidget.HORIZONTAL) {
                    value = first.getDrawX();
                } else {
                    value = first.getDrawY();
                }
                while (currentWidget != null) {
                    value += currentWidget.mListAnchors[offset].getMargin();
                    system.addEquality(currentWidget.mListAnchors[offset].mSolverVariable, value);
                    ConstraintWidget next = null;
                    if (orientation == ConstraintWidget.HORIZONTAL) {
                        next = currentWidget.mHorizontalNextWidget;
                        value += currentWidget.getWidth();
                    } else {
                        next = currentWidget.mVerticalNextWidget;
                        value += currentWidget.getHeight();
                    }
                    value += currentWidget.mListAnchors[offset + 1].getMargin();
                    if (next == null) {
                        next = constraintWidgetContainer.mChainEnds[CHAIN_LAST];
                    }
                    if (currentWidget != next) {
                        currentWidget = next;
                    } else {
                        currentWidget = null;
                    }
                }
                continue;
            }

            if (orientation == ConstraintWidget.HORIZONTAL) {
                isChainSpread = first.mHorizontalChainStyle == ConstraintWidget.CHAIN_SPREAD;
                isChainPacked = first.mHorizontalChainStyle == ConstraintWidget.CHAIN_PACKED;
                isWrapContent = constraintWidgetContainer.mListDimensionBehaviors[ConstraintWidget.DIMENSION_HORIZONTAL] == ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;
            } else {
                isChainSpread = first.mVerticalChainStyle == ConstraintWidget.CHAIN_SPREAD;
                isChainPacked = first.mVerticalChainStyle == ConstraintWidget.CHAIN_PACKED;
                isWrapContent = constraintWidgetContainer.mListDimensionBehaviors[ConstraintWidget.DIMENSION_VERTICAL] == ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;
            }

            if (numMatchConstraints == 0 || isChainPacked) {
                applyConstraintsSimpleChain(constraintWidgetContainer.mChainEnds, constraintWidgetContainer.mListAnchors, system, orientation, isWrapContent, first, offset, dimensionOffset, isChainSpread, isChainPacked, currentWidget);
            } else {
                applyConstraintsComplexChain(constraintWidgetContainer.mChainEnds, constraintWidgetContainer.mMatchConstraintsChainedWidgets, system, orientation, isWrapContent, first, numMatchConstraints, offset, dimensionOffset, currentWidget);
            }
        }

    }

    /**
     * Apply constraints for a complex chain
     *
     * @param mChainEnds
     * @param mMatchConstraintsChainedWidgets
     * @param system
     * @param orientation
     * @param isWrapContent
     * @param first
     * @param numMatchConstraints
     * @param offset
     * @param dimensionOffset
     * @param currentWidget
     */
    private static void applyConstraintsComplexChain(ConstraintWidget[] mChainEnds, ConstraintWidget[] mMatchConstraintsChainedWidgets, LinearSystem system, int orientation, boolean isWrapContent, ConstraintWidget first, int numMatchConstraints, int offset, int dimensionOffset, ConstraintWidget currentWidget) {
        ConstraintWidget widget = first;
        ConstraintWidget previous = null;
        float totalWeights = 0;
        while (currentWidget != null) {
            if (currentWidget.mListDimensionBehaviors[dimensionOffset] != ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
                int margin = currentWidget.mListAnchors[offset].getMargin();
                if (previous != null) {
                    margin += previous.mListAnchors[offset + 1].getMargin();
                }
                int strength = SolverVariable.STRENGTH_FIXED;
                if (currentWidget.mListAnchors[offset].mTarget.mOwner.mListDimensionBehaviors[dimensionOffset] == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
                    strength = SolverVariable.STRENGTH_MEDIUM;
                }
                system.addEquality(currentWidget.mListAnchors[offset].mSolverVariable, currentWidget.mListAnchors[offset].mTarget.mSolverVariable, margin, strength);
                system.addGreaterThan(currentWidget.mListAnchors[offset].mSolverVariable, currentWidget.mListAnchors[offset].mTarget.mSolverVariable, margin, SolverVariable.STRENGTH_FIXED);
                margin = currentWidget.mListAnchors[offset + 1].getMargin();
                if (currentWidget.mListAnchors[offset + 1].mTarget.mOwner.mListAnchors[offset].mTarget != null && currentWidget.mListAnchors[offset + 1].mTarget.mOwner.mListAnchors[offset].mTarget.mOwner == currentWidget) {
                    margin += currentWidget.mListAnchors[offset + 1].mTarget.mOwner.mListAnchors[offset].getMargin();
                }
                strength = SolverVariable.STRENGTH_HIGH;
                if (currentWidget.mListAnchors[offset + 1].mTarget.mOwner.mListDimensionBehaviors[dimensionOffset] == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
                    strength = SolverVariable.STRENGTH_MEDIUM;
                }
                SolverVariable rightTarget = currentWidget.mListAnchors[offset + 1].mTarget.mSolverVariable;
                if (currentWidget == mChainEnds[CHAIN_LAST_VISIBLE]) {
                    rightTarget = mChainEnds[CHAIN_LAST].mListAnchors[offset + 1].mTarget.mSolverVariable;
                    strength = SolverVariable.STRENGTH_HIGH;
                }
                system.addEquality(currentWidget.mListAnchors[offset + 1].mSolverVariable, rightTarget, -margin, strength);
                system.addLowerThan(currentWidget.mListAnchors[offset + 1].mSolverVariable, rightTarget, -margin, SolverVariable.STRENGTH_FIXED);
            } else {
                totalWeights += currentWidget.mWeight[orientation];
                int margin = 0;
                if (currentWidget.mListAnchors[offset + 1].mTarget != null) {
                    margin = currentWidget.mListAnchors[offset + 1].getMargin();
                    if (currentWidget != mChainEnds[CHAIN_LAST_VISIBLE]) {
                        margin += currentWidget.mListAnchors[offset + 1].mTarget.mOwner.mListAnchors[offset].getMargin();
                    }
                }

                SolverVariable rightTarget = currentWidget.mListAnchors[offset + 1].mTarget.mSolverVariable;
                if (currentWidget == mChainEnds[CHAIN_LAST_VISIBLE]) {
                    rightTarget = mChainEnds[CHAIN_LAST].mListAnchors[offset + 1].mTarget.mSolverVariable;
                }

                system.addGreaterThan(currentWidget.mListAnchors[offset + 1].mSolverVariable, currentWidget.mListAnchors[offset].mSolverVariable, 0, SolverVariable.STRENGTH_FIXED);
                system.addLowerThan(currentWidget.mListAnchors[offset + 1].mSolverVariable, rightTarget, -margin, SolverVariable.STRENGTH_LOW);
            }
            previous = currentWidget;
            if (orientation == ConstraintWidget.HORIZONTAL) {
                currentWidget = currentWidget.mHorizontalNextWidget;
            } else {
                currentWidget = currentWidget.mVerticalNextWidget;
            }
        }
        if (numMatchConstraints == 1) {
            ConstraintWidget w = mMatchConstraintsChainedWidgets[0];
            int beginMargin = w.mListAnchors[offset].getMargin();
            if (w.mListAnchors[offset].mTarget != null) {
                beginMargin += w.mListAnchors[offset].mTarget.getMargin();
            }
            int endMargin = w.mListAnchors[offset + 1].getMargin();
            if (w.mListAnchors[offset + 1].mTarget != null) {
                endMargin += w.mListAnchors[offset + 1].mTarget.getMargin();
            }
            SolverVariable rightTarget = widget.mListAnchors[offset + 1].mTarget.mSolverVariable;
            if (w == mChainEnds[CHAIN_LAST_VISIBLE]) {
                rightTarget = mChainEnds[CHAIN_LAST].mListAnchors[offset + 1].mTarget.mSolverVariable;
            }

            boolean constraintWrap;
            if (orientation == ConstraintWidget.HORIZONTAL) {
                constraintWrap = w.mMatchConstraintDefaultWidth == ConstraintWidget.MATCH_CONSTRAINT_WRAP;
            } else {
                constraintWrap = w.mMatchConstraintDefaultHeight == ConstraintWidget.MATCH_CONSTRAINT_WRAP;
            }
            if (constraintWrap) {
                system.addGreaterThan(widget.mListAnchors[offset].mSolverVariable, widget.mListAnchors[offset].mTarget.mSolverVariable, beginMargin, SolverVariable.STRENGTH_FIXED);
                system.addLowerThan(widget.mListAnchors[offset + 1].mSolverVariable, rightTarget, -endMargin, SolverVariable.STRENGTH_FIXED);
                system.addEquality(widget.mListAnchors[offset + 1].mSolverVariable, widget.mListAnchors[offset].mSolverVariable, widget.getWidth(), SolverVariable.STRENGTH_MEDIUM);
            } else {
                system.addGreaterThan(w.mListAnchors[offset].mSolverVariable, w.mListAnchors[offset].mTarget.mSolverVariable, beginMargin, SolverVariable.STRENGTH_FIXED);
                system.addEquality(w.mListAnchors[offset].mSolverVariable, w.mListAnchors[offset].mTarget.mSolverVariable, beginMargin, SolverVariable.STRENGTH_LOW);
                system.addEquality(w.mListAnchors[offset + 1].mSolverVariable, rightTarget, -endMargin, SolverVariable.STRENGTH_LOW);
            }
        } else {
            for (int j = 0; j < numMatchConstraints - 1; j++) {
                ConstraintWidget current = mMatchConstraintsChainedWidgets[j];
                ConstraintWidget nextWidget = mMatchConstraintsChainedWidgets[j + 1];
                SolverVariable begin = current.mListAnchors[offset].mSolverVariable;
                SolverVariable end = current.mListAnchors[offset + 1].mSolverVariable;
                SolverVariable nextLeft = nextWidget.mListAnchors[offset].mSolverVariable;
                SolverVariable nextRight = nextWidget.mListAnchors[offset + 1].mSolverVariable;

                int margin = current.mListAnchors[offset].getMargin();
                if (current.mListAnchors[offset].mTarget != null && current.mListAnchors[offset].mTarget.mOwner.mListAnchors[offset + 1].mTarget != null
                        && current.mListAnchors[offset].mTarget.mOwner.mListAnchors[offset + 1].mTarget.mOwner == current) {
                    margin += current.mListAnchors[offset].mTarget.mOwner.mListAnchors[offset + 1].getMargin();
                }
                system.addEquality(begin, current.mListAnchors[offset].mTarget.mSolverVariable, margin, SolverVariable.STRENGTH_MEDIUM);
                system.addGreaterThan(begin, current.mListAnchors[offset].mTarget.mSolverVariable, margin, SolverVariable.STRENGTH_FIXED);
                margin = current.mListAnchors[offset + 1].getMargin();
                if (current.mListAnchors[offset + 1].mTarget != null && current.mHorizontalNextWidget != null) {
                    margin += current.mHorizontalNextWidget.mListAnchors[offset].mTarget != null ? current.mHorizontalNextWidget.mListAnchors[offset].getMargin() : 0;
                }
                system.addEquality(end, current.mListAnchors[offset + 1].mTarget.mSolverVariable, -margin, SolverVariable.STRENGTH_MEDIUM);
                system.addLowerThan(end, current.mListAnchors[offset + 1].mTarget.mSolverVariable, -margin, SolverVariable.STRENGTH_FIXED);
                if (j + 1 == numMatchConstraints - 1) {
                    // last element
                    margin = nextWidget.mListAnchors[offset].getMargin();
                    if (nextWidget.mListAnchors[offset].mTarget != null && nextWidget.mListAnchors[offset].mTarget.mOwner.mListAnchors[offset + 1].mTarget != null
                            && nextWidget.mListAnchors[offset].mTarget.mOwner.mListAnchors[offset + 1].mTarget.mOwner == nextWidget) {
                        margin += nextWidget.mListAnchors[offset].mTarget.mOwner.mListAnchors[offset + 1].getMargin();
                    }
                    system.addEquality(nextLeft, nextWidget.mListAnchors[offset].mTarget.mSolverVariable, margin, SolverVariable.STRENGTH_MEDIUM);
                    system.addGreaterThan(nextLeft, nextWidget.mListAnchors[offset].mTarget.mSolverVariable, margin, SolverVariable.STRENGTH_FIXED);
                    ConstraintAnchor anchor = nextWidget.mListAnchors[offset + 1];
                    if (nextWidget == mChainEnds[CHAIN_LAST_VISIBLE]) {
                        anchor = mChainEnds[CHAIN_LAST].mListAnchors[offset + 1];
                    }
                    margin = anchor.getMargin();
                    if (anchor.mTarget != null && anchor.mTarget.mOwner.mListAnchors[offset].mTarget != null
                            && anchor.mTarget.mOwner.mListAnchors[offset].mTarget.mOwner == nextWidget) {
                        margin += anchor.mTarget.mOwner.mListAnchors[offset].getMargin();
                    }
                    system.addEquality(nextRight, anchor.mTarget.mSolverVariable, -margin, SolverVariable.STRENGTH_MEDIUM);
                    system.addLowerThan(nextRight, anchor.mTarget.mSolverVariable, -margin, SolverVariable.STRENGTH_FIXED);
                }

                if (orientation == ConstraintWidget.HORIZONTAL) {
                    if (widget.mMatchConstraintMaxWidth > 0) {
                        system.addLowerThan(end, begin, widget.mMatchConstraintMaxWidth, SolverVariable.STRENGTH_MEDIUM);
                    }
                } else {
                    if (widget.mMatchConstraintMaxHeight > 0) {
                        system.addLowerThan(end, begin, widget.mMatchConstraintMaxHeight, SolverVariable.STRENGTH_MEDIUM);
                    }
                }

                ArrayRow row = system.createRow();
                float currentWeight;
                float nextWeight;
                currentWeight = current.mWeight[orientation];
                nextWeight = nextWidget.mWeight[orientation];
                row.createRowEqualDimension(currentWeight,
                        totalWeights, nextWeight,
                        begin, current.mListAnchors[offset].getMargin(),
                        end, current.mListAnchors[offset + 1].getMargin(),
                        nextLeft, nextWidget.mListAnchors[offset].getMargin(),
                        nextRight, nextWidget.mListAnchors[offset + 1].getMargin());

                system.addConstraint(row);
            }
        }
    }

    /**
     * Apply constraints for a simple chain
     *
     * @param mChainEnds
     * @param mListAnchors
     * @param system
     * @param orientation
     * @param isWrapContent
     * @param first
     * @param offset
     * @param dimensionOffset
     * @param isChainSpread
     * @param isChainPacked
     * @param currentWidget
     */
    private static void applyConstraintsSimpleChain(ConstraintWidget[] mChainEnds, ConstraintAnchor[] mListAnchors, LinearSystem system, int orientation, boolean isWrapContent, ConstraintWidget first, int offset, int dimensionOffset, boolean isChainSpread, boolean isChainPacked, ConstraintWidget currentWidget) {
        ConstraintWidget previousVisibleWidget = null;
        ConstraintWidget lastWidget = null;
        ConstraintWidget firstVisibleWidget = currentWidget;

        // Then iterate on the widgets, skipping the ones with visibility == GONE
        boolean isLast = false;
        ConstraintWidget next = null;
        while (currentWidget != null) {
            if (orientation == ConstraintWidget.HORIZONTAL) {
                next = currentWidget.mHorizontalNextWidget;
            } else {
                next = currentWidget.mVerticalNextWidget;
            }
            if (next == null) {
                lastWidget = mChainEnds[CHAIN_LAST];
                isLast = true;
            }
            if (isChainPacked) {
                ConstraintAnchor begin = currentWidget.mListAnchors[offset];
                int margin = begin.getMargin();
                if (previousVisibleWidget != null) {
                    int previousMargin = previousVisibleWidget.mListAnchors[offset + 1].getMargin();
                    margin += previousMargin;
                }
                int strength = SolverVariable.STRENGTH_LOW;
                if (firstVisibleWidget != currentWidget) {
                    strength = SolverVariable.STRENGTH_HIGHEST;
                }
                system.addGreaterThan(begin.mSolverVariable, begin.mTarget.mSolverVariable,
                        begin.mMargin, SolverVariable.STRENGTH_FIXED);
                system.addEquality(begin.mSolverVariable, begin.mTarget.mSolverVariable, margin, strength);
                if (currentWidget.mListDimensionBehaviors[dimensionOffset] == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
                    ConstraintAnchor end = currentWidget.mListAnchors[offset + 1];
                    boolean constraintWrap;
                    if (orientation == ConstraintWidget.HORIZONTAL) {
                        constraintWrap = currentWidget.mMatchConstraintDefaultWidth == ConstraintWidget.MATCH_CONSTRAINT_WRAP;
                    } else {
                        constraintWrap = currentWidget.mMatchConstraintDefaultHeight == ConstraintWidget.MATCH_CONSTRAINT_WRAP;
                    }

                    if (constraintWrap) {
                        int dimension = 0;
                        if (orientation == ConstraintWidget.HORIZONTAL) {
                            dimension = Math.max(currentWidget.mMatchConstraintMinWidth, currentWidget.getWidth());
                        } else {
                            dimension = Math.max(currentWidget.mMatchConstraintMinHeight, currentWidget.getHeight());
                        }
                        system.addEquality(end.mSolverVariable, begin.mSolverVariable,
                                dimension, SolverVariable.STRENGTH_HIGHEST);
                    } else {
                        int min = 0;
                        if (orientation == ConstraintWidget.HORIZONTAL) {
                            min = currentWidget.mMatchConstraintMinWidth;
                        } else {
                            min = currentWidget.mMatchConstraintMinHeight;
                        }
                        system.addEquality(end.mSolverVariable, begin.mSolverVariable,0, SolverVariable.STRENGTH_FIXED);
                        system.addLowerThan(end.mSolverVariable, begin.mSolverVariable, min, SolverVariable.STRENGTH_FIXED);
                    }
                }
            } else {
                if (!isChainSpread && isLast && previousVisibleWidget != null) {
                    if (currentWidget.mListAnchors[offset + 1].mTarget == null) {
                        int maxDraw;
                        if (orientation == ConstraintWidget.HORIZONTAL) {
                            maxDraw = currentWidget.getDrawRight();
                        } else {
                            maxDraw = currentWidget.getDrawBottom();
                        }
                        system.addEquality(currentWidget.mListAnchors[offset + 1].mSolverVariable, maxDraw);
                    } else {
                        int margin = currentWidget.mListAnchors[offset + 1].getMargin();
                        system.addEquality(currentWidget.mListAnchors[offset + 1].mSolverVariable, lastWidget.mListAnchors[offset + 1].mTarget.mSolverVariable, -margin, SolverVariable.STRENGTH_EQUALITY);
                    }
                } else if (!isChainSpread && !isLast && previousVisibleWidget == null) { // First element
                    if (currentWidget.mListAnchors[offset].mTarget == null) {
                        int minDraw;
                        if (orientation == ConstraintWidget.HORIZONTAL) {
                            minDraw = currentWidget.getDrawX();
                        } else {
                            minDraw = currentWidget.getDrawY();
                        }
                        system.addEquality(currentWidget.mListAnchors[offset].mSolverVariable, minDraw);
                    } else {
                        int margin = currentWidget.mListAnchors[offset].getMargin();
                        system.addEquality(currentWidget.mListAnchors[offset].mSolverVariable, first.mListAnchors[offset].mTarget.mSolverVariable, margin, SolverVariable.STRENGTH_EQUALITY);
                    }
                } else {
                    // Middle elements, let's center things
                    ConstraintAnchor begin = currentWidget.mListAnchors[offset];
                    ConstraintAnchor end = currentWidget.mListAnchors[offset + 1];
                    int beginMargin = begin.getMargin();
                    int endMargin = end.getMargin();
                    system.addGreaterThan(begin.mSolverVariable, begin.mTarget.mSolverVariable, beginMargin, SolverVariable.STRENGTH_FIXED);
                    system.addLowerThan(end.mSolverVariable, end.mTarget.mSolverVariable, -endMargin, SolverVariable.STRENGTH_FIXED);
                    SolverVariable leftTarget = begin.mTarget != null ? begin.mTarget.mSolverVariable : null;
                    if (previousVisibleWidget == null) {
                        // just in case we are dealing with a chain with only one visible element...
                        leftTarget = first.mListAnchors[offset].mTarget != null ? first.mListAnchors[offset].mTarget.mSolverVariable : null;
                    }
                    if (next == null) {
                        next = lastWidget.mListAnchors[offset + 1].mTarget != null ? lastWidget.mListAnchors[offset + 1].mTarget.mOwner : null;
                    }
                    if (next != null) {
                        SolverVariable rightTarget = next.mListAnchors[offset].mSolverVariable;
                        if (isLast) {
                            rightTarget = lastWidget.mListAnchors[offset + 1].mTarget != null ? lastWidget.mListAnchors[offset + 1].mTarget.mSolverVariable : null;
                        }
                        if (leftTarget != null && rightTarget != null) {
                            system.addCentering(begin.mSolverVariable, leftTarget, beginMargin, 0.5f,
                                    rightTarget, end.mSolverVariable, endMargin, SolverVariable.STRENGTH_HIGHEST);
                        }
                    }
                }
            }
            if (isWrapContent) {
                system.addGreaterThan(mListAnchors[offset + 1].mSolverVariable, currentWidget.mListAnchors[offset + 1].mSolverVariable, 0, SolverVariable.STRENGTH_FIXED);
            } else if (isLast && !isWrapContent) {
                system.addGreaterThan(mListAnchors[offset + 1].mSolverVariable, currentWidget.mListAnchors[offset + 1].mSolverVariable, 0, SolverVariable.STRENGTH_EQUALITY);
            }

            previousVisibleWidget = currentWidget;
            currentWidget = isLast ? null : next;
        }
        if (isChainPacked) {
            ConstraintAnchor begin = firstVisibleWidget.mListAnchors[offset];
            ConstraintAnchor end = lastWidget.mListAnchors[offset + 1];
            int beginMargin = begin.getMargin();
            int endMargin = end.getMargin();
            SolverVariable leftTarget = first.mListAnchors[offset].mTarget != null ? first.mListAnchors[offset].mTarget.mSolverVariable : null;
            SolverVariable rightTarget = lastWidget.mListAnchors[offset + 1].mTarget != null ? lastWidget.mListAnchors[offset + 1].mTarget.mSolverVariable : null;
            if (leftTarget != null && rightTarget != null) {
                float bias = 0.5f;
                if (orientation == ConstraintWidget.HORIZONTAL) {
                    bias = first.mHorizontalBiasPercent;
                } else {
                    bias = first.mVerticalBiasPercent;
                }
                system.addCentering(begin.mSolverVariable, leftTarget, beginMargin, bias,
                        rightTarget, end.mSolverVariable, endMargin, SolverVariable.STRENGTH_HIGHEST);
            }
        }
    }

    /**
     * Traverse a chain and fill the mMatchConstraintsChainedWidgets array with widgets
     * that are set to MATCH_CONSTRAINT, as we need to apply a common behavior to those
     * (we set their dimensions to be equal, minus their margins)
     */
    private static int countMatchConstraintsChainedWidgets(ConstraintWidgetContainer constraintWidgetContainer, LinearSystem system, ConstraintWidget[] chainEnds, ConstraintWidget widget, int direction, boolean[] flags) {
        int count = 0;
        flags[Optimizer.FLAG_USE_OPTIMIZE] = true; // will set to false if the chain is not optimizable
        flags[Optimizer.FLAG_CHAIN_DANGLING] = false; // will set to true if the chain is not connected on one or both endpoints
        chainEnds[CHAIN_FIRST] = null;
        chainEnds[CHAIN_FIRST_VISIBLE] = null;
        chainEnds[CHAIN_LAST] = null;
        chainEnds[CHAIN_LAST_VISIBLE] = null;

        if (direction == ConstraintWidget.HORIZONTAL) {
            boolean fixedPosition = true;
            ConstraintWidget first = widget;
            ConstraintWidget last = null;
            if (widget.mLeft.mTarget != null && widget.mLeft.mTarget.mOwner != constraintWidgetContainer) {
                fixedPosition = false;
            }
            widget.mHorizontalNextWidget = null;
            ConstraintWidget firstVisible = null;
            if (widget.getVisibility() != ConstraintWidget.GONE) {
                firstVisible = widget;
            }
            ConstraintWidget lastVisible = firstVisible;
            while (widget.mRight.mTarget != null) {
                widget.mHorizontalNextWidget = null;
                if (widget.getVisibility() != ConstraintWidget.GONE) {
                    if (firstVisible == null) {
                        firstVisible = widget;
                    }
                    if (lastVisible != null && lastVisible != widget) {
                        lastVisible.mHorizontalNextWidget = widget;
                    }
                    lastVisible = widget;
                } else {
                    system.addEquality(widget.mLeft.mSolverVariable, widget.mLeft.mTarget.mSolverVariable, 0, SolverVariable.STRENGTH_EQUALITY);
                    system.addEquality(widget.mRight.mSolverVariable, widget.mLeft.mSolverVariable, 0, SolverVariable.STRENGTH_EQUALITY);
                }
                if (widget.getVisibility() != ConstraintWidget.GONE && widget.mListDimensionBehaviors[ConstraintWidget.DIMENSION_HORIZONTAL] == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
                    if (widget.mListDimensionBehaviors[ConstraintWidget.DIMENSION_VERTICAL] == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
                        flags[Optimizer.FLAG_USE_OPTIMIZE] = false; // signal that this chain is not optimizable.
                    }
                    if (widget.mDimensionRatio <= 0) {
                        // we don't want to count ratio as match constraints (even if they are) as they should be handled
                        // as finite dimensions in the chain
                        flags[Optimizer.FLAG_USE_OPTIMIZE] = false; // signal that this chain is not optimizable.
                        if (count + 1 >= constraintWidgetContainer.mMatchConstraintsChainedWidgets.length) {
                            constraintWidgetContainer.mMatchConstraintsChainedWidgets = Arrays.copyOf(constraintWidgetContainer.mMatchConstraintsChainedWidgets, constraintWidgetContainer.mMatchConstraintsChainedWidgets.length * 2);
                        }
                        constraintWidgetContainer.mMatchConstraintsChainedWidgets[count++] = widget;
                    }
                }
                if (widget.mRight.mTarget.mOwner.mLeft.mTarget == null) {
                    break;
                }
                if (widget.mRight.mTarget.mOwner.mLeft.mTarget.mOwner != widget) {
                    break;
                }
                if (widget.mRight.mTarget.mOwner == widget) {
                    break;
                }
                widget = widget.mRight.mTarget.mOwner;
                last = widget;
            }
            if (widget.mRight.mTarget != null && widget.mRight.mTarget.mOwner != constraintWidgetContainer) {
                fixedPosition = false;
            }
            if (first.mLeft.mTarget == null || last.mRight.mTarget == null) {
                flags[Optimizer.FLAG_CHAIN_DANGLING] = true;
            }
            // keep track of the endpoints -- if both are fixed (for now, only look if they point to the parent)
            // we can optimize the resolution without passing by the solver
            first.mHorizontalChainFixedPosition = fixedPosition;
            last.mHorizontalNextWidget = null;
            chainEnds[CHAIN_FIRST] = first;
            chainEnds[CHAIN_FIRST_VISIBLE] = firstVisible;
            chainEnds[CHAIN_LAST] = last;
            chainEnds[CHAIN_LAST_VISIBLE] = lastVisible;
        } else {
            boolean fixedPosition = true;
            ConstraintWidget first = widget;
            ConstraintWidget last = null;
            if (widget.mTop.mTarget != null && widget.mTop.mTarget.mOwner != constraintWidgetContainer) {
                fixedPosition = false;
            }
            widget.mVerticalNextWidget = null;
            ConstraintWidget firstVisible = null;
            if (widget.getVisibility() != ConstraintWidget.GONE) {
                firstVisible = widget;
            }
            ConstraintWidget lastVisible = firstVisible;
            while (widget.mBottom.mTarget != null) {
                widget.mVerticalNextWidget = null;
                if (widget.getVisibility() != ConstraintWidget.GONE) {
                    if (firstVisible == null) {
                        firstVisible = widget;
                    }
                    if (lastVisible != null && lastVisible != widget) {
                        lastVisible.mVerticalNextWidget = widget;
                    }
                    lastVisible = widget;
                } else {
                    system.addEquality(widget.mTop.mSolverVariable, widget.mTop.mTarget.mSolverVariable, 0, SolverVariable.STRENGTH_EQUALITY);
                    system.addEquality(widget.mBottom.mSolverVariable, widget.mTop.mSolverVariable, 0, SolverVariable.STRENGTH_EQUALITY);
                }
                if (widget.getVisibility() != ConstraintWidget.GONE && widget.mListDimensionBehaviors[ConstraintWidget.DIMENSION_VERTICAL] == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
                    if (widget.mListDimensionBehaviors[ConstraintWidget.DIMENSION_HORIZONTAL] == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
                        flags[Optimizer.FLAG_USE_OPTIMIZE] = false; // signal that this chain is not optimizable.
                    }
                    if (widget.mDimensionRatio <= 0) {
                        flags[Optimizer.FLAG_USE_OPTIMIZE] = false; // signal that this chain is not optimizable.
                        if (count + 1 >= constraintWidgetContainer.mMatchConstraintsChainedWidgets.length) {
                            constraintWidgetContainer.mMatchConstraintsChainedWidgets = Arrays.copyOf(constraintWidgetContainer.mMatchConstraintsChainedWidgets, constraintWidgetContainer.mMatchConstraintsChainedWidgets.length * 2);
                        }
                        constraintWidgetContainer.mMatchConstraintsChainedWidgets[count++] = widget;
                    }
                }
                if (widget.mBottom.mTarget.mOwner.mTop.mTarget == null) {
                    break;
                }
                if (widget.mBottom.mTarget.mOwner.mTop.mTarget.mOwner != widget) {
                    break;
                }
                if (widget.mBottom.mTarget.mOwner == widget) {
                    break;
                }
                widget = widget.mBottom.mTarget.mOwner;
                last = widget;
            }
            if (widget.mBottom.mTarget != null && widget.mBottom.mTarget.mOwner != constraintWidgetContainer) {
                fixedPosition = false;
            }
            if (first.mTop.mTarget == null || last.mBottom.mTarget == null) {
                flags[Optimizer.FLAG_CHAIN_DANGLING] = true;
            }
            // keep track of the endpoints -- if both are fixed (for now, only look if they point to the parent)
            // we can optimize the resolution without passing by the solver
            first.mVerticalChainFixedPosition = fixedPosition;
            last.mVerticalNextWidget = null;
            chainEnds[CHAIN_FIRST] = first;
            chainEnds[CHAIN_FIRST_VISIBLE] = firstVisible;
            chainEnds[CHAIN_LAST] = last;
            chainEnds[CHAIN_LAST_VISIBLE] = lastVisible;
        }
        return count;
    }
}
