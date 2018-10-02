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

import static android.support.constraint.solver.widgets.ConstraintWidget.*;
import static android.support.constraint.solver.widgets.ConstraintWidget.DimensionBehaviour.FIXED;
import static android.support.constraint.solver.widgets.ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT;

/**
 * Implements direct resolution without using the solver
 */
public class Optimizer {

    // Optimization levels (mask)
    public static final int OPTIMIZATION_NONE  = 0;
    public static final int OPTIMIZATION_DIRECT = 1;
    public static final int OPTIMIZATION_BARRIER = 1 << 1;
    public static final int OPTIMIZATION_CHAIN = 1 << 2;
    public static final int OPTIMIZATION_DIMENSIONS = 1 << 3;
    public static final int OPTIMIZATION_RATIO = 1 << 4;
    public static final int OPTIMIZATION_GROUPS = 1 << 5;
    public static final int OPTIMIZATION_STANDARD = OPTIMIZATION_DIRECT
            | OPTIMIZATION_BARRIER
            | OPTIMIZATION_CHAIN
            /* | OPTIMIZATION_DIMENSIONS */
            ;

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
    static void analyze(int optimisationLevel, ConstraintWidget widget) {

        // Let's update the graph from the nodes!
        // This will only apply if the nodes are not part of a chain.
        // It will set up direct connections depending on the intrinsic size (fixed or match_constraint)
        // as well as identifying centered connections and match connections.

        widget.updateResolutionNodes();

        ResolutionAnchor leftNode = widget.mLeft.getResolutionNode();
        ResolutionAnchor topNode = widget.mTop.getResolutionNode();
        ResolutionAnchor rightNode = widget.mRight.getResolutionNode();
        ResolutionAnchor bottomNode = widget.mBottom.getResolutionNode();

        boolean optimiseDimensions = (optimisationLevel & OPTIMIZATION_DIMENSIONS) == OPTIMIZATION_DIMENSIONS;

        // First the horizontal nodes...

        boolean isOptimizableHorizontalMatch = widget.mListDimensionBehaviors[HORIZONTAL] == MATCH_CONSTRAINT
            && optimizableMatchConstraint(widget, HORIZONTAL);

        if (leftNode.type != ResolutionAnchor.CHAIN_CONNECTION
                && rightNode.type != ResolutionAnchor.CHAIN_CONNECTION) {
            if (widget.mListDimensionBehaviors[HORIZONTAL] == FIXED
                || (isOptimizableHorizontalMatch && widget.getVisibility() == ConstraintWidget.GONE)) {
                if (widget.mLeft.mTarget == null && widget.mRight.mTarget == null) {
                    leftNode.setType(ResolutionAnchor.DIRECT_CONNECTION);
                    rightNode.setType(ResolutionAnchor.DIRECT_CONNECTION);
                    if (optimiseDimensions) {
                        rightNode.dependsOn(leftNode, 1, widget.getResolutionWidth());
                    } else {
                        rightNode.dependsOn(leftNode, widget.getWidth());
                    }
                } else if (widget.mLeft.mTarget != null && widget.mRight.mTarget == null) {
                    leftNode.setType(ResolutionAnchor.DIRECT_CONNECTION);
                    rightNode.setType(ResolutionAnchor.DIRECT_CONNECTION);
                    if (optimiseDimensions) {
                        rightNode.dependsOn(leftNode, 1, widget.getResolutionWidth());
                    } else {
                        rightNode.dependsOn(leftNode, widget.getWidth());
                    }
                } else if (widget.mLeft.mTarget == null && widget.mRight.mTarget != null) {
                    leftNode.setType(ResolutionAnchor.DIRECT_CONNECTION);
                    rightNode.setType(ResolutionAnchor.DIRECT_CONNECTION);
                    leftNode.dependsOn(rightNode, -widget.getWidth());
                    if (optimiseDimensions) {
                        leftNode.dependsOn(rightNode, -1, widget.getResolutionWidth());
                    } else {
                        leftNode.dependsOn(rightNode, -widget.getWidth());
                    }
                } else if (widget.mLeft.mTarget != null && widget.mRight.mTarget != null) {
                    leftNode.setType(ResolutionAnchor.CENTER_CONNECTION);
                    rightNode.setType(ResolutionAnchor.CENTER_CONNECTION);
                    if (optimiseDimensions) {
                        widget.getResolutionWidth().addDependent(leftNode);
                        widget.getResolutionWidth().addDependent(rightNode);
                        leftNode.setOpposite(rightNode, -1, widget.getResolutionWidth());
                        rightNode.setOpposite(leftNode, 1, widget.getResolutionWidth());
                    } else {
                        leftNode.setOpposite(rightNode, -widget.getWidth());
                        rightNode.setOpposite(leftNode, widget.getWidth());
                    }
                }
            } else if (isOptimizableHorizontalMatch) {
                int width = widget.getWidth();
                // TODO: ratio won't work with optimiseDimensions as it is
                // ...but ratio won't work period for now as optimizableMatchConstraint will return false
                // if (widget.mDimensionRatio != 0) {
                //     width = (int) (widget.getHeight() * widget.mDimensionRatio);
                // }
                leftNode.setType(ResolutionAnchor.DIRECT_CONNECTION);
                rightNode.setType(ResolutionAnchor.DIRECT_CONNECTION);
                if (widget.mLeft.mTarget == null && widget.mRight.mTarget == null) {
                    if (optimiseDimensions) {
                        rightNode.dependsOn(leftNode, 1, widget.getResolutionWidth());
                    } else {
                        rightNode.dependsOn(leftNode, width);
                    }
                } else if (widget.mLeft.mTarget != null && widget.mRight.mTarget == null) {
                    if (optimiseDimensions) {
                        rightNode.dependsOn(leftNode, 1, widget.getResolutionWidth());
                    } else {
                        rightNode.dependsOn(leftNode, width);
                    }
                } else if (widget.mLeft.mTarget == null && widget.mRight.mTarget != null) {
                    if (optimiseDimensions) {
                        leftNode.dependsOn(rightNode, -1, widget.getResolutionWidth());
                    } else {
                        leftNode.dependsOn(rightNode, -width);
                    }
                } else if (widget.mLeft.mTarget != null && widget.mRight.mTarget != null) {
                    if (optimiseDimensions) {
                        widget.getResolutionWidth().addDependent(leftNode);
                        widget.getResolutionWidth().addDependent(rightNode);
                    }
                    if (widget.mDimensionRatio == 0) {
                        leftNode.setType(ResolutionAnchor.MATCH_CONNECTION);
                        rightNode.setType(ResolutionAnchor.MATCH_CONNECTION);
                        leftNode.setOpposite(rightNode, 0);
                        rightNode.setOpposite(leftNode, 0);
                    } else {
                        // TODO -- fix ratio. For now this won't work.
                        leftNode.setType(ResolutionAnchor.CENTER_CONNECTION);
                        rightNode.setType(ResolutionAnchor.CENTER_CONNECTION);
                        leftNode.setOpposite(rightNode, -width);
                        rightNode.setOpposite(leftNode, width);
                        widget.setWidth(width);
                    }
                }
            }
        }

        // ...then the vertical ones

      boolean isOptimizableVerticalMatch = widget.mListDimensionBehaviors[VERTICAL] == MATCH_CONSTRAINT
          && optimizableMatchConstraint(widget, VERTICAL);

      if (topNode.type != ResolutionAnchor.CHAIN_CONNECTION
                && bottomNode.type != ResolutionAnchor.CHAIN_CONNECTION
                /* && mBaseline.getResolutionNode().type == ResolutionAnchor.UNCONNECTED */) {
            if (widget.mListDimensionBehaviors[VERTICAL] == FIXED
                || (isOptimizableVerticalMatch && widget.getVisibility() == ConstraintWidget.GONE)) {
                if (widget.mTop.mTarget == null && widget.mBottom.mTarget == null) {
                    topNode.setType(ResolutionAnchor.DIRECT_CONNECTION);
                    bottomNode.setType(ResolutionAnchor.DIRECT_CONNECTION);
                    if (optimiseDimensions) {
                        bottomNode.dependsOn(topNode, 1, widget.getResolutionHeight());
                    } else {
                        bottomNode.dependsOn(topNode, widget.getHeight());
                    }
                    if (widget.mBaseline.mTarget != null) {
                        widget.mBaseline.getResolutionNode().setType(ResolutionAnchor.DIRECT_CONNECTION);
                        topNode.dependsOn(ResolutionAnchor.DIRECT_CONNECTION,
                                widget.mBaseline.getResolutionNode(), -widget.mBaselineDistance);
                    }
                } else if (widget.mTop.mTarget != null && widget.mBottom.mTarget == null) {
                    topNode.setType(ResolutionAnchor.DIRECT_CONNECTION);
                    bottomNode.setType(ResolutionAnchor.DIRECT_CONNECTION);
                    if (optimiseDimensions) {
                        bottomNode.dependsOn(topNode, 1, widget.getResolutionHeight());
                    } else {
                        bottomNode.dependsOn(topNode, widget.getHeight());
                    }
                    if (widget.mBaselineDistance > 0) {
                        widget.mBaseline.getResolutionNode().dependsOn(ResolutionAnchor.DIRECT_CONNECTION, topNode, widget.mBaselineDistance);
                    }
                } else if (widget.mTop.mTarget == null && widget.mBottom.mTarget != null) {
                    topNode.setType(ResolutionAnchor.DIRECT_CONNECTION);
                    bottomNode.setType(ResolutionAnchor.DIRECT_CONNECTION);
                    if (optimiseDimensions) {
                        topNode.dependsOn(bottomNode, -1, widget.getResolutionHeight());
                    } else {
                        topNode.dependsOn(bottomNode, -widget.getHeight());
                    }
                    if (widget.mBaselineDistance > 0) {
                        widget.mBaseline.getResolutionNode().dependsOn(ResolutionAnchor.DIRECT_CONNECTION, topNode, widget.mBaselineDistance);
                    }
                } else if (widget.mTop.mTarget != null && widget.mBottom.mTarget != null) {
                    topNode.setType(ResolutionAnchor.CENTER_CONNECTION);
                    bottomNode.setType(ResolutionAnchor.CENTER_CONNECTION);
                    if (optimiseDimensions) {
                        topNode.setOpposite(bottomNode, -1, widget.getResolutionHeight());
                        bottomNode.setOpposite(topNode, 1, widget.getResolutionHeight());
                        widget.getResolutionHeight().addDependent(topNode);
                        widget.getResolutionWidth().addDependent(bottomNode);
                    } else {
                        topNode.setOpposite(bottomNode, -widget.getHeight());
                        bottomNode.setOpposite(topNode, widget.getHeight());
                    }
                    if (widget.mBaselineDistance > 0) {
                        widget.mBaseline.getResolutionNode().dependsOn(ResolutionAnchor.DIRECT_CONNECTION, topNode, widget.mBaselineDistance);
                    }
                }
            } else if (isOptimizableVerticalMatch) {
                int height = widget.getHeight();
                // TODO: fix ratio (right it won't work, optimizableMatchConstraint will return false
                // if (widget.mDimensionRatio != 0) {
                //     height = (int) (widget.getWidth() * widget.mDimensionRatio);
                // }
                topNode.setType(ResolutionAnchor.DIRECT_CONNECTION);
                bottomNode.setType(ResolutionAnchor.DIRECT_CONNECTION);
                if (widget.mTop.mTarget == null && widget.mBottom.mTarget == null) {
                    if (optimiseDimensions) {
                        bottomNode.dependsOn(topNode, 1, widget.getResolutionHeight());
                    } else {
                        bottomNode.dependsOn(topNode, height);
                    }
                } else if (widget.mTop.mTarget != null && widget.mBottom.mTarget == null) {
                    if (optimiseDimensions) {
                        bottomNode.dependsOn(topNode, 1, widget.getResolutionHeight());
                    } else {
                        bottomNode.dependsOn(topNode, height);
                    }
                } else if (widget.mTop.mTarget == null && widget.mBottom.mTarget != null) {
                    if (optimiseDimensions) {
                        topNode.dependsOn(bottomNode, -1, widget.getResolutionHeight());
                    } else {
                        topNode.dependsOn(bottomNode, -height);
                    }
                } else if (widget.mTop.mTarget != null && widget.mBottom.mTarget != null) {
                    if (optimiseDimensions) {
                        widget.getResolutionHeight().addDependent(topNode);
                        widget.getResolutionWidth().addDependent(bottomNode);
                    }
                    if (widget.mDimensionRatio == 0) {
                        topNode.setType(ResolutionAnchor.MATCH_CONNECTION);
                        bottomNode.setType(ResolutionAnchor.MATCH_CONNECTION);
                        topNode.setOpposite(bottomNode, 0);
                        bottomNode.setOpposite(topNode, 0);
                    } else {
                        topNode.setType(ResolutionAnchor.CENTER_CONNECTION);
                        bottomNode.setType(ResolutionAnchor.CENTER_CONNECTION);
                        topNode.setOpposite(bottomNode, -height);
                        bottomNode.setOpposite(topNode, height);
                        widget.setHeight(height);
                        if (widget.mBaselineDistance > 0) {
                            widget.mBaseline.getResolutionNode().dependsOn(ResolutionAnchor.DIRECT_CONNECTION, topNode, widget.mBaselineDistance);
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
     * @param chainHead
     *
     * @return true if the chain has been optimized, false otherwise
     */
    static boolean applyChainOptimized(ConstraintWidgetContainer container, LinearSystem system,
                                      int orientation, int offset, ChainHead chainHead) {

        ConstraintWidget first = chainHead.mFirst;
        ConstraintWidget last = chainHead.mLast;
        ConstraintWidget firstVisibleWidget = chainHead.mFirstVisibleWidget;
        ConstraintWidget lastVisibleWidget = chainHead.mLastVisibleWidget;
        ConstraintWidget head = chainHead.mHead;

        ConstraintWidget widget = first;
        ConstraintWidget next = null;
        boolean done = false;

        int numMatchConstraints = 0;
        float totalWeights = chainHead.mTotalWeight;
        ConstraintWidget firstMatchConstraintsWidget = chainHead.mFirstMatchConstraintWidget;
        ConstraintWidget previousMatchConstraintsWidget = chainHead.mLastMatchConstraintWidget;

        boolean isWrapContent = container.mListDimensionBehaviors[orientation] == DimensionBehaviour.WRAP_CONTENT;
        boolean isChainSpread = false;
        boolean isChainSpreadInside = false;
        boolean isChainPacked = false;

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
        // - build a linked list of matched constraints widgets

        float totalSize = 0;
        float totalMargins = 0;
        int numVisibleWidgets = 0;

        while (!done) {
            // Measure visible widgets and add margins.
            if (widget.getVisibility() != ConstraintWidget.GONE) {
                numVisibleWidgets ++;
                if (orientation == HORIZONTAL) {
                    totalSize += widget.getWidth();
                } else {
                    totalSize += widget.getHeight();
                }
                if (widget != firstVisibleWidget) {
                    totalSize += widget.mListAnchors[offset].getMargin();
                }
                if (widget != lastVisibleWidget) {
                    totalSize += widget.mListAnchors[offset + 1].getMargin();
                }
                totalMargins += widget.mListAnchors[offset].getMargin();
                totalMargins += widget.mListAnchors[offset + 1].getMargin();
            }

            ConstraintAnchor begin = widget.mListAnchors[offset];

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
                if (widget.mDimensionRatio != 0.0f) {
                    return false;
                }
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

        ResolutionAnchor firstNode = first.mListAnchors[offset].getResolutionNode();
        ResolutionAnchor lastNode = last.mListAnchors[offset + 1].getResolutionNode();

        if (firstNode.target == null || lastNode.target == null) {
            // dangling chain, let's bail for now
            return false;
        }

        // let's look at the endpoints
        if (firstNode.target.state != ResolutionAnchor.RESOLVED
                || lastNode.target.state != ResolutionAnchor.RESOLVED) {
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
            widget = first;
            float position = firstOffset;
            while (widget != null) {
                if (system.sMetrics != null) {
                    system.sMetrics.nonresolvedWidgets--;
                    system.sMetrics.resolvedWidgets++;
                    system.sMetrics.chainConnectionResolved++;
                }
                next = widget.mNextChainWidget[orientation];
                if (next != null || widget == last) {
                    float dimension = distance / numMatchConstraints;
                    if (totalWeights > 0) {
                        if (widget.mWeight[orientation] == UNKNOWN) {
                            dimension = 0;
                        } else {
                            dimension = widget.mWeight[orientation] * distance / totalWeights;
                        }
                    }
                    if (widget.getVisibility() == GONE) {
                        dimension = 0;
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

        // If there is not enough space, the chain has to behave as a packed chain.
        if (distance < 0) {
            isChainSpread = false;
            isChainSpreadInside = false;
            isChainPacked = true;
        }

        if (isChainPacked) {
            distance -= extraMargin;
            // Now let's iterate on those widgets
            widget = first;
            distance = firstOffset + (distance * first.getBiasPercent(orientation)); // start after the gap
            while (widget != null) {
                if (system.sMetrics != null) {
                    system.sMetrics.nonresolvedWidgets--;
                    system.sMetrics.resolvedWidgets++;
                    system.sMetrics.chainConnectionResolved++;
                }
                next = widget.mNextChainWidget[orientation];
                if (next != null || widget == last) {
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
            widget = first;
            float gap = distance / (float) (numVisibleWidgets + 1);
            if (isChainSpreadInside) {
                if (numVisibleWidgets > 1) {
                    gap = distance / (float) (numVisibleWidgets - 1);
                } else {
                    gap = distance / 2f; // center
                }
            }
            distance = firstOffset;
            if (first.getVisibility() != GONE) {
                distance += gap; // start after the gap
            }
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
                next = widget.mNextChainWidget[orientation];
                if (next != null || widget == last) {
                    float dimension = 0;
                    if (orientation == HORIZONTAL) {
                        dimension = widget.getWidth();
                    } else {
                        dimension = widget.getHeight();
                    }
                    if (widget != firstVisibleWidget) {
                        distance += widget.mListAnchors[offset].getMargin();
                    }
                    widget.mListAnchors[offset].getResolutionNode().resolve(firstNode.resolvedTarget,
                            distance);
                    widget.mListAnchors[offset + 1].getResolutionNode().resolve(firstNode.resolvedTarget,
                            distance + dimension);
                    widget.mListAnchors[offset].getResolutionNode().addResolvedValue(system);
                    widget.mListAnchors[offset + 1].getResolutionNode().addResolvedValue(system);
                    distance += dimension + widget.mListAnchors[offset + 1].getMargin();
                    if (next != null && next.getVisibility() != GONE) {
                        distance += gap;
                    }
                }
                widget = next;
            }
        }

        return true; // optimized!
    }

    //TODO: Might want to use ResolutionAnchor::resolve(target, offset).
    /**
     * Set a {@link ConstraintWidget} optimized position and dimension in an specific orientation.
     *
     * @param widget         Widget to be optimized.
     * @param orientation    Orientation to set optimization (HORIZONTAL{0}/VERTICAL{1}).
     * @param resolvedOffset The resolved offset of the widget with respect to the root.
     */
    static void setOptimizedWidget(ConstraintWidget widget, int orientation, int resolvedOffset) {
        final int startOffset = orientation * 2;
        final int endOffset = startOffset + 1;
        // Left/top of widget.
        widget.mListAnchors[startOffset].getResolutionNode().resolvedTarget =
                widget.getParent().mLeft.getResolutionNode();
        widget.mListAnchors[startOffset].getResolutionNode().resolvedOffset =
                resolvedOffset;
        widget.mListAnchors[startOffset].getResolutionNode().state = ResolutionNode.RESOLVED;
        // Right/bottom of widget.
        widget.mListAnchors[endOffset].getResolutionNode().resolvedTarget =
                widget.mListAnchors[startOffset].getResolutionNode();
        widget.mListAnchors[endOffset].getResolutionNode().resolvedOffset =
                widget.getLength(orientation);
        widget.mListAnchors[endOffset].getResolutionNode().state = ResolutionNode.RESOLVED;
    }
}
