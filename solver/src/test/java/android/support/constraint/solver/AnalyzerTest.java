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

package android.support.constraint.solver;

import android.support.constraint.solver.widgets.Analyzer;
import android.support.constraint.solver.widgets.ConstraintAnchor.Type;
import android.support.constraint.solver.widgets.ConstraintWidget;
import android.support.constraint.solver.widgets.ConstraintWidget.DimensionBehaviour;
import android.support.constraint.solver.widgets.ConstraintWidgetContainer;
import android.support.constraint.solver.widgets.Optimizer;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class AnalyzerTest {

    @Test
    public void basicAnalyzerTest() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 800);
        ConstraintWidget A = new ConstraintWidget(20, 20);
        ConstraintWidget B = new ConstraintWidget(20, 20);
        ConstraintWidget C = new ConstraintWidget(20, 20);

        root.setDebugSolverName(root.getSystem(), "root");
        A.setDebugSolverName(root.getSystem(), "A");
        B.setDebugSolverName(root.getSystem(), "B");
        C.setDebugSolverName(root.getSystem(), "C");

        A.connect(Type.LEFT, root, Type.LEFT);
        A.connect(Type.TOP, root, Type.TOP);
        B.connect(Type.LEFT, root, Type.LEFT);
        B.connect(Type.BOTTOM, root, Type.BOTTOM);
        C.connect(Type.RIGHT, root, Type.RIGHT);
        C.connect(Type.TOP, root, Type.TOP);

        root.add(A, B, C);
        root.layout();
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_STANDARD | Optimizer.OPTIMIZATION_GROUPS);

        Analyzer.determineGroups(root);

        assertEquals(root.getWidgetGroups().size(), 3);
    }

    @Test
    public void basicAnalyzerTest2() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 800);
        ConstraintWidget A = new ConstraintWidget(20, 20);
        ConstraintWidget B = new ConstraintWidget(20, 20);
        ConstraintWidget C = new ConstraintWidget(20, 20);

        root.setDebugSolverName(root.getSystem(), "root");
        A.setDebugSolverName(root.getSystem(), "A");
        B.setDebugSolverName(root.getSystem(), "B");
        C.setDebugSolverName(root.getSystem(), "C");

        A.connect(Type.LEFT, root, Type.LEFT);
        A.connect(Type.TOP, root, Type.TOP);
        A.connect(Type.RIGHT, B, Type.RIGHT);
        B.connect(Type.TOP, root, Type.TOP);
        B.connect(Type.RIGHT, C, Type.LEFT);
        C.connect(Type.RIGHT, root, Type.RIGHT);
        C.connect(Type.TOP, root, Type.TOP);

        root.add(A, B, C);
        root.layout();
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_STANDARD | Optimizer.OPTIMIZATION_GROUPS);

        Analyzer.determineGroups(root);

        assertEquals(root.getWidgetGroups().size(), 1);
    }

    @Test
    public void extendedAnalyzerTest() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 800);
        ConstraintWidget A = new ConstraintWidget(20, 20);
        ConstraintWidget B = new ConstraintWidget(20, 20);
        ConstraintWidget C = new ConstraintWidget(20, 20);
        ConstraintWidget D = new ConstraintWidget(20, 20);
        ConstraintWidget E = new ConstraintWidget(20, 20);
        ConstraintWidget F = new ConstraintWidget(20, 20);
        ConstraintWidget G = new ConstraintWidget(20, 20);

        root.setDebugSolverName(root.getSystem(), "root");
        A.setDebugSolverName(root.getSystem(), "A");
        B.setDebugSolverName(root.getSystem(), "B");
        C.setDebugSolverName(root.getSystem(), "C");
        D.setDebugSolverName(root.getSystem(), "D");
        E.setDebugSolverName(root.getSystem(), "E");
        F.setDebugSolverName(root.getSystem(), "F");
        G.setDebugSolverName(root.getSystem(), "G");

        A.connect(Type.LEFT, root, Type.LEFT);
        A.connect(Type.BOTTOM, root, Type.BOTTOM);
        A.connect(Type.RIGHT, B, Type.LEFT);
        B.connect(Type.LEFT, A, Type.RIGHT);
        B.connect(Type.BOTTOM, root, Type.BOTTOM);
        B.connect(Type.RIGHT, C, Type.LEFT);
        C.connect(Type.LEFT, B, Type.RIGHT);
        C.connect(Type.BOTTOM, root, Type.BOTTOM);
        C.connect(Type.RIGHT, root, Type.RIGHT);

        D.connect(Type.LEFT, root, Type.LEFT);
        D.connect(Type.BOTTOM, A, Type.TOP);

        E.connect(Type.RIGHT, root, Type.RIGHT);
        E.connect(Type.BOTTOM, C, Type.TOP);
        E.connect(Type.TOP, F, Type.BOTTOM);

        F.connect(Type.LEFT, root, Type.LEFT);
        F.connect(Type.BOTTOM, D, Type.TOP);

        G.connect(Type.RIGHT, root, Type.RIGHT);
        G.connect(Type.BOTTOM, root, Type.BOTTOM);

        root.add(A, B, C, D, E, F, G);
        root.layout();
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_STANDARD | Optimizer.OPTIMIZATION_GROUPS);

        Analyzer.determineGroups(root);

        assertEquals(root.getWidgetGroups().size(), 2);
        assertEquals(root.getWidgetGroups().get(0).mConstrainedGroup.size(), 6);
        assertEquals(root.getWidgetGroups().get(1).mConstrainedGroup.size(), 1);

        assertFalse(root.getWidgetGroups().get(0).mConstrainedGroup
            .contains(root.getWidgetGroups().get(1).mConstrainedGroup.get(0)));
    }

    @Test
    public void basicWrapContentGroup() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 800);
        ConstraintWidget A = new ConstraintWidget(20, 20);
        ConstraintWidget B = new ConstraintWidget(20, 20);
        ConstraintWidget C = new ConstraintWidget(20, 20);

        root.setDebugSolverName(root.getSystem(), "root");
        A.setDebugSolverName(root.getSystem(), "A");
        B.setDebugSolverName(root.getSystem(), "B");
        C.setDebugSolverName(root.getSystem(), "C");

        A.connect(Type.LEFT, root, Type.LEFT);
        A.connect(Type.TOP, root, Type.TOP);
        B.connect(Type.LEFT, root, Type.LEFT);
        B.connect(Type.TOP, A, Type.BOTTOM);
        C.connect(Type.LEFT, root, Type.LEFT);
        C.connect(Type.TOP, A, Type.TOP);

        root.add(A, B, C);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_STANDARD | Optimizer.OPTIMIZATION_GROUPS);
        root.setHorizontalDimensionBehaviour(DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(DimensionBehaviour.WRAP_CONTENT);

        Analyzer.determineGroups(root);

        // Make sure the right number of groups is identified and have the right properties.
        assertEquals(root.getWidgetGroups().size(), 1);
        assertEquals(root.getWidgetGroups().get(0).mGroupDimensions[0], 20);
        assertEquals(root.getWidgetGroups().get(0).mGroupDimensions[1], 40);
        assertEquals(root.getWidgetGroups().get(0).getStartWidgets(0).size(), 3);
        assertEquals(root.getWidgetGroups().get(0).getStartWidgets(1).size(), 1);
        // The layout widget should now have the maximum width and height and be set as fixed.
        assertEquals(root.getWidth(), 20);
        assertEquals(root.getHeight(), 40);
        assertEquals(root.getHorizontalDimensionBehaviour(), DimensionBehaviour.FIXED);
        assertEquals(root.getVerticalDimensionBehaviour(), DimensionBehaviour.FIXED);
    }

    @Test
    public void twoDirectionWrapContentGroup() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 800);
        ConstraintWidget A = new ConstraintWidget(20, 20);
        ConstraintWidget B = new ConstraintWidget(10, 10);

        ConstraintWidget C = new ConstraintWidget(20, 20);
        ConstraintWidget D = new ConstraintWidget(20, 20);

        root.setDebugSolverName(root.getSystem(), "root");
        A.setDebugSolverName(root.getSystem(), "A");
        B.setDebugSolverName(root.getSystem(), "B");
        C.setDebugSolverName(root.getSystem(), "C");
        D.setDebugSolverName(root.getSystem(), "D");

        A.connect(Type.LEFT, root, Type.LEFT);
        A.connect(Type.TOP, root, Type.TOP);
        B.connect(Type.LEFT, root, Type.LEFT);
        B.connect(Type.TOP, A, Type.BOTTOM);

        C.connect(Type.LEFT, root, Type.LEFT);
        C.connect(Type.BOTTOM, root, Type.BOTTOM);
        D.connect(Type.LEFT, root, Type.LEFT);
        D.connect(Type.BOTTOM, C, Type.BOTTOM);

        root.add(A, B, C, D);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_STANDARD | Optimizer.OPTIMIZATION_GROUPS);
        root.setHorizontalDimensionBehaviour(DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(DimensionBehaviour.WRAP_CONTENT);

        Analyzer.determineGroups(root);

        assertEquals(root.getWidgetGroups().size(), 2);
        assertEquals(root.getWidgetGroups().get(0).mGroupDimensions[0], 20);
        assertEquals(root.getWidgetGroups().get(0).mGroupDimensions[1], 30);
        assertEquals(root.getWidgetGroups().get(1).mGroupDimensions[0], 20);
        assertEquals(root.getWidgetGroups().get(1).mGroupDimensions[1], 20);
        assertEquals(root.getWidth(), 20);
        assertEquals(root.getHeight(), 30);
    }

    @Test
    public void horizontalVerticalWrapContentGroup() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 800);
        ConstraintWidget A = new ConstraintWidget(25, 22);
        ConstraintWidget B = new ConstraintWidget(25, 22);

        ConstraintWidget C = new ConstraintWidget(20, 10);
        ConstraintWidget D = new ConstraintWidget(20, 20);

        root.setDebugSolverName(root.getSystem(), "root");
        A.setDebugSolverName(root.getSystem(), "A");
        B.setDebugSolverName(root.getSystem(), "B");
        C.setDebugSolverName(root.getSystem(), "C");
        D.setDebugSolverName(root.getSystem(), "D");

        A.connect(Type.LEFT, root, Type.LEFT);
        A.connect(Type.TOP, root, Type.TOP);
        B.connect(Type.LEFT, root, Type.LEFT);
        B.connect(Type.TOP, A, Type.BOTTOM);

        C.connect(Type.LEFT, root, Type.LEFT);
        C.connect(Type.BOTTOM, root, Type.BOTTOM);
        D.connect(Type.LEFT, C, Type.RIGHT);
        D.connect(Type.BOTTOM, root, Type.BOTTOM);

        root.add(A, B, C, D);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_STANDARD | Optimizer.OPTIMIZATION_GROUPS);
        root.setHorizontalDimensionBehaviour(DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(DimensionBehaviour.WRAP_CONTENT);

        Analyzer.determineGroups(root);

        assertEquals(root.getWidgetGroups().size(), 2);
        assertEquals(root.getWidgetGroups().get(0).mGroupDimensions[0], 25);
        assertEquals(root.getWidgetGroups().get(0).mGroupDimensions[1], 44);
        assertEquals(root.getWidgetGroups().get(1).mGroupDimensions[0], 40);
        assertEquals(root.getWidgetGroups().get(1).mGroupDimensions[1], 20);
        assertEquals(root.getWidth(), 40);
        assertEquals(root.getHeight(), 44);
    }

    @Test
    public void ignoreWidgetOnReverseFlowWrapContentGroup() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 800);
        ConstraintWidget A = new ConstraintWidget(20, 20);
        ConstraintWidget B = new ConstraintWidget(20, 20);
        ConstraintWidget C = new ConstraintWidget(30, 100);

        root.setDebugSolverName(root.getSystem(), "root");
        A.setDebugSolverName(root.getSystem(), "A");
        B.setDebugSolverName(root.getSystem(), "B");
        C.setDebugSolverName(root.getSystem(), "C");

        A.connect(Type.LEFT, root, Type.LEFT);
        A.connect(Type.TOP, root, Type.TOP);
        B.connect(Type.LEFT, root, Type.LEFT);
        B.connect(Type.TOP, A, Type.BOTTOM);
        // Widget C is going in a different direction: bottom to top, instead of top to bottom.
        C.connect(Type.LEFT, root, Type.LEFT);
        C.connect(Type.BOTTOM, B, Type.BOTTOM);

        root.add(A, B, C);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_STANDARD | Optimizer.OPTIMIZATION_GROUPS);
        root.setHorizontalDimensionBehaviour(DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(DimensionBehaviour.WRAP_CONTENT);

        Analyzer.determineGroups(root);

        assertEquals(root.getWidgetGroups().size(), 1);
        assertEquals(root.getWidth(), 30);
        assertEquals(root.getHeight(), 40);
    }

    @Test
    public void maxSizeOnReverseDirectionWrapTest() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 800);
        ConstraintWidget A = new ConstraintWidget(20, 20);
        ConstraintWidget B = new ConstraintWidget(20, 20);
        ConstraintWidget C = new ConstraintWidget(30, 100);
        ConstraintWidget D = new ConstraintWidget(20, 200);

        root.setDebugSolverName(root.getSystem(), "root");
        A.setDebugSolverName(root.getSystem(), "A");
        B.setDebugSolverName(root.getSystem(), "B");
        C.setDebugSolverName(root.getSystem(), "C");
        D.setDebugSolverName(root.getSystem(), "D");

        A.connect(Type.LEFT, root, Type.LEFT);
        A.connect(Type.TOP, root, Type.TOP);
        B.connect(Type.LEFT, root, Type.LEFT);
        B.connect(Type.TOP, A, Type.BOTTOM);
        // Widget C is going in a different direction: bottom to top, instead of top to bottom.
        C.connect(Type.LEFT, root, Type.LEFT);
        C.connect(Type.BOTTOM, B, Type.BOTTOM);
        D.connect(Type.LEFT, root, Type.LEFT);
        D.connect(Type.TOP, C, Type.TOP);

        root.add(A, B, C, D);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_STANDARD | Optimizer.OPTIMIZATION_GROUPS);
        root.setHorizontalDimensionBehaviour(DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(DimensionBehaviour.WRAP_CONTENT);

        Analyzer.determineGroups(root);

        assertEquals(root.getWidgetGroups().size(), 1);
        assertEquals(root.getWidth(), 30);
        assertEquals(root.getHeight(), 140);
    }

    @Test
    public void basicBaselineWrapTest() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 400, 400);
        ConstraintWidget A = new ConstraintWidget(20, 20);
        ConstraintWidget B = new ConstraintWidget(20, 20);
        A.setBaselineDistance(10);
        B.setBaselineDistance(5);

        root.setDebugSolverName(root.getSystem(), "root");
        A.setDebugSolverName(root.getSystem(), "A");
        B.setDebugSolverName(root.getSystem(), "B");

        A.connect(Type.LEFT, root, Type.LEFT);
        A.connect(Type.TOP, root, Type.TOP);
        B.connect(Type.LEFT, root, Type.LEFT);
        B.connect(Type.BASELINE, A, Type.BASELINE);

        root.add(A, B);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_STANDARD | Optimizer.OPTIMIZATION_GROUPS);
        root.setHorizontalDimensionBehaviour(DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(DimensionBehaviour.WRAP_CONTENT);

        Analyzer.determineGroups(root);

        assertEquals(root.getWidgetGroups().size(), 1);
        assertEquals(root.getWidth(), 20);
        assertEquals(root.getHeight(), 25);
    }

    @Test
    public void baselineToBaselineWrapTest() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 400, 400);
        ConstraintWidget A = new ConstraintWidget(20, 20);
        ConstraintWidget B = new ConstraintWidget(20, 20);
        ConstraintWidget C = new ConstraintWidget(20, 80);
        A.setBaselineDistance(5);
        B.setBaselineDistance(10);
        C.setBaselineDistance(15);

        root.setDebugSolverName(root.getSystem(), "root");
        A.setDebugSolverName(root.getSystem(), "A");
        B.setDebugSolverName(root.getSystem(), "B");
        C.setDebugSolverName(root.getSystem(), "C");

        A.connect(Type.LEFT, root, Type.LEFT);
        A.connect(Type.BOTTOM, root, Type.BOTTOM);
        B.connect(Type.LEFT, A, Type.RIGHT);
        B.connect(Type.BASELINE, A, Type.BASELINE);
        C.connect(Type.LEFT, B, Type.RIGHT);
        C.connect(Type.BASELINE, A, Type.BASELINE);

        root.add(A, B, C);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_STANDARD | Optimizer.OPTIMIZATION_GROUPS);
        root.setHorizontalDimensionBehaviour(DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(DimensionBehaviour.WRAP_CONTENT);

        Analyzer.determineGroups(root);

        assertEquals(root.getWidgetGroups().size(), 1);
        assertEquals(root.getWidth(), 60);
        assertEquals(root.getHeight(), 30);
    }

    @Test
    public void skipSolverBasic() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 800);
        ConstraintWidget A = new ConstraintWidget(20, 20);
        ConstraintWidget B = new ConstraintWidget(20, 20);
        ConstraintWidget C = new ConstraintWidget(20, 20);

        root.setDebugSolverName(root.getSystem(), "root");
        A.setDebugSolverName(root.getSystem(), "A");
        B.setDebugSolverName(root.getSystem(), "B");
        C.setDebugSolverName(root.getSystem(), "C");

        A.connect(Type.LEFT, root, Type.LEFT);
        A.connect(Type.TOP, root, Type.TOP);
        B.connect(Type.LEFT, A, Type.LEFT);
        B.connect(Type.TOP, A, Type.BOTTOM);
        C.connect(Type.LEFT, B, Type.LEFT);
        C.connect(Type.TOP, B, Type.BOTTOM);

        root.add(A, B, C);
        root.setHorizontalDimensionBehaviour(DimensionBehaviour.FIXED);
        root.setVerticalDimensionBehaviour(DimensionBehaviour.FIXED);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_STANDARD | Optimizer.OPTIMIZATION_GROUPS);
        Analyzer.determineGroups(root);

        assertTrue(root.mSkipSolver);
        assertEquals(root.mWidgetGroups.size(), 1);
        assertTrue(root.mWidgetGroups.get(0).mSkipSolver);
    }

    @Test
    public void skipSolverWrap() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 800);
        ConstraintWidget A = new ConstraintWidget(20, 20);
        ConstraintWidget B = new ConstraintWidget(20, 20);
        ConstraintWidget C = new ConstraintWidget(20, 20);

        root.setDebugSolverName(root.getSystem(), "root");
        A.setDebugSolverName(root.getSystem(), "A");
        B.setDebugSolverName(root.getSystem(), "B");
        C.setDebugSolverName(root.getSystem(), "C");

        A.connect(Type.LEFT, root, Type.LEFT);
        A.connect(Type.TOP, root, Type.TOP);
        B.connect(Type.LEFT, A, Type.LEFT);
        B.connect(Type.TOP, A, Type.BOTTOM);
        C.connect(Type.LEFT, B, Type.LEFT);
        C.connect(Type.TOP, B, Type.BOTTOM);

        root.add(A, B, C);
        root.setHorizontalDimensionBehaviour(DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(DimensionBehaviour.WRAP_CONTENT);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_STANDARD | Optimizer.OPTIMIZATION_GROUPS);
        Analyzer.determineGroups(root);

        assertTrue(root.mSkipSolver);
        assertEquals(root.mWidgetGroups.size(), 1);
        assertTrue(root.mWidgetGroups.get(0).mSkipSolver);
    }

    @Test
    public void skipSolverMixedDimBehaviour() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 800);
        ConstraintWidget A = new ConstraintWidget(20, 20);
        ConstraintWidget B = new ConstraintWidget(20, 20);
        ConstraintWidget C = new ConstraintWidget(20, 20);

        root.setDebugSolverName(root.getSystem(), "root");
        A.setDebugSolverName(root.getSystem(), "A");
        B.setDebugSolverName(root.getSystem(), "B");
        C.setDebugSolverName(root.getSystem(), "C");

        A.connect(Type.LEFT, root, Type.LEFT);
        A.connect(Type.TOP, root, Type.TOP);
        B.connect(Type.LEFT, A, Type.LEFT);
        B.connect(Type.TOP, A, Type.BOTTOM);
        C.connect(Type.LEFT, B, Type.LEFT);
        C.connect(Type.TOP, B, Type.BOTTOM);

        root.add(A, B, C);
        root.setHorizontalDimensionBehaviour(DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(DimensionBehaviour.FIXED);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_STANDARD | Optimizer.OPTIMIZATION_GROUPS);
        Analyzer.determineGroups(root);

        assertTrue(root.mSkipSolver);
        assertEquals(root.mWidgetGroups.size(), 1);
        assertTrue(root.mWidgetGroups.get(0).mSkipSolver);
    }

    @Test
    public void skipSolverOneGroup() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 800);
        ConstraintWidget A = new ConstraintWidget(20, 20);
        ConstraintWidget B = new ConstraintWidget(20, 20);
        ConstraintWidget C = new ConstraintWidget(20, 20);

        root.setDebugSolverName(root.getSystem(), "root");
        A.setDebugSolverName(root.getSystem(), "A");
        B.setDebugSolverName(root.getSystem(), "B");
        C.setDebugSolverName(root.getSystem(), "C");

        A.connect(Type.LEFT, root, Type.LEFT);
        A.connect(Type.TOP, root, Type.TOP);
        B.connect(Type.RIGHT, root, Type.RIGHT);
        B.connect(Type.TOP, root, Type.TOP);
        B.connect(Type.BOTTOM, C, Type.TOP);
        C.connect(Type.RIGHT, root, Type.RIGHT);
        C.connect(Type.BOTTOM, root, Type.BOTTOM);

        root.add(A, B, C);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_STANDARD | Optimizer.OPTIMIZATION_GROUPS);
        Analyzer.determineGroups(root);

        assertFalse(root.mSkipSolver);
        assertEquals(root.mWidgetGroups.size(), 2);
        assertTrue(root.mWidgetGroups.get(0).mSkipSolver);
        assertFalse(root.mWidgetGroups.get(1).mSkipSolver);
    }
}
