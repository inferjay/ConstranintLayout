 /*
  * Copyright (C) 2018 The Android Open Source Project * Copyright (C) 201
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

 import static android.support.constraint.solver.widgets.ConstraintWidget.HORIZONTAL;
 import static android.support.constraint.solver.widgets.ConstraintWidget.UNKNOWN;
 import static android.support.constraint.solver.widgets.ConstraintWidget.VERTICAL;

 import java.util.ArrayList;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Set;

 /**
  * Class for groups of widgets constrained between each other in a ConstraintWidgetContainer.
  * <p>
  * Will possess the list of ConstraintWidget in the group.
  * The mChains that exist in the group.
  * Each group can be solved for individually.
  */
 public class ConstraintWidgetGroup {

     public List<ConstraintWidget> mConstrainedGroup;
     int mGroupWidth = UNKNOWN;
     int mGroupHeight = UNKNOWN;
     public boolean mSkipSolver = false;
     public final int[] mGroupDimensions = {mGroupWidth, mGroupHeight};
     /**
      * Arrays to contain the widgets that determine the start of a group relative to their layout.
      * A widget is at the start, if their left/top anchor is constrained to their parent.
      * If the left/top constraint is null, is considered at the start if there are no widgets
      * constrained to it from their right/bottom anchor.
      */
     List<ConstraintWidget> mStartHorizontalWidgets = new ArrayList<>();
     List<ConstraintWidget> mStartVerticalWidgets = new ArrayList<>();
     HashSet<ConstraintWidget> mWidgetsToSetHorizontal = new HashSet<>();
     HashSet<ConstraintWidget> mWidgetsToSetVertical = new HashSet<>();
     List<ConstraintWidget> mWidgetsToSolve = new ArrayList<>();
     List<ConstraintWidget> mUnresolvedWidgets = new ArrayList<>();

     ConstraintWidgetGroup(List<ConstraintWidget> widgets) {
         this.mConstrainedGroup = widgets;
     }

     ConstraintWidgetGroup(List<ConstraintWidget> widgets, boolean skipSolver) {
         this.mConstrainedGroup = widgets;
         this.mSkipSolver = skipSolver;
     }

     public List<ConstraintWidget> getStartWidgets(int orientation) {
         if (orientation == HORIZONTAL) {
             return mStartHorizontalWidgets;
         } else if (orientation == VERTICAL) {
             return mStartVerticalWidgets;
         }
         return null;
     }

     Set<ConstraintWidget> getWidgetsToSet(int orientation) {
         if (orientation == HORIZONTAL) {
             return mWidgetsToSetHorizontal;
         } else if (orientation == VERTICAL) {
             return mWidgetsToSetVertical;
         }
         return null;
     }

     void addWidgetsToSet(ConstraintWidget widget, int orientation) {
         if (orientation == HORIZONTAL) {
             mWidgetsToSetHorizontal.add(widget);
         } else if (orientation == VERTICAL) {
             mWidgetsToSetVertical.add(widget);
         }
     }

     /**
      * Get a list of widgets that haven't been fully resolved and require the Linear Solver
      * to resolve.
      * Sets {@link #mUnresolvedWidgets} with the widgets that haven't been resolved, but don't
      * require the Linear Solver.
      *
      * @return List of widgets to be solved.
      */
     List<ConstraintWidget> getWidgetsToSolve() {
         if (!mWidgetsToSolve.isEmpty()) {
             return mWidgetsToSolve;
         }
         final int size = mConstrainedGroup.size();
         for (int i = 0; i < size; i++) {
             ConstraintWidget widget = mConstrainedGroup.get(i);
             if (!widget.mOptimizerMeasurable) {
                 getWidgetsToSolveTraversal((ArrayList<ConstraintWidget>)mWidgetsToSolve, widget);
             }
         }
         mUnresolvedWidgets.clear();
         mUnresolvedWidgets.addAll(mConstrainedGroup);
         mUnresolvedWidgets.removeAll(mWidgetsToSolve);
         return mWidgetsToSolve;
     }

     /**
      * Helper method to find widgets to be solved.
      *
      * @param widgetsToSolve Current list of widgets to be solved.
      * @param widget         Widget being traversed.
      */
     private void getWidgetsToSolveTraversal(ArrayList<ConstraintWidget> widgetsToSolve, ConstraintWidget widget) {
         if (widget.mGroupsToSolver) {
             return;
         }
         widgetsToSolve.add(widget);
         widget.mGroupsToSolver = true;
         if (widget.isFullyResolved()) {
             return;
         }
         if (widget instanceof Helper) {
             Helper helper = (Helper) widget;
             final int widgetCount = helper.mWidgetsCount;
             for (int i = 0; i < widgetCount; i++) {
                 getWidgetsToSolveTraversal(widgetsToSolve, helper.mWidgets[i]);
             }
         }
         // Propagate from every unmeasurable widget to the parent.
         final int count = widget.mListAnchors.length;
         for (int i = 0; i < count; i++) {
             ConstraintAnchor targetAnchor = widget.mListAnchors[i].mTarget;
             ConstraintWidget targetWidget = null;
             if (targetAnchor != null) {
                 targetWidget = targetAnchor.mOwner;
             } else {
                 continue;
             }
             // Traverse until we hit a resolved widget or the parent.
             if (targetAnchor != null && (targetWidget != widget.getParent())) {
                 getWidgetsToSolveTraversal(widgetsToSolve, targetWidget);
             }
         }
     }

     /**
      * After solving, update any widgets that depended on unmeasurable widgets.
      */
     void updateUnresolvedWidgets() {
         final int size = mUnresolvedWidgets.size();
         for (int i = 0; i < size; i++) {
             ConstraintWidget widget = mUnresolvedWidgets.get(i);
             // Needs start, end, orientation.
             // Or left,right/top, bottom.
             updateResolvedDimension(widget);
         }
     }

     /**
      * Update widget's dimension according to the widget it depends on.
      *
      * @param widget Widget to resolve dimension.
      */
     private void updateResolvedDimension(ConstraintWidget widget) {
         int start = 0, end = 0;
         if (widget.mOptimizerMeasurable) {
             // No need to update dimension if it has been resolved.
             if (widget.isFullyResolved()) {
                 return;
             }
             // Horizontal.
             boolean rightSide = widget.mRight.mTarget != null;
             ConstraintAnchor targetAnchor;
             // Get measure if target is resolved, otherwise, resolve target.
             if (rightSide) {
                 targetAnchor = widget.mRight.mTarget;
             } else {
                 targetAnchor = widget.mLeft.mTarget;
             }
             if (targetAnchor != null) {
                 if (!targetAnchor.mOwner.mOptimizerMeasured) {
                     updateResolvedDimension(targetAnchor.mOwner);
                 }
                 if (targetAnchor.mType == ConstraintAnchor.Type.RIGHT) {
                     end = targetAnchor.mOwner.mX + targetAnchor.mOwner.getWidth();
                 } else if (targetAnchor.mType == ConstraintAnchor.Type.LEFT) {
                     end = targetAnchor.mOwner.mX;
                 }
             }
             if (rightSide) {
                 end -= widget.mRight.getMargin();
             } else {
                 end += widget.mLeft.getMargin() + widget.getWidth();
             }
             start = end - widget.getWidth();
             widget.setHorizontalDimension(start, end);
             // Vertical.
             if (widget.mBaseline.mTarget != null) {
                 targetAnchor = widget.mBaseline.mTarget;
                 if (!targetAnchor.mOwner.mOptimizerMeasured) {
                     updateResolvedDimension(targetAnchor.mOwner);
                 }
                 start = targetAnchor.mOwner.mY + targetAnchor.mOwner.mBaselineDistance
                         - widget.mBaselineDistance;
                 end = start + widget.mHeight;
                 widget.setVerticalDimension(start, end);
                 widget.mOptimizerMeasured = true;
                 return;
             }
             boolean bottomSide = widget.mBottom.mTarget != null;
             // Get measure if target is resolved, otherwise, resolve target.
             if (bottomSide) {
                 targetAnchor = widget.mBottom.mTarget;
             } else {
                 targetAnchor = widget.mTop.mTarget;
             }
             if (targetAnchor != null) {
                 if (!targetAnchor.mOwner.mOptimizerMeasured) {
                     updateResolvedDimension(targetAnchor.mOwner);
                 }
                 if (targetAnchor.mType == ConstraintAnchor.Type.BOTTOM) {
                     end = targetAnchor.mOwner.mY + targetAnchor.mOwner.getHeight();
                 } else if (targetAnchor.mType == ConstraintAnchor.Type.TOP) {
                     end = targetAnchor.mOwner.mY;
                 }
             }
             if (bottomSide) {
                 end -= widget.mBottom.getMargin();
             } else {
                 end += widget.mTop.getMargin() + widget.getHeight();
             }
             start = end - widget.getHeight();
             widget.setVerticalDimension(start, end);
             widget.mOptimizerMeasured = true;
         }
     }
 }
