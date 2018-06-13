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

import static android.support.constraint.solver.widgets.ConstraintWidget.*;

/**
 * Chain management and constraints creation
 */
class Chain {

    private static final boolean DEBUG = false;

    /**
     * Apply specific rules for dealing with chains of widgets.
     * Chains are defined as a list of widget linked together with bi-directional connections
     *
     * @param constraintWidgetContainer root container
     * @param system the linear system we add the equations to
     * @param orientation HORIZONTAL or VERTICAL
     */
    static void applyChainConstraints(ConstraintWidgetContainer constraintWidgetContainer, LinearSystem system, int orientation) {

        // what to do:
        // Don't skip things. Either the element is GONE or not.
        int offset = 0;
        int chainsSize = 0;
        ConstraintWidget[] chainsArray = null;
        if (orientation == ConstraintWidget.HORIZONTAL) {
            offset = 0;
            chainsSize = constraintWidgetContainer.mHorizontalChainsSize;
            chainsArray = constraintWidgetContainer.mHorizontalChainsArray;
        } else {
            offset = 2;
            chainsSize = constraintWidgetContainer.mVerticalChainsSize;
            chainsArray = constraintWidgetContainer.mVerticalChainsArray;
        }
        for (int i = 0; i < chainsSize; i++) {
            ConstraintWidget first = chainsArray[i];
            if (constraintWidgetContainer.optimizeFor(Optimizer.OPTIMIZATION_CHAIN)) {
                if (!Optimizer.applyChainOptimized(constraintWidgetContainer, system, orientation, offset, first)) {
                    applyChainConstraints(constraintWidgetContainer, system, orientation, offset, first);
                }
            } else {
                applyChainConstraints(constraintWidgetContainer, system, orientation, offset, first);
            }
        }
    }

    /**
     * Apply specific rules for dealing with chains of widgets.
     * Chains are defined as a list of widget linked together with bi-directional connections
     *
     * @param container the root container
     * @param system the linear system we add the equations to
     * @param orientation HORIZONTAL or VERTICAL
     * @param offset 0 or 2 to accomodate for HORIZONTAL / VERTICAL
     * @param first first widget of the chain
     */
    static void applyChainConstraints(ConstraintWidgetContainer container, LinearSystem system,
                                      int orientation, int offset, ConstraintWidget first) {
        ConstraintWidget widget = first;
        ConstraintWidget next = null;
        ConstraintWidget firstVisibleWidget = null;
        ConstraintWidget lastVisibleWidget = null;
        boolean done = false;
        int numMatchConstraints = 0;
        float totalWeights = 0;
        ConstraintWidget firstMatchConstraintsWidget = null;
        ConstraintWidget previousMatchConstraintsWidget = null;

        boolean isWrapContent = container.mListDimensionBehaviors[orientation] == ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;
        boolean isChainSpread = false;
        boolean isChainSpreadInside = false;
        boolean isChainPacked = false;

        ConstraintWidget head = first;
        if (orientation == ConstraintWidget.HORIZONTAL && container.isRtl()) {
            // find the last widget
            while (!done) {
                // go to the next widget
                ConstraintAnchor nextAnchor = widget.mListAnchors[offset + 1].mTarget;
                if (nextAnchor != null) {
                    next = nextAnchor.mOwner;
                    if (next.mListAnchors[offset].mTarget == null
                            || next.mListAnchors[offset].mTarget.mOwner != widget) {
                        next = null;
                    }
                } else {
                    next = null;
                }
                if (next != null) {
                    widget = next;
                } else {
                    done = true;
                }
            }
            head = widget;
            widget = first;
            next = null;
            done = false;
        }

        if (orientation == ConstraintWidget.HORIZONTAL) {
            isChainSpread = head.mHorizontalChainStyle == ConstraintWidget.CHAIN_SPREAD;
            isChainSpreadInside = head.mHorizontalChainStyle == ConstraintWidget.CHAIN_SPREAD_INSIDE;
            isChainPacked = head.mHorizontalChainStyle == ConstraintWidget.CHAIN_PACKED;
        } else {
            isChainSpread = head.mVerticalChainStyle == ConstraintWidget.CHAIN_SPREAD;
            isChainSpreadInside = head.mVerticalChainStyle == ConstraintWidget.CHAIN_SPREAD_INSIDE;
            isChainPacked = head.mVerticalChainStyle == ConstraintWidget.CHAIN_PACKED;
        }

        // The first traversal will:
        // - set up some basic ordering constraints
        // - build a linked list of visible widgets
        // - build a linked list of matched constraints widgets

        while (!done) {
            // apply ordering on the current widget

            // First, let's maintain a linked list of visible widgets for the chain
            widget.mListNextVisibleWidget[orientation] = null;
            if (widget.getVisibility() != ConstraintWidget.GONE) {
                if (lastVisibleWidget != null) {
                    lastVisibleWidget.mListNextVisibleWidget[orientation] = widget;
                }
                if (firstVisibleWidget == null) {
                    firstVisibleWidget = widget;
                }
                lastVisibleWidget = widget;
            }

            ConstraintAnchor begin = widget.mListAnchors[offset];
            int strength = SolverVariable.STRENGTH_HIGHEST;
            if (isWrapContent || isChainPacked) {
                strength = SolverVariable.STRENGTH_LOW;
            }
            int margin = begin.getMargin();

            if (begin.mTarget != null && widget != first) {
                margin += begin.mTarget.getMargin();
            }

            if (isChainPacked && widget != first && widget != firstVisibleWidget) {
                strength = SolverVariable.STRENGTH_FIXED;
            } else if (isChainSpread && isWrapContent) {
                // on chain spread, keep the default strength connecting to endpoints to highest
                // this makes it on par with ratio strength.
                strength = SolverVariable.STRENGTH_HIGHEST;
            }

            if (begin.mTarget != null) {
                if (widget == firstVisibleWidget) {
                    system.addGreaterThan(begin.mSolverVariable, begin.mTarget.mSolverVariable,
                        margin, SolverVariable.STRENGTH_EQUALITY);
                } else {
                    system.addGreaterThan(begin.mSolverVariable, begin.mTarget.mSolverVariable,
                        margin, SolverVariable.STRENGTH_FIXED);
                }
                system.addEquality(begin.mSolverVariable, begin.mTarget.mSolverVariable, margin,
                    strength);
            }

            // First, let's maintain a linked list of matched widgets for the chain
            widget.mListNextMatchConstraintsWidget[orientation] = null;
            if (widget.getVisibility() != ConstraintWidget.GONE
                    && widget.mListDimensionBehaviors[orientation] == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
                numMatchConstraints++;
                totalWeights += widget.mWeight[orientation];
                if (firstMatchConstraintsWidget == null) {
                    firstMatchConstraintsWidget = widget;
                } else {
                    previousMatchConstraintsWidget.mListNextMatchConstraintsWidget[orientation] = widget;
                }
                previousMatchConstraintsWidget = widget;
                if (isWrapContent) {
                    system.addGreaterThan(widget.mListAnchors[offset + 1].mSolverVariable,
                        widget.mListAnchors[offset].mSolverVariable, 0,
                        SolverVariable.STRENGTH_EQUALITY);
                }
            }

            if (isWrapContent) {
                system.addGreaterThan(widget.mListAnchors[offset].mSolverVariable,
                        container.mListAnchors[offset].mSolverVariable,
                        0, SolverVariable.STRENGTH_FIXED);
            }

            // go to the next widget
            ConstraintAnchor nextAnchor = widget.mListAnchors[offset + 1].mTarget;
            if (nextAnchor != null) {
                next = nextAnchor.mOwner;
                if (next.mListAnchors[offset].mTarget == null || next.mListAnchors[offset].mTarget.mOwner != widget) {
                    next = null;
                }
            } else {
                next = null;
            }
            if (next != null) {
                widget = next;
            } else {
                done = true;
            }
        }
        ConstraintWidget last = widget;

        // Make sure we have constraints for the last anchors / targets
        if (lastVisibleWidget != null && last.mListAnchors[offset + 1].mTarget != null) {
            ConstraintAnchor end = lastVisibleWidget.mListAnchors[offset + 1];
            system.addLowerThan(end.mSolverVariable,
                    last.mListAnchors[offset + 1].mTarget.mSolverVariable, -end.getMargin(),
                    SolverVariable.STRENGTH_EQUALITY);
        }

        // ... and make sure the root end is constrained in wrap content.
        if (isWrapContent) {
            system.addGreaterThan(container.mListAnchors[offset + 1].mSolverVariable,
                    last.mListAnchors[offset + 1].mSolverVariable,
                    last.mListAnchors[offset + 1].getMargin(), SolverVariable.STRENGTH_FIXED);
        }

        // Now, let's apply the centering / spreading for matched constraints widgets
        if (numMatchConstraints > 0) {
            // TODO: we should not try to apply the constraints for weights = 0
            widget = firstMatchConstraintsWidget;
            while (widget != null) {
                next = widget.mListNextMatchConstraintsWidget[orientation];
                if (next != null) {
                    float currentWeight = widget.mWeight[orientation];
                    float nextWeight = next.mWeight[orientation];
                    SolverVariable begin = widget.mListAnchors[offset].mSolverVariable;
                    SolverVariable end = widget.mListAnchors[offset + 1].mSolverVariable;
                    SolverVariable nextBegin = next.mListAnchors[offset].mSolverVariable;
                    SolverVariable nextEnd = next.mListAnchors[offset + 1].mSolverVariable;

                    boolean applyEquality;
                    int currentDimensionDefault;
                    int nextDimensionDefault;
                    if (orientation == ConstraintWidget.HORIZONTAL) {
                        currentDimensionDefault = widget.mMatchConstraintDefaultWidth;
                        nextDimensionDefault = next.mMatchConstraintDefaultWidth;
                    } else {
                        currentDimensionDefault = widget.mMatchConstraintDefaultHeight;
                        nextDimensionDefault = next.mMatchConstraintDefaultHeight;
                    }
                    applyEquality = ((currentDimensionDefault == MATCH_CONSTRAINT_SPREAD)
                            || (currentDimensionDefault == MATCH_CONSTRAINT_RATIO)) &&
                            ((nextDimensionDefault == MATCH_CONSTRAINT_SPREAD)
                                    || (nextDimensionDefault == MATCH_CONSTRAINT_RATIO));

                    if (applyEquality) {
                        ArrayRow row = system.createRow();
                        row.createRowEqualMatchDimensions(currentWeight, totalWeights, nextWeight,
                                begin, end, nextBegin, nextEnd);
                        system.addConstraint(row);
                    }

                }
                widget = next;
            }
        }

        if (DEBUG) {
            widget = firstVisibleWidget;
            while (widget != null) {
                next = widget.mListNextVisibleWidget[orientation];
                widget.mListAnchors[offset].mSolverVariable.setName("" + widget.getDebugName() + ".left");
                widget.mListAnchors[offset + 1].mSolverVariable.setName("" + widget.getDebugName() + ".right");
                widget = next;
            }
        }

        // Finally, let's apply the specific rules dealing with the different chain types

        if (firstVisibleWidget != null && (firstVisibleWidget == lastVisibleWidget || isChainPacked)) {
            ConstraintAnchor begin = first.mListAnchors[offset];
            ConstraintAnchor end = last.mListAnchors[offset + 1];
            SolverVariable beginTarget = first.mListAnchors[offset].mTarget != null ? first.mListAnchors[offset].mTarget.mSolverVariable : null;
            SolverVariable endTarget = last.mListAnchors[offset + 1].mTarget != null ? last.mListAnchors[offset + 1].mTarget.mSolverVariable : null;
            if (firstVisibleWidget == lastVisibleWidget) {
                begin = firstVisibleWidget.mListAnchors[offset];
                end = firstVisibleWidget.mListAnchors[offset + 1];
            }
            if (beginTarget != null && endTarget != null) {
                float bias = 0.5f;
                if (orientation == ConstraintWidget.HORIZONTAL) {
                    bias = head.mHorizontalBiasPercent;
                } else {
                    bias = head.mVerticalBiasPercent;
                }
                int beginMargin = begin.getMargin();
                int endMargin = end.getMargin();
                system.addCentering(begin.mSolverVariable, beginTarget, beginMargin, bias,
                        endTarget, end.mSolverVariable, endMargin, SolverVariable.STRENGTH_EQUALITY);
            }
        } else if (isChainSpread && firstVisibleWidget != null) {
            // for chain spread, we need to add equal dimensions in between *visible* widgets
            widget = firstVisibleWidget;
            ConstraintWidget previousVisibleWidget = firstVisibleWidget;
            while (widget != null) {
                next = widget.mListNextVisibleWidget[orientation];
                if (next != null || widget == lastVisibleWidget) {
                    ConstraintAnchor beginAnchor = widget.mListAnchors[offset];
                    SolverVariable begin = beginAnchor.mSolverVariable;
                    SolverVariable beginTarget = beginAnchor.mTarget != null ? beginAnchor.mTarget.mSolverVariable : null;
                    if (previousVisibleWidget != widget) {
                        beginTarget = previousVisibleWidget.mListAnchors[offset + 1].mSolverVariable;
                    } else if (widget == firstVisibleWidget && previousVisibleWidget == widget) {
                        beginTarget = first.mListAnchors[offset].mTarget != null ? first.mListAnchors[offset].mTarget.mSolverVariable : null;
                    }

                    ConstraintAnchor beginNextAnchor = null;
                    SolverVariable beginNext = null;
                    SolverVariable beginNextTarget = null;
                    int beginMargin = beginAnchor.getMargin();
                    int nextMargin = widget.mListAnchors[offset + 1].getMargin();

                    if (next != null) {
                        beginNextAnchor = next.mListAnchors[offset];
                        beginNext = beginNextAnchor.mSolverVariable;
                        beginNextTarget = widget.mListAnchors[offset + 1].mSolverVariable;
                    } else {
                        beginNextAnchor = last.mListAnchors[offset + 1].mTarget;
                        if (beginNextAnchor != null) {
                            beginNext = beginNextAnchor.mSolverVariable;
                        }
                        beginNextTarget = widget.mListAnchors[offset + 1].mSolverVariable;
                    }

                    if (beginNextAnchor != null) {
                        nextMargin += beginNextAnchor.getMargin();
                    }
                    if (previousVisibleWidget != null) {
                        beginMargin += previousVisibleWidget.mListAnchors[offset + 1].getMargin();
                    }
                    if (begin != null && beginTarget != null && beginNext != null && beginNextTarget != null) {
                        int margin1 = beginMargin;
                        if (widget == firstVisibleWidget) {
                            margin1 = firstVisibleWidget.mListAnchors[offset].getMargin();
                        }
                        int margin2 = nextMargin;
                        if (widget == lastVisibleWidget) {
                            margin2 = lastVisibleWidget.mListAnchors[offset + 1].getMargin();
                        }
                        system.addCentering(begin, beginTarget, margin1, 0.5f,
                                beginNext, beginNextTarget, margin2,
                                SolverVariable.STRENGTH_HIGHEST);
                    }
                }
                previousVisibleWidget = widget;
                widget = next;
            }
        } else if (isChainSpreadInside && firstVisibleWidget != null) {
            // for chain spread inside, we need to add equal dimensions in between *visible* widgets
            widget = firstVisibleWidget;
            ConstraintWidget previousVisibleWidget = firstVisibleWidget;
            while (widget != null) {
                next = widget.mListNextVisibleWidget[orientation];
                if (widget != firstVisibleWidget && widget != lastVisibleWidget && next != null) {
                    if (next == lastVisibleWidget) {
                        next = null;
                    }
                    ConstraintAnchor beginAnchor = widget.mListAnchors[offset];
                    SolverVariable begin = beginAnchor.mSolverVariable;
                    SolverVariable beginTarget = beginAnchor.mTarget != null ? beginAnchor.mTarget.mSolverVariable : null;
                    beginTarget = previousVisibleWidget.mListAnchors[offset + 1].mSolverVariable;
                    ConstraintAnchor beginNextAnchor = null;
                    SolverVariable beginNext = null;
                    SolverVariable beginNextTarget = null;
                    int beginMargin = beginAnchor.getMargin();
                    int nextMargin = widget.mListAnchors[offset + 1].getMargin();

                    if (next != null) {
                        beginNextAnchor = next.mListAnchors[offset];
                        beginNext = beginNextAnchor.mSolverVariable;
                        beginNextTarget = beginNextAnchor.mTarget != null ? beginNextAnchor.mTarget.mSolverVariable : null;
                    } else {
                        beginNextAnchor = widget.mListAnchors[offset + 1].mTarget;
                        if (beginNextAnchor != null) {
                            beginNext = beginNextAnchor.mSolverVariable;
                        }
                        beginNextTarget = widget.mListAnchors[offset + 1].mSolverVariable;
                    }

                    if (beginNextAnchor != null) {
                        nextMargin += beginNextAnchor.getMargin();
                    }
                    if (previousVisibleWidget != null) {
                        beginMargin += previousVisibleWidget.mListAnchors[offset + 1].getMargin();
                    }
                    if (begin != null && beginTarget != null && beginNext != null && beginNextTarget != null) {
                        system.addCentering(begin, beginTarget, beginMargin, 0.5f,
                                beginNext, beginNextTarget, nextMargin,
                                SolverVariable.STRENGTH_HIGHEST);
                    }
                }
                previousVisibleWidget = widget;
                widget = next;
            }
            ConstraintAnchor begin = firstVisibleWidget.mListAnchors[offset];
            ConstraintAnchor beginTarget = first.mListAnchors[offset].mTarget;
            ConstraintAnchor end = lastVisibleWidget.mListAnchors[offset + 1];
            ConstraintAnchor endTarget = last.mListAnchors[offset + 1].mTarget;
            if (beginTarget != null) {
                if (firstVisibleWidget != lastVisibleWidget) {
                    system.addEquality(begin.mSolverVariable, beginTarget.mSolverVariable, begin.getMargin(), SolverVariable.STRENGTH_EQUALITY);
                } else if (endTarget != null) {
                    system.addCentering(begin.mSolverVariable, beginTarget.mSolverVariable, begin.getMargin(), 0.5f,
                            end.mSolverVariable, endTarget.mSolverVariable, end.getMargin(), SolverVariable.STRENGTH_EQUALITY);
                }
            }
            if (endTarget != null && (firstVisibleWidget != lastVisibleWidget)) {
                system.addEquality(end.mSolverVariable, endTarget.mSolverVariable, -end.getMargin(), SolverVariable.STRENGTH_EQUALITY);
            }

        }

        // final centering, necessary if the chain is smaller than the available space...
        if ((isChainSpread || isChainSpreadInside) && firstVisibleWidget != null) {
            ConstraintAnchor begin = firstVisibleWidget.mListAnchors[offset];
            ConstraintAnchor end = lastVisibleWidget.mListAnchors[offset + 1];
            SolverVariable beginTarget = begin.mTarget != null ? begin.mTarget.mSolverVariable : null;
            SolverVariable endTarget = end.mTarget != null ? end.mTarget.mSolverVariable : null;
            if (last != lastVisibleWidget) {
                ConstraintAnchor realEnd = last.mListAnchors[offset + 1];
                endTarget = realEnd.mTarget != null ? realEnd.mTarget.mSolverVariable : null;
            }
            if (firstVisibleWidget == lastVisibleWidget) {
                begin = firstVisibleWidget.mListAnchors[offset];
                end = firstVisibleWidget.mListAnchors[offset + 1];
            }
            if (beginTarget != null && endTarget != null) {
                float bias = 0.5f;
                int beginMargin = begin.getMargin();
                if (lastVisibleWidget == null) {
                    // everything is hidden
                    lastVisibleWidget = last;
                }
                int endMargin = lastVisibleWidget.mListAnchors[offset + 1].getMargin();
                system.addCentering(begin.mSolverVariable, beginTarget, beginMargin, bias,
                        endTarget, end.mSolverVariable, endMargin, SolverVariable.STRENGTH_EQUALITY);
            }
        }
    }
}
