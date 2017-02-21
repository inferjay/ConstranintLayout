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
package android.support.constraint.solver;

import android.support.constraint.solver.widgets.ConstraintAnchor;
import android.support.constraint.solver.widgets.ConstraintWidget;
import android.support.constraint.solver.widgets.ConstraintWidgetContainer;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class RatioTest {

    @Test
    public void testBasicRatio() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 600);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        A.setDebugName("A");
        root.add(A);
        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        A.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        A.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        A.setVerticalBiasPercent(0);
        A.setHorizontalBiasPercent(0);
        A.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        A.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        A.setDimensionRatio("1:1");
        root.setOptimizationLevel(ConstraintWidgetContainer.OPTIMIZATION_NONE);
        root.layout();
        System.out.println("a) root: " + root + " A: " + A);
        assertEquals(A.getLeft(), 0);
        assertEquals(A.getTop(), 0);
        assertEquals(A.getWidth(), 600);
        assertEquals(A.getHeight(), 600);
        root.setOptimizationLevel(ConstraintWidgetContainer.OPTIMIZATION_ALL);
        root.layout();
        System.out.println("b) root: " + root + " A: " + A);
        assertEquals(A.getLeft(), 0);
        assertEquals(A.getTop(), 0);
        assertEquals(A.getWidth(), 600);
        assertEquals(A.getHeight(), 600);
    }

    @Test
    public void testRatioWithMinimum() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 600);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        A.setDebugName("A");
        root.add(A);
        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        A.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        A.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        A.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        A.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        A.setDimensionRatio("16:9");
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setOptimizationLevel(ConstraintWidgetContainer.OPTIMIZATION_ALL);
        root.setWidth(0);
        root.setHeight(0);
        root.layout();
        System.out.println("a) root: " + root + " A: " + A);
        assertEquals(root.getWidth(), 0);
        assertEquals(root.getHeight(), 0);
        A.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_SPREAD, 100, 0);
        root.setWidth(0);
        root.setHeight(0);
        root.layout();
        System.out.println("b) root: " + root + " A: " + A);
        assertEquals(root.getWidth(), 100);
        assertEquals(root.getHeight(), 56);
        A.setVerticalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_SPREAD, 100, 0);
        root.setWidth(0);
        root.setHeight(0);
        root.layout();
        System.out.println("c) root: " + root + " A: " + A);
        assertEquals(root.getWidth(), 178);
        assertEquals(root.getHeight(), 100);
    }
 }
