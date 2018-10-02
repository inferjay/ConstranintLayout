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

import android.support.constraint.solver.widgets.ConstraintWidget.DimensionBehaviour;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to do widget constraints analysis.
 * <p>
 * Identify groups of widgets independent from each other.
 * TODO: Identify Chains here instead.
 */
public class Analyzer {

    private Analyzer() {
    }

    /**
     * Find groups of constrained widgets.
     * <p>
     * Used to simplify the resolution process to layout the widgets when using optimizations.
     * Wrap_content layouts require measuring the final size, groups are identified when
     * the layout can be measured.
     *
     * @param layoutWidget Layout to analyze.
     */
    public static void determineGroups(ConstraintWidgetContainer layoutWidget) {
        if ((layoutWidget.getOptimizationLevel() & Optimizer.OPTIMIZATION_GROUPS) != Optimizer.OPTIMIZATION_GROUPS) {
            singleGroup(layoutWidget);
            return;
        }
        layoutWidget.mSkipSolver = true;
        layoutWidget.mGroupsWrapOptimized = false;
        layoutWidget.mHorizontalWrapOptimized = false;
        layoutWidget.mVerticalWrapOptimized = false;
        final List<ConstraintWidget> widgets = layoutWidget.mChildren;
        final List<ConstraintWidgetGroup> widgetGroups = layoutWidget.mWidgetGroups;
        boolean horizontalWrapContent = layoutWidget.getHorizontalDimensionBehaviour() == DimensionBehaviour.WRAP_CONTENT;
        boolean verticalWrapContent = layoutWidget.getVerticalDimensionBehaviour() == DimensionBehaviour.WRAP_CONTENT;
        boolean hasWrapContent = horizontalWrapContent || verticalWrapContent;
        widgetGroups.clear();

        for (ConstraintWidget widget : widgets) {
            widget.mBelongingGroup = null;
            widget.mGroupsToSolver = false;
            widget.resetResolutionNodes();
        }
        for (ConstraintWidget widget : widgets) {
            if (widget.mBelongingGroup == null) {
                if (!determineGroups(widget, widgetGroups, hasWrapContent)) {
                    singleGroup(layoutWidget);
                    layoutWidget.mSkipSolver = false;
                    return;
                }
            }
        }
        int measuredWidth = 0;
        int measuredHeight = 0;
        // Resolve solvable widgets.
        for (ConstraintWidgetGroup group : widgetGroups) {
            measuredWidth = Math.max(measuredWidth,
                    getMaxDimension(group, ConstraintWidget.HORIZONTAL));
            measuredHeight = Math.max(measuredHeight,
                    getMaxDimension(group, ConstraintWidget.VERTICAL));
        }
        // Change container to fixed and set resolved dimensions.
        if (horizontalWrapContent) {
            layoutWidget.setHorizontalDimensionBehaviour(DimensionBehaviour.FIXED);
            layoutWidget.setWidth(measuredWidth);
            layoutWidget.mGroupsWrapOptimized = true;
            layoutWidget.mHorizontalWrapOptimized = true;
            layoutWidget.mWrapFixedWidth = measuredWidth;
        }
        if (verticalWrapContent) {
            layoutWidget.setVerticalDimensionBehaviour(DimensionBehaviour.FIXED);
            layoutWidget.setHeight(measuredHeight);
            layoutWidget.mGroupsWrapOptimized = true;
            layoutWidget.mVerticalWrapOptimized = true;
            layoutWidget.mWrapFixedHeight = measuredHeight;
        }
        setPosition(widgetGroups, ConstraintWidget.HORIZONTAL, layoutWidget.getWidth());
        setPosition(widgetGroups, ConstraintWidget.VERTICAL, layoutWidget.getHeight());
    }

    /**
     * @param widget         Widget being traversed.
     * @param widgetGroups   Starting list to contain the widgets in this group.
     * @param hasWrapContent Indicating if any dimension of the parent is in wrap_content.
     * @return False if the group can't be optimized in any way.
     */
    private static boolean determineGroups(ConstraintWidget widget,
                                           List<ConstraintWidgetGroup> widgetGroups, boolean hasWrapContent) {
        ConstraintWidgetGroup traverseList = new ConstraintWidgetGroup(new ArrayList<ConstraintWidget>(), true);
        widgetGroups.add(traverseList);
        return traverse(widget, traverseList, widgetGroups, hasWrapContent);
    }

    /**
     * Recursive function to traverse constrained widgets.
     * The objective is to maintain in a single list all the widgets that can be reached through
     * their constraints except for their parent.
     *
     * @param widget         Widget being traversed.
     * @param upperGroup     List being passed down, originally by {@link #determineGroups(ConstraintWidget, List, boolean)}.
     * @param widgetGroups   List of widget groups identified.
     * @param hasWrapContent Indicates if the layout has any dimension as wrap_content.
     * @return If the group analysis failed or can't be done.
     */
    private static boolean traverse(ConstraintWidget widget, ConstraintWidgetGroup upperGroup,
                                    List<ConstraintWidgetGroup> widgetGroups, boolean hasWrapContent) {
        if (widget == null) {
            return true;
        }
        widget.mOptimizerMeasured = false;
        ConstraintWidgetContainer layoutWidget = (ConstraintWidgetContainer) widget.getParent();
        if (widget.mBelongingGroup == null) {
            // If it hasn't been assigned to a group.
            widget.mOptimizerMeasurable = true;
            upperGroup.mConstrainedGroup.add(widget);
            widget.mBelongingGroup = upperGroup;
            // Determine if group is measurable.
            if (widget.mLeft.mTarget == null
                    && widget.mRight.mTarget == null
                    && widget.mTop.mTarget == null
                    && widget.mBottom.mTarget == null
                    && widget.mBaseline.mTarget == null
                    && widget.mCenter.mTarget == null) {
                invalidate(layoutWidget, widget, upperGroup);
                if (hasWrapContent) {
                    return false;
                }
            }
            // Check if it has vertical bias.
            if (widget.mTop.mTarget != null && widget.mBottom.mTarget != null) {
                // Allow if it has no wrap content in that dimension an constrained to the parent.
                boolean wrap = layoutWidget.getVerticalDimensionBehaviour() == DimensionBehaviour.WRAP_CONTENT;
                if (hasWrapContent) {
                    invalidate(layoutWidget, widget, upperGroup);
                    return false;
                } else if (!(widget.mTop.mTarget.mOwner == widget.getParent()
                        && widget.mBottom.mTarget.mOwner == widget.getParent())) {
                    invalidate(layoutWidget, widget, upperGroup);
                }
            }
            // Check if it has horizontal bias.
            if (widget.mLeft.mTarget != null && widget.mRight.mTarget != null) {
                // Allow if it has no wrap content in that dimension an constrained to the parent.
                boolean wrap = layoutWidget.getHorizontalDimensionBehaviour() == DimensionBehaviour.WRAP_CONTENT;
                if (hasWrapContent) {
                    invalidate(layoutWidget, widget, upperGroup);
                    return false;
                } else if (!(widget.mLeft.mTarget.mOwner == widget.getParent()
                        && widget.mRight.mTarget.mOwner == widget.getParent())) {
                    invalidate(layoutWidget, widget, upperGroup);
                }
            }
            if ((widget.getHorizontalDimensionBehaviour() == DimensionBehaviour.MATCH_CONSTRAINT
                    ^ widget.getVerticalDimensionBehaviour() == DimensionBehaviour.MATCH_CONSTRAINT)
                    && widget.mDimensionRatio != 0.0f) {
                // Calculate dimension.
                resolveDimensionRatio(widget);
            } else if (!(widget.getHorizontalDimensionBehaviour() != DimensionBehaviour.MATCH_CONSTRAINT
                    && widget.getVerticalDimensionBehaviour() != DimensionBehaviour.MATCH_CONSTRAINT)) {
                invalidate(layoutWidget, widget, upperGroup);
                if (hasWrapContent) {
                    return false;
                }
            }
            // Is Horizontal start
            if (((widget.mLeft.mTarget == null && widget.mRight.mTarget == null)
                    || (widget.mLeft.mTarget != null && widget.mLeft.mTarget.mOwner == widget.mParent && widget.mRight.mTarget == null)
                    || (widget.mRight.mTarget != null && widget.mRight.mTarget.mOwner == widget.mParent && widget.mLeft.mTarget == null)
                    || (widget.mLeft.mTarget != null && widget.mLeft.mTarget.mOwner == widget.mParent
                    && widget.mRight.mTarget != null && widget.mRight.mTarget.mOwner == widget.mParent))
                    && (widget.mCenter.mTarget == null)) {
                if (!(widget instanceof Guideline) && !(widget instanceof Helper)) {
                    upperGroup.mStartHorizontalWidgets.add(widget);
                }

            }
            // Is Vertical start
            if (((widget.mTop.mTarget == null && widget.mBottom.mTarget == null)
                    || (widget.mTop.mTarget != null && widget.mTop.mTarget.mOwner == widget.mParent && widget.mBottom.mTarget == null)
                    || (widget.mBottom.mTarget != null && widget.mBottom.mTarget.mOwner == widget.mParent && widget.mTop.mTarget == null)
                    || (widget.mTop.mTarget != null && widget.mTop.mTarget.mOwner == widget.mParent
                    && widget.mBottom.mTarget != null && widget.mBottom.mTarget.mOwner == widget.mParent))
                    && (widget.mCenter.mTarget == null && widget.mBaseline.mTarget == null)) {
                if (!(widget instanceof Guideline) && !(widget instanceof Helper)) {
                    upperGroup.mStartVerticalWidgets.add(widget);
                }
            }
        } else {
            // If it has, join the list and re-assign. Remove joint list from mWidgetGroups (if its a different list)
            if (widget.mBelongingGroup != upperGroup) {
                upperGroup.mConstrainedGroup.addAll(widget.mBelongingGroup.mConstrainedGroup);
                upperGroup.mStartHorizontalWidgets.addAll(widget.mBelongingGroup.mStartHorizontalWidgets);
                upperGroup.mStartVerticalWidgets.addAll(widget.mBelongingGroup.mStartVerticalWidgets);
                if (widget.mBelongingGroup.mSkipSolver == false) {
                    upperGroup.mSkipSolver = false;
                }
                widgetGroups.remove(widget.mBelongingGroup);
                for (ConstraintWidget auxWidget : widget.mBelongingGroup.mConstrainedGroup) {
                    auxWidget.mBelongingGroup = upperGroup;
                }
            }
            return true;
        }
        // Proceed to traverse widgets, start with HelperWidgets since they contain multiple widgets.
        if (widget instanceof Helper) {
            invalidate(layoutWidget, widget, upperGroup);
            if (hasWrapContent) {
                return false;
            }
            final Helper hWidget = (Helper) widget;
            for (int widgetsCount = 0; widgetsCount < hWidget.mWidgetsCount; widgetsCount++) {
                if (!traverse(hWidget.mWidgets[widgetsCount], upperGroup, widgetGroups, hasWrapContent)) {
                    return false;
                }
            }
        }
        // We traverse every anchor, for wrap_content we ignore center (circular constraints).
        final int anchorsSize = widget.mListAnchors.length;
        for (int i = 0; i < anchorsSize; i++) {
            final ConstraintAnchor anchor = widget.mListAnchors[i];
            if (anchor.mTarget != null && anchor.mTarget.mOwner != widget.getParent()) {
                if (anchor.mType == ConstraintAnchor.Type.CENTER) {
                    invalidate(layoutWidget, widget, upperGroup);
                    if (hasWrapContent) {
                        return false;
                    }
                } else {
                    setConnection(anchor);
                }
                if (!traverse(anchor.mTarget.mOwner, upperGroup, widgetGroups, hasWrapContent)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void invalidate(ConstraintWidgetContainer layoutWidget, ConstraintWidget widget, ConstraintWidgetGroup group) {
        group.mSkipSolver = false;
        layoutWidget.mSkipSolver = false;
        widget.mOptimizerMeasurable = false;
    }

    /**
     * Obtain the max length of a {@link ConstraintWidgetGroup} on a specific orientation.
     * Length is saved on the group for future use as well.
     *
     * @param group       Group of widgets being measured.
     * @param orientation Orientation being measured.
     * @return Max dimension in the group.
     */
    private static int getMaxDimension(ConstraintWidgetGroup group, int orientation) {
        int dimension = 0;
        int offset = orientation * 2;
        List<ConstraintWidget> startWidgets = group.getStartWidgets(orientation);
        final int size = startWidgets.size();
        for (int i = 0; i < size; i++) {
            ConstraintWidget widget = startWidgets.get(i);
            boolean topLeftFlow = widget.mListAnchors[offset + 1].mTarget == null
                    || (widget.mListAnchors[offset].mTarget != null
                    && widget.mListAnchors[offset + 1].mTarget != null);
            dimension = Math.max(dimension, getMaxDimensionTraversal(widget, orientation, topLeftFlow, 0));
        }

        group.mGroupDimensions[orientation] = dimension;
        return dimension;
    }

    /**
     * Traverse from a widget at the start of a tree (a widget constrained to any side of their parent),
     * find the maximum length of the tree.
     * Avoids cases when a widget's dimension shouldn't be considered.
     *
     * @param widget      Widget being traversed.
     * @param orientation Dimension being measured (HORIZONTAL/VERTICAL).
     * @param topLeftFlow Indicates if the tree starts at the top or left of the container.
     * @param depth       How far the widget is from the start of the tree.
     * @return Max dimension from the widget being traversed.
     */
    private static int getMaxDimensionTraversal(ConstraintWidget widget, int orientation, boolean topLeftFlow, int depth) {
        // Start and end offset used to point to the correct anchors according to the flow
        // of the widget at the start of the tree.
        if (!widget.mOptimizerMeasurable) {
            return 0;
        }
        int startOffset;
        int endOffset;
        int dimension = 0;
        int dimensionPre = 0;
        int dimensionPost = 0;
        final int flow;
        final int baselinePreDistance;
        final int baselinePostDistance;
        // If it has baseline, the dimensions change, despite maintaining the flow.
        final boolean hasBaseline = widget.mBaseline.mTarget != null && orientation == ConstraintWidget.VERTICAL;

        if (topLeftFlow) {
            baselinePreDistance = widget.getBaselineDistance();
            baselinePostDistance = widget.getHeight() - widget.getBaselineDistance();
            startOffset = orientation * 2;
            endOffset = startOffset + 1;
        } else {
            baselinePreDistance = widget.getHeight() - widget.getBaselineDistance();
            baselinePostDistance = widget.getBaselineDistance();
            endOffset = orientation * 2;
            startOffset = endOffset + 1;
        }

        // Define the correct flow of direction. left -> right or left <- right.
        // If the flow is going opposite from the startWidget, lengths and margin subtract.
        if (widget.mListAnchors[endOffset].mTarget != null && widget.mListAnchors[startOffset].mTarget == null) {
            flow = -1;
            int aux = startOffset;
            startOffset = endOffset;
            endOffset = aux;
        } else {
            flow = 1;
        }

        if (hasBaseline) {
            depth -= baselinePreDistance;
        }
        // Get position from horizontal/vertical bias.
        dimension = widget.mListAnchors[startOffset].getMargin() * flow + getParentBiasOffset(widget, orientation);
        int downDepth = dimension + depth;
        int postTemp = ((orientation == ConstraintWidget.HORIZONTAL) ? widget.getWidth() : widget.getHeight()) * flow;
        for (ResolutionNode targetNode : widget.mListAnchors[startOffset].getResolutionNode().dependents) {
            final ResolutionAnchor anchor = (ResolutionAnchor) targetNode;
            dimensionPre = Math.max(dimensionPre, getMaxDimensionTraversal(anchor.myAnchor.mOwner, orientation, topLeftFlow, downDepth));
        }
        for (ResolutionNode targetNode : widget.mListAnchors[endOffset].getResolutionNode().dependents) {
            final ResolutionAnchor anchor = (ResolutionAnchor) targetNode;
            dimensionPost = Math.max(dimensionPost, getMaxDimensionTraversal(anchor.myAnchor.mOwner, orientation, topLeftFlow, postTemp + downDepth));
        }
        if (hasBaseline) {
            dimensionPre -= baselinePreDistance;
            dimensionPost += baselinePostDistance;
        } else {
            dimensionPost += ((orientation == ConstraintWidget.HORIZONTAL) ? widget.getWidth() : widget.getHeight()) * flow;
        }

        // Baseline, only add distance from baseline to bottom instead of entire height.
        int dimensionBaseline = 0;
        if (orientation == ConstraintWidget.VERTICAL) {
            for (ResolutionNode targetNode : widget.mBaseline.getResolutionNode().dependents) {
                final ResolutionAnchor anchor = (ResolutionAnchor) targetNode;
                if (flow == 1) {
                    dimensionBaseline = Math.max(dimensionBaseline, getMaxDimensionTraversal(anchor.myAnchor.mOwner, orientation, topLeftFlow, baselinePreDistance + downDepth));
                } else {
                    dimensionBaseline = Math.max(dimensionBaseline, getMaxDimensionTraversal(anchor.myAnchor.mOwner, orientation, topLeftFlow, (baselinePostDistance * flow) + downDepth));
                }
            }
            if (widget.mBaseline.getResolutionNode().dependents.size() > 0 && !hasBaseline) {
                if (flow == 1) {
                    dimensionBaseline += baselinePreDistance;
                } else {
                    dimensionBaseline -= baselinePostDistance;
                }
            }
        }

        int distanceBeforeWidget = dimension;
        dimension += Math.max(dimensionPre, Math.max(dimensionPost, dimensionBaseline));
        int leftTop = depth + distanceBeforeWidget;
        int end = leftTop + postTemp;
        if (flow == -1) {
            int aux = end;
            end = leftTop;
            leftTop = aux;
        }
        if (topLeftFlow) {
            Optimizer.setOptimizedWidget(widget, orientation, leftTop);
            widget.setFrame(leftTop, end, orientation);
        } else {
            widget.mBelongingGroup.addWidgetsToSet(widget, orientation);
            widget.setRelativePositioning(leftTop, orientation);
        }
        // Assuming widgets with only one dimension on Match_constraint would be measurable.
        if (widget.getDimensionBehaviour(orientation) == DimensionBehaviour.MATCH_CONSTRAINT
                && widget.mDimensionRatio != 0.0f) {
            widget.mBelongingGroup.addWidgetsToSet(widget, orientation);
        }
        // Assuming is not measurable when the parent is on wrap_content.
        if (widget.mListAnchors[startOffset].mTarget != null
                && widget.mListAnchors[endOffset].mTarget != null) {
            final ConstraintWidget parent = widget.getParent();
            if (widget.mListAnchors[startOffset].mTarget.mOwner == parent
                    && widget.mListAnchors[endOffset].mTarget.mOwner == parent) {
                widget.mBelongingGroup.addWidgetsToSet(widget, orientation);
            }
        }
        return dimension;
    }

    private static void setConnection(ConstraintAnchor originAnchor) {
        ResolutionNode originNode = originAnchor.getResolutionNode();
        if (originAnchor.mTarget != null && originAnchor.mTarget.mTarget != originAnchor) {
            // Go to Owner and add the dependent.
            originAnchor.mTarget.getResolutionNode().addDependent(originNode);
        }
    }

    /**
     * Used when the Analyzer cannot simplify in independent groups.
     * This will make it so all widgets are included in the same group.
     *
     * @param layoutWidget ConstrainedWidgetContainer being analyzed.
     */
    private static void singleGroup(ConstraintWidgetContainer layoutWidget) {
        layoutWidget.mWidgetGroups.clear();
        layoutWidget.mWidgetGroups.add(0, new ConstraintWidgetGroup(layoutWidget.mChildren));
    }

    /**
     * Update widgets positions.
     * Necessary for widgets dependent on the right/bottom side of the Container.
     *
     * @param groups          Groups of widgets being updated.
     * @param orientation     Dimension to update on the widgets.
     * @param containerLength Length of the widget container.
     */
    public static void setPosition(List<ConstraintWidgetGroup> groups, int orientation, int containerLength) {
        final int groupsSize = groups.size();
        for (int i = 0; i < groupsSize; i++) {
            ConstraintWidgetGroup group = groups.get(i);
            for (ConstraintWidget widget : group.getWidgetsToSet(orientation)) {
                // We can only update those that we can measure.
                if (widget.mOptimizerMeasurable) {
                    updateSizeDependentWidgets(widget, orientation, containerLength);
                }
            }
        }
    }

    /**
     * Update the final layout position of widgets that depend on the size of the container.
     * Exception for dimension-ratio as a work-around.
     *
     * @param widget          Widget being updated.
     * @param orientation     Orientation being updated.
     * @param containerLength The final container dimension in the orientation.
     */
    private static void updateSizeDependentWidgets(ConstraintWidget widget, int orientation, int containerLength) {
        final int end;
        final int start;
        final int offset = orientation * 2;
        ConstraintAnchor startAnchor = widget.mListAnchors[offset];
        ConstraintAnchor endAnchor = widget.mListAnchors[offset + 1];
        boolean hasBias = startAnchor.mTarget != null && endAnchor.mTarget != null;
        if (hasBias) {
            start = getParentBiasOffset(widget, orientation) + startAnchor.getMargin();
            Optimizer.setOptimizedWidget(widget, orientation, start);
            return;
        }
        /*
         * ConstraintLayout::internalMeasureChildren() workaround (it would reset the widget's
         * dimension even if it was set beforehand).
         * It is assumed that the left/top anchor has been resolved. Since only the dimension is being reset.
         */
        if (widget.mDimensionRatio != 0.0f && widget.getDimensionBehaviour(orientation) == DimensionBehaviour.MATCH_CONSTRAINT) {
            int length = resolveDimensionRatio(widget);
            start = (int) widget.mListAnchors[offset].getResolutionNode().resolvedOffset;
            end = start + length;
            endAnchor.getResolutionNode().resolvedTarget = startAnchor.getResolutionNode();
            endAnchor.getResolutionNode().resolvedOffset = length;
            endAnchor.getResolutionNode().state = ResolutionNode.RESOLVED;
            widget.setFrame(start, end, orientation);
            return;
        }
        end = containerLength - widget.getRelativePositioning(orientation);
        start = end - widget.getLength(orientation);
        widget.setFrame(start, end, orientation);
        Optimizer.setOptimizedWidget(widget, orientation, start);
    }

    /**
     * Get the offset of a widget with bias exclusively with the parent.
     * Offset is the distance from the left/top side of the parent to the start of the widget.
     *
     * @param orientation Orientation for the offset.
     * @return The distance from the root based on the bias (does not include margin distance). 0 if it can't be calculated.
     */
    private static int getParentBiasOffset(ConstraintWidget widget, int orientation) {
        int offset = orientation * 2;
        ConstraintAnchor startAnchor = widget.mListAnchors[offset];
        ConstraintAnchor endAnchor = widget.mListAnchors[offset + 1];
        if (startAnchor.mTarget != null && startAnchor.mTarget.mOwner == widget.mParent
                && endAnchor.mTarget != null && endAnchor.mTarget.mOwner == widget.mParent) {
            int length = 0;
            int widgetDimension = 0;
            float bias = 0.0f;
            length = widget.mParent.getLength(orientation);
            bias = (orientation == ConstraintWidget.HORIZONTAL) ? widget.mHorizontalBiasPercent :
                    widget.mVerticalBiasPercent;
            widgetDimension = widget.getLength(orientation);
            length = length - startAnchor.getMargin() - endAnchor.getMargin();
            length = length - widgetDimension;
            length = ((int) ((float) length * bias));
            return length;
        } else {
            return 0;
        }
    }

    /**
     * Calculate the widget's dimension based on dimension ratio.
     *
     * @return The dimension calculated.
     */
    private static int resolveDimensionRatio(ConstraintWidget widget) {
        int length = ConstraintWidget.UNKNOWN;
        if (widget.getHorizontalDimensionBehaviour() == DimensionBehaviour.MATCH_CONSTRAINT) {
            if (widget.mDimensionRatioSide == ConstraintWidget.HORIZONTAL) {
                length = (int) ((float) widget.getHeight() * widget.mDimensionRatio);
            } else {
                length = (int) ((float) widget.getHeight() / widget.mDimensionRatio);
            }
            widget.setWidth(length);
        } else if (widget.getVerticalDimensionBehaviour() == DimensionBehaviour.MATCH_CONSTRAINT) {
            if (widget.mDimensionRatioSide == ConstraintWidget.VERTICAL) {
                length = (int) ((float) widget.getWidth() * widget.mDimensionRatio);
            } else {
                length = (int) ((float) widget.getWidth() / widget.mDimensionRatio);
            }
            widget.setHeight(length);
        }
        return length;
    }
}
