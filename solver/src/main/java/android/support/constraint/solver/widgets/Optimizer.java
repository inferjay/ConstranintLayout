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

import java.util.ArrayList;

import static android.support.constraint.solver.widgets.ConstraintWidget.*;
import static android.support.constraint.solver.widgets.ConstraintWidget.DimensionBehaviour.FIXED;
import static android.support.constraint.solver.widgets.ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT;

/**
 * Implements direct resolution without using the solver
 */
public class Optimizer {

    // Optimization levels (mask)
    public static final int OPTIMIZATION_NONE  = 1;
    public static final int OPTIMIZATION_GRAPH = 1 << 1;
    public static final int OPTIMIZATION_BASIC = 1 << 2;
    public static final int OPTIMIZATION_CHAIN = 1 << 3;
    public static final int OPTIMIZATION_RATIO = 1 << 4;
    public static final int OPTIMIZATION_ALL = OPTIMIZATION_GRAPH | OPTIMIZATION_BASIC /* | OPTIMIZATION_CHAIN */;

    // Internal use.
    static boolean[] flags = new boolean[3];
    static final int FLAG_USE_OPTIMIZE = 0; // simple enough to use optimizer
    static final int FLAG_CHAIN_DANGLING = 1;
    static final int FLAG_RECOMPUTE_BOUNDS = 2;

    /**
     * Looks at optimizing match_parent
     *
     * @param container
     * @param system
     * @param widget
     */
    static void checkMatchParent(ConstraintWidgetContainer container, LinearSystem system, ConstraintWidget widget) {
        if (container.mListDimensionBehaviors[DIMENSION_HORIZONTAL] != ConstraintWidget.DimensionBehaviour.WRAP_CONTENT
            && widget.mListDimensionBehaviors[DIMENSION_HORIZONTAL] == ConstraintWidget.DimensionBehaviour.MATCH_PARENT) {

            int left = widget.mLeft.mMargin;
            int right = container.getWidth() - widget.mRight.mMargin;

            if (false) {
                // TODO : activate this
                widget.mLeft.getResolutionNode().resolve(null, left);
                widget.mRight.getResolutionNode().resolve(null, right);
            } else {
                widget.mLeft.mSolverVariable = system.createObjectVariable(widget.mLeft);
                widget.mRight.mSolverVariable = system.createObjectVariable(widget.mRight);
                system.addEquality(widget.mLeft.mSolverVariable, left);
                system.addEquality(widget.mRight.mSolverVariable, right);
                widget.mHorizontalResolution = ConstraintWidget.DIRECT;
            }
            widget.setHorizontalDimension(left, right);
        }
        if (container.mListDimensionBehaviors[DIMENSION_VERTICAL] != ConstraintWidget.DimensionBehaviour.WRAP_CONTENT
            && widget.mListDimensionBehaviors[DIMENSION_VERTICAL] == ConstraintWidget.DimensionBehaviour.MATCH_PARENT) {

            int top = widget.mTop.mMargin;
            int bottom = container.getHeight() - widget.mBottom.mMargin;

            if (false) {
                // TODO : activate this
                widget.mTop.getResolutionNode().resolve(null, top);
                widget.mBottom.getResolutionNode().resolve(null, bottom);
            } else {
                widget.mTop.mSolverVariable = system.createObjectVariable(widget.mTop);
                widget.mBottom.mSolverVariable = system.createObjectVariable(widget.mBottom);
                system.addEquality(widget.mTop.mSolverVariable, top);
                system.addEquality(widget.mBottom.mSolverVariable, bottom);
                if (widget.mBaselineDistance > 0 || widget.getVisibility() == ConstraintWidget.GONE) {
                    widget.mBaseline.mSolverVariable = system.createObjectVariable(widget.mBaseline);
                    system.addEquality(widget.mBaseline.mSolverVariable, top + widget.mBaselineDistance);
                }
                widget.mVerticalResolution = ConstraintWidget.DIRECT;
            }
            widget.setVerticalDimension(top, bottom);
        }
    }

    /**
     * Returns true if the given widget is optimizable.
     *
     * This function allows us to decide which patterns are deemed directly optimizable.
     *
     * @param constraintWidget
     * @param orientation
     * @return true if optimizable
     */
    private static boolean optimizableMatchConstraint(ConstraintWidget constraintWidget, int orientation) {
        if (constraintWidget.mListDimensionBehaviors[orientation] != MATCH_CONSTRAINT) {
            return false;
        }
        if (constraintWidget.mDimensionRatio != 0) {
            if (constraintWidget.mListDimensionBehaviors[orientation == HORIZONTAL ? VERTICAL : HORIZONTAL]
                    == MATCH_CONSTRAINT) {
                return false;
            }
            // TODO -- makes this work in the optimizer -- this will be OPTIMIZATION_RATIO
            return false;
        }
        if (orientation == HORIZONTAL) {
            if (constraintWidget.mMatchConstraintDefaultWidth != MATCH_CONSTRAINT_SPREAD) {
                return false;
            }
            if (constraintWidget.mMatchConstraintMinWidth != 0 || constraintWidget.mMatchConstraintMaxWidth != 0) {
                return false;
            }
        } else {
            if (constraintWidget.mMatchConstraintDefaultHeight != MATCH_CONSTRAINT_SPREAD) {
                return false;
            }
            if (constraintWidget.mMatchConstraintMinHeight != 0 || constraintWidget.mMatchConstraintMaxHeight != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Creates the dependency graph for the given widget
     *
     * @param widget
     */
    static void analyze(ConstraintWidget widget) {

        // Let's optimize guidelines
        if (widget instanceof Guideline) {
            Guideline guideline = (Guideline) widget;
            ConstraintWidget constraintWidgetContainer = widget.getParent();
            if (constraintWidgetContainer == null) {
                return;
            }
            if (guideline.getOrientation() == Guideline.VERTICAL) {
                guideline.mTop.getResolutionNode().dependsOn(ResolutionNode.DIRECT_CONNECTION,constraintWidgetContainer.mTop.getResolutionNode(), 0);
                guideline.mBottom.getResolutionNode().dependsOn(ResolutionNode.DIRECT_CONNECTION, constraintWidgetContainer.mTop.getResolutionNode(), 0);
                if (guideline.mRelativeBegin != -1) {
                    guideline.mLeft.getResolutionNode().dependsOn(ResolutionNode.DIRECT_CONNECTION, constraintWidgetContainer.mLeft.getResolutionNode(), guideline.mRelativeBegin);
                    guideline.mRight.getResolutionNode().dependsOn(ResolutionNode.DIRECT_CONNECTION, constraintWidgetContainer.mLeft.getResolutionNode(), guideline.mRelativeBegin);
                } else if (guideline.mRelativeEnd != -1) {
                    guideline.mLeft.getResolutionNode().dependsOn(ResolutionNode.DIRECT_CONNECTION, constraintWidgetContainer.mRight.getResolutionNode(), -guideline.mRelativeEnd);
                    guideline.mRight.getResolutionNode().dependsOn(ResolutionNode.DIRECT_CONNECTION, constraintWidgetContainer.mRight.getResolutionNode(), -guideline.mRelativeEnd);
                } else if (guideline.mRelativePercent != -1 && constraintWidgetContainer.getHorizontalDimensionBehaviour() == FIXED) {
                    int position = (int) (constraintWidgetContainer.mWidth * guideline.mRelativePercent);
                    guideline.mLeft.getResolutionNode().dependsOn(ResolutionNode.DIRECT_CONNECTION, constraintWidgetContainer.mLeft.getResolutionNode(), position);
                    guideline.mRight.getResolutionNode().dependsOn(ResolutionNode.DIRECT_CONNECTION, constraintWidgetContainer.mLeft.getResolutionNode(), position);
                }
            } else {
                guideline.mLeft.getResolutionNode().dependsOn(ResolutionNode.DIRECT_CONNECTION, constraintWidgetContainer.mLeft.getResolutionNode(), 0);
                guideline.mRight.getResolutionNode().dependsOn(ResolutionNode.DIRECT_CONNECTION, constraintWidgetContainer.mLeft.getResolutionNode(), 0);
                if (guideline.mRelativeBegin != -1) {
                    guideline.mTop.getResolutionNode().dependsOn(ResolutionNode.DIRECT_CONNECTION, constraintWidgetContainer.mTop.getResolutionNode(), guideline.mRelativeBegin);
                    guideline.mBottom.getResolutionNode().dependsOn(ResolutionNode.DIRECT_CONNECTION, constraintWidgetContainer.mTop.getResolutionNode(), guideline.mRelativeBegin);
                } else if (guideline.mRelativeEnd != -1) {
                    guideline.mTop.getResolutionNode().dependsOn(ResolutionNode.DIRECT_CONNECTION, constraintWidgetContainer.mBottom.getResolutionNode(), -guideline.mRelativeEnd);
                    guideline.mBottom.getResolutionNode().dependsOn(ResolutionNode.DIRECT_CONNECTION, constraintWidgetContainer.mBottom.getResolutionNode(), -guideline.mRelativeEnd);
                } else if (guideline.mRelativePercent != -1 && constraintWidgetContainer.getVerticalDimensionBehaviour() == FIXED) {
                    int position = (int) (constraintWidgetContainer.mHeight * guideline.mRelativePercent);
                    guideline.mTop.getResolutionNode().dependsOn(ResolutionNode.DIRECT_CONNECTION, constraintWidgetContainer.mTop.getResolutionNode(), position);
                    guideline.mBottom.getResolutionNode().dependsOn(ResolutionNode.DIRECT_CONNECTION, constraintWidgetContainer.mTop.getResolutionNode(), position);
                }
            }
            return;
        }

        // Let's update the graph from the nodes!
        // This will only apply if the nodes are not part of a chain.
        // It will set up direct connections depending on the intrinsic size (fixed or match_constraint)
        // as well as identifying centered connections and match connections.

        widget.updateResolutionNodes();

        ResolutionNode leftNode = widget.mLeft.getResolutionNode();
        ResolutionNode topNode = widget.mTop.getResolutionNode();
        ResolutionNode rightNode = widget.mRight.getResolutionNode();
        ResolutionNode bottomNode = widget.mBottom.getResolutionNode();

        // First the horizontal nodes...

        if (leftNode.type != ResolutionNode.CHAIN_CONNECTION
                && rightNode.type != ResolutionNode.CHAIN_CONNECTION) {
            if (widget.mListDimensionBehaviors[HORIZONTAL] == FIXED) {
                if (widget.mLeft.mTarget == null && widget.mRight.mTarget == null) {
                    leftNode.setType(ResolutionNode.DIRECT_CONNECTION);
                    rightNode.setType(ResolutionNode.DIRECT_CONNECTION);
                    rightNode.dependsOn(leftNode, widget.getWidth());
                } else if (widget.mLeft.mTarget != null && widget.mRight.mTarget == null) {
                    leftNode.setType(ResolutionNode.DIRECT_CONNECTION);
                    rightNode.setType(ResolutionNode.DIRECT_CONNECTION);
                    rightNode.dependsOn(leftNode, widget.getWidth());
                } else if (widget.mLeft.mTarget == null && widget.mRight.mTarget != null) {
                    leftNode.setType(ResolutionNode.DIRECT_CONNECTION);
                    rightNode.setType(ResolutionNode.DIRECT_CONNECTION);
                    leftNode.dependsOn(rightNode, -widget.getWidth());
                } else if (widget.mLeft.mTarget != null && widget.mRight.mTarget != null) {
                    leftNode.setType(ResolutionNode.CENTER_CONNECTION);
                    rightNode.setType(ResolutionNode.CENTER_CONNECTION);
                    leftNode.setOpposite(rightNode, -widget.getWidth());
                    rightNode.setOpposite(leftNode, widget.getWidth());
                }
            } else if (widget.mListDimensionBehaviors[HORIZONTAL] == MATCH_CONSTRAINT
                    && optimizableMatchConstraint(widget, HORIZONTAL)) {
                int width = widget.getWidth();
                if (widget.mDimensionRatio != 0) {
                    width = (int) (widget.getHeight() * widget.mDimensionRatio);
                }
                leftNode.setType(ResolutionNode.DIRECT_CONNECTION);
                rightNode.setType(ResolutionNode.DIRECT_CONNECTION);
                if (widget.mLeft.mTarget == null && widget.mRight.mTarget == null) {
                    rightNode.dependsOn(leftNode, width);
                } else if (widget.mLeft.mTarget != null && widget.mRight.mTarget == null) {
                    rightNode.dependsOn(leftNode, width);
                } else if (widget.mLeft.mTarget == null && widget.mRight.mTarget != null) {
                    leftNode.dependsOn(rightNode, -width);
                } else if (widget.mLeft.mTarget != null && widget.mRight.mTarget != null) {
                    if (widget.mDimensionRatio == 0) {
                        leftNode.setType(ResolutionNode.MATCH_CONNECTION);
                        rightNode.setType(ResolutionNode.MATCH_CONNECTION);
                        leftNode.setOpposite(rightNode, 0);
                        rightNode.setOpposite(leftNode, 0);
                    } else {
                        leftNode.setType(ResolutionNode.CENTER_CONNECTION);
                        rightNode.setType(ResolutionNode.CENTER_CONNECTION);
                        leftNode.setOpposite(rightNode, -width);
                        rightNode.setOpposite(leftNode, width);
                        widget.setWidth(width);
                    }
                }
            }
        }

        // ...then the vertical ones

        if (topNode.type != ResolutionNode.CHAIN_CONNECTION
                && bottomNode.type != ResolutionNode.CHAIN_CONNECTION
                /* && mBaseline.getResolutionNode().type == ResolutionNode.UNCONNECTED */) {
            if (widget.mListDimensionBehaviors[VERTICAL] == FIXED) {
                if (widget.mTop.mTarget == null && widget.mBottom.mTarget == null) {
                    topNode.setType(ResolutionNode.DIRECT_CONNECTION);
                    bottomNode.setType(ResolutionNode.DIRECT_CONNECTION);
                    bottomNode.dependsOn(topNode, widget.getHeight());
                    if (widget.mBaseline.mTarget != null) {
                        widget.mBaseline.getResolutionNode().setType(ResolutionNode.DIRECT_CONNECTION);
                        topNode.dependsOn(ResolutionNode.DIRECT_CONNECTION,
                                widget.mBaseline.getResolutionNode(), -widget.mBaselineDistance);
                    }
                } else if (widget.mTop.mTarget != null && widget.mBottom.mTarget == null) {
                    topNode.setType(ResolutionNode.DIRECT_CONNECTION);
                    bottomNode.setType(ResolutionNode.DIRECT_CONNECTION);
                    bottomNode.dependsOn(topNode, widget.getHeight());
                    if (widget.mBaselineDistance > 0) {
                        widget.mBaseline.getResolutionNode().dependsOn(ResolutionNode.DIRECT_CONNECTION, topNode, widget.mBaselineDistance);
                    }
                } else if (widget.mTop.mTarget == null && widget.mBottom.mTarget != null) {
                    topNode.setType(ResolutionNode.DIRECT_CONNECTION);
                    bottomNode.setType(ResolutionNode.DIRECT_CONNECTION);
                    topNode.dependsOn(bottomNode, -widget.getHeight());
                    if (widget.mBaselineDistance > 0) {
                        widget.mBaseline.getResolutionNode().dependsOn(ResolutionNode.DIRECT_CONNECTION, topNode, widget.mBaselineDistance);
                    }
                } else if (widget.mTop.mTarget != null && widget.mBottom.mTarget != null) {
                    topNode.setType(ResolutionNode.CENTER_CONNECTION);
                    bottomNode.setType(ResolutionNode.CENTER_CONNECTION);
                    topNode.setOpposite(bottomNode, -widget.getHeight());
                    bottomNode.setOpposite(topNode, widget.getHeight());
                    if (widget.mBaselineDistance > 0) {
                        widget.mBaseline.getResolutionNode().dependsOn(ResolutionNode.DIRECT_CONNECTION, topNode, widget.mBaselineDistance);
                    }
                }
            } else if (widget.mListDimensionBehaviors[VERTICAL] == MATCH_CONSTRAINT
                    && optimizableMatchConstraint(widget, VERTICAL)) {
                int height = widget.getHeight();
                if (widget.mDimensionRatio != 0) {
                    height = (int) (widget.getWidth() * widget.mDimensionRatio);
                }
                topNode.setType(ResolutionNode.DIRECT_CONNECTION);
                bottomNode.setType(ResolutionNode.DIRECT_CONNECTION);
                if (widget.mTop.mTarget == null && widget.mBottom.mTarget == null) {
                    bottomNode.dependsOn(topNode, height);
                } else if (widget.mTop.mTarget != null && widget.mBottom.mTarget == null) {
                    bottomNode.dependsOn(topNode, height);
                } else if (widget.mTop.mTarget == null && widget.mBottom.mTarget != null) {
                    topNode.dependsOn(bottomNode, -height);
                } else if (widget.mTop.mTarget != null && widget.mBottom.mTarget != null) {
                    if (widget.mDimensionRatio == 0) {
                        topNode.setType(ResolutionNode.MATCH_CONNECTION);
                        bottomNode.setType(ResolutionNode.MATCH_CONNECTION);
                        topNode.setOpposite(bottomNode, 0);
                        bottomNode.setOpposite(topNode, 0);
                    } else {
                        topNode.setType(ResolutionNode.CENTER_CONNECTION);
                        bottomNode.setType(ResolutionNode.CENTER_CONNECTION);
                        topNode.setOpposite(bottomNode, -height);
                        bottomNode.setOpposite(topNode, height);
                        widget.setHeight(height);
                        if (widget.mBaselineDistance > 0) {
                            widget.mBaseline.getResolutionNode().dependsOn(ResolutionNode.DIRECT_CONNECTION, topNode, widget.mBaselineDistance);
                        }
                    }
                }
            }
        }
    }

    /**
     * Try to apply the chain using the resolution nodes
     *
     * @param container
     * @param system
     * @param orientation
     * @param offset
     * @param first
     *
     * @return true if the chain has been optimized, false otherwise
     */
    static boolean applyChainOptimized(ConstraintWidgetContainer container, LinearSystem system,
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

        boolean isWrapContent = container.mListDimensionBehaviors[orientation] == DimensionBehaviour.WRAP_CONTENT;
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

        float totalSize = 0;
        float totalMargins = 0;
        int numVisibleWidgets = 0;

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
                numVisibleWidgets ++;
                if (orientation == HORIZONTAL) {
                    totalSize += widget.getWidth();
                } else {
                    totalSize += widget.getHeight();
                }
                if (widget != firstVisibleWidget) {
                    totalSize += widget.mListAnchors[offset].getMargin();
                }
                totalMargins += widget.mListAnchors[offset].getMargin();
                totalMargins += widget.mListAnchors[offset + 1].getMargin();
            }

            ConstraintAnchor begin = widget.mListAnchors[offset];

            // First, let's maintain a linked list of matched widgets for the chain
            widget.mListNextMatchConstraintsWidget[orientation] = null;
            if (widget.getVisibility() != ConstraintWidget.GONE
                    && widget.mListDimensionBehaviors[orientation] == MATCH_CONSTRAINT) {
                numMatchConstraints++;
                // only supports basic match_constraints
                if (orientation == HORIZONTAL) {
                    if (widget.mMatchConstraintDefaultWidth != ConstraintWidget.MATCH_CONSTRAINT_SPREAD) {
                        return false;
                    } else if (widget.mMatchConstraintMinWidth != 0 || widget.mMatchConstraintMaxWidth != 0) {
                        return false;
                    }
                } else {
                    if (widget.mMatchConstraintDefaultHeight != ConstraintWidget.MATCH_CONSTRAINT_SPREAD) {
                        return false;
                    } else if (widget.mMatchConstraintMinHeight != 0 || widget.mMatchConstraintMaxHeight != 0) {
                        return false;
                    }
                }
                totalWeights += widget.mWeight[orientation];
                if (firstMatchConstraintsWidget == null) {
                    firstMatchConstraintsWidget = widget;
                } else {
                    previousMatchConstraintsWidget.mListNextMatchConstraintsWidget[orientation] = widget;
                }
                previousMatchConstraintsWidget = widget;
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

        ResolutionNode firstNode = first.mListAnchors[offset].getResolutionNode();
        ResolutionNode lastNode = last.mListAnchors[offset + 1].getResolutionNode();

        if (firstNode.target == null || lastNode.target == null) {
            // dangling chain, let's bail for now
            return false;
        }

        // let's look at the endpoints
        if (firstNode.target.state != ResolutionNode.RESOLVED
                && lastNode.target.state != ResolutionNode.RESOLVED) {
            // No resolved endpoints, let's exit
            return false;
        }

        if (numMatchConstraints > 0 && numMatchConstraints != numVisibleWidgets) {
            // for now, only supports basic case
            return false;
        }

        float extraMargin = 0;
        if (isChainPacked || isChainSpread || isChainSpreadInside) {
            if (firstVisibleWidget != null) {
                extraMargin = firstVisibleWidget.mListAnchors[offset].getMargin();
            }
            if (lastVisibleWidget != null) {
                extraMargin += lastVisibleWidget.mListAnchors[offset + 1].getMargin();
            }
        }

        float firstOffset = firstNode.target.resolvedOffset;
        float lastOffset = lastNode.target.resolvedOffset;
        float distance = 0;
        if (firstOffset < lastOffset) {
            distance = lastOffset - firstOffset - totalSize;
        } else {
            distance = firstOffset - lastOffset - totalSize;
        }

        if (numMatchConstraints > 0 && numMatchConstraints == numVisibleWidgets) {
            if (widget.getParent() != null && widget.getParent().mListDimensionBehaviors[orientation] == DimensionBehaviour.WRAP_CONTENT) {
                return false;
            }
            distance += totalSize;
            distance -= totalMargins;
            widget = firstVisibleWidget;
            float position = firstOffset;
            if (isChainSpread) {
                distance -= (totalMargins - extraMargin);
            }
            if (isChainSpread) {
                position += widget.mListAnchors[offset + 1].getMargin();
                next = widget.mListNextVisibleWidget[orientation];
                if (next != null) {
                    position += next.mListAnchors[offset].getMargin();
                }
            }
            while (widget != null) {
                if (system.sMetrics != null) {
                    system.sMetrics.nonresolvedWidgets--;
                    system.sMetrics.resolvedWidgets++;
                    system.sMetrics.chainConnectionResolved++;
                }
                next = widget.mListNextVisibleWidget[orientation];
                if (next != null || widget == lastVisibleWidget) {
                    float dimension = distance / numMatchConstraints;
                    if (totalWeights > 0) {
                        dimension = widget.mWeight[orientation] * distance / totalWeights;
                    }
                    position += widget.mListAnchors[offset].getMargin();
                    widget.mListAnchors[offset].getResolutionNode().resolve(firstNode.resolvedTarget,
                            position);
                    widget.mListAnchors[offset + 1].getResolutionNode().resolve(firstNode.resolvedTarget,
                            position + dimension);
                    widget.mListAnchors[offset].getResolutionNode().addResolvedValue(system);
                    widget.mListAnchors[offset + 1].getResolutionNode().addResolvedValue(system);
                    position += dimension;
                    position += widget.mListAnchors[offset + 1].getMargin();
                }
                widget = next;
            }
            return true;
        }

        if (distance < totalSize) {
            return false;
        }

        if (isChainPacked) {
            distance -= extraMargin;
            // Now let's iterate on those widgets
            widget = firstVisibleWidget;
            distance = firstOffset + (distance * first.getHorizontalBiasPercent()); // start after the gap
            while (widget != null) {
                if (system.sMetrics != null) {
                    system.sMetrics.nonresolvedWidgets--;
                    system.sMetrics.resolvedWidgets++;
                    system.sMetrics.chainConnectionResolved++;
                }
                next = widget.mListNextVisibleWidget[orientation];
                if (next != null || widget == lastVisibleWidget) {
                    float dimension = 0;
                    if (orientation == HORIZONTAL) {
                        dimension = widget.getWidth();
                    } else {
                        dimension = widget.getHeight();
                    }
                    distance += widget.mListAnchors[offset].getMargin();
                    widget.mListAnchors[offset].getResolutionNode().resolve(firstNode.resolvedTarget,
                            distance);
                    widget.mListAnchors[offset + 1].getResolutionNode().resolve(firstNode.resolvedTarget,
                            distance + dimension);
                    widget.mListAnchors[offset].getResolutionNode().addResolvedValue(system);
                    widget.mListAnchors[offset + 1].getResolutionNode().addResolvedValue(system);
                    distance += dimension;
                    distance += widget.mListAnchors[offset + 1].getMargin();
                }
                widget = next;
            }
        } else if (isChainSpread || isChainSpreadInside) {
            if (isChainSpread) {
                distance -= extraMargin;
            } else if (isChainSpreadInside) {
                distance -= extraMargin;
            }
            widget = firstVisibleWidget;
            float gap = distance / (float) (numVisibleWidgets + 1);
            if (isChainSpreadInside) {
                if (numVisibleWidgets > 1) {
                    gap = distance / (float) (numVisibleWidgets - 1);
                } else {
                    gap = distance / 2f; // center
                }
            }
            distance = firstOffset + gap; // start after the gap
            if (isChainSpreadInside && numVisibleWidgets > 1) {
                distance = firstOffset + firstVisibleWidget.mListAnchors[offset].getMargin();
            }
            if (isChainSpread) {
                if (firstVisibleWidget != null) {
                    distance += firstVisibleWidget.mListAnchors[offset].getMargin();
                }
            }
            while (widget != null) {
                if (system.sMetrics != null) {
                    system.sMetrics.nonresolvedWidgets--;
                    system.sMetrics.resolvedWidgets++;
                    system.sMetrics.chainConnectionResolved++;
                }
                next = widget.mListNextVisibleWidget[orientation];
                if (next != null || widget == lastVisibleWidget) {
                    float dimension = 0;
                    if (orientation == HORIZONTAL) {
                        dimension = widget.getWidth();
                    } else {
                        dimension = widget.getHeight();
                    }
                    widget.mListAnchors[offset].getResolutionNode().resolve(firstNode.resolvedTarget,
                            distance);
                    widget.mListAnchors[offset + 1].getResolutionNode().resolve(firstNode.resolvedTarget,
                            distance + dimension);
                    widget.mListAnchors[offset].getResolutionNode().addResolvedValue(system);
                    widget.mListAnchors[offset + 1].getResolutionNode().addResolvedValue(system);
                    distance += dimension + gap;
                }
                widget = next;
            }
        }

        return true; // optimized!
    }
}
