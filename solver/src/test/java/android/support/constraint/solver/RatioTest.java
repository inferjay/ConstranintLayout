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
import android.support.constraint.solver.widgets.Optimizer;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class RatioTest {

    @Test
    public void testRatioMax() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 1000);
        ConstraintWidget A = new ConstraintWidget(100, 100);
        root.setDebugName("root");
        root.add(A);
        A.setDebugName("A");

        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);

        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        A.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);

        A.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        A.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        A.setVerticalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_RATIO, 0, 150, 0);
        A.setDimensionRatio("W,16:9");

        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.layout();

        System.out.println("a) root: " + root + " A: " + A);
        assertEquals(A.getWidth(), 267);
        assertEquals(A.getHeight(), 150);
        assertEquals(A.getTop(), 425);
    }

    @Test
    public void testRatioSingleTarget() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 1000);
        ConstraintWidget A = new ConstraintWidget(100, 100);
        ConstraintWidget B = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        root.add(A);
        root.add(B);
        A.setDebugName("A");
        B.setDebugName("B");

        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        A.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        A.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);

        B.connect(ConstraintAnchor.Type.TOP, A, ConstraintAnchor.Type.BOTTOM);
        B.connect(ConstraintAnchor.Type.BOTTOM, A, ConstraintAnchor.Type.BOTTOM);
        B.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        B.setDimensionRatio("2:3");
        B.connect(ConstraintAnchor.Type.LEFT, A, ConstraintAnchor.Type.LEFT, 50);

        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.layout();

        System.out.println("a) root: " + root + " A: " + A + " B: " + B);
        assertEquals(B.getHeight(), 150);
        assertEquals(B.getTop(), A.getBottom() - B.getHeight() / 2);
    }

    @Test
    public void testSimpleWrapRatio() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 1000);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        root.add(A);
        A.setDebugName("A");

        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        A.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        A.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);


        A.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        A.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);

        A.setDimensionRatio("1:1");
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();

        System.out.println("a) root: " + root + " A: " + A);
        assertEquals(root.getWidth(), 1000);
        assertEquals(root.getHeight(), 1000);
        assertEquals(A.getWidth(), 1000);
        assertEquals(A.getHeight(), 1000);
    }

    @Test
    public void testSimpleWrapRatio2() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 1000);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        root.add(A);
        A.setDebugName("A");

        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        A.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        A.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);


        A.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        A.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);

        A.setDimensionRatio("1:1");
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();

        System.out.println("a) root: " + root + " A: " + A);
        assertEquals(root.getWidth(), 1000);
        assertEquals(root.getHeight(), 1000);
        assertEquals(A.getWidth(), 1000);
        assertEquals(A.getHeight(), 1000);
    }

    @Test
    public void testNestedRatio() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 1000);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        ConstraintWidget B = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        A.setDebugName("A");
        B.setDebugName("B");
        root.add(A);
        root.add(B);
        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        A.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        A.connect(ConstraintAnchor.Type.BOTTOM, B, ConstraintAnchor.Type.TOP);

        B.connect(ConstraintAnchor.Type.LEFT, A, ConstraintAnchor.Type.LEFT);
        B.connect(ConstraintAnchor.Type.RIGHT, A, ConstraintAnchor.Type.RIGHT);
        B.connect(ConstraintAnchor.Type.TOP, A, ConstraintAnchor.Type.BOTTOM);
        B.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);

        A.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        A.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        B.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        B.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);

        A.setDimensionRatio("1:1");
        B.setDimensionRatio("1:1");

        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();

        System.out.println("a) root: " + root + " A: " + A + " B: " + B);
        assertEquals(root.getWidth(), 500);
        assertEquals(A.getWidth(), 500);
        assertEquals(B.getWidth(), 500);
        assertEquals(root.getHeight(), 1000);
        assertEquals(A.getHeight(), 500);
        assertEquals(B.getHeight(), 500);
    }


    @Test
    public void testBasicCenter() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 600);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        A.setDebugName("A");
        root.add(A);
        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        A.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        A.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.layout();
        System.out.println("a) root: " + root + " A: " + A);
        assertEquals(A.getLeft(), 450);
        assertEquals(A.getTop(), 290);
        assertEquals(A.getWidth(), 100);
        assertEquals(A.getHeight(), 20);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_STANDARD);
        root.layout();
        System.out.println("b) root: " + root + " A: " + A);
        assertEquals(A.getLeft(), 450);
        assertEquals(A.getTop(), 290);
        assertEquals(A.getWidth(), 100);
        assertEquals(A.getHeight(), 20);
    }

    @Test
    public void testBasicRatio() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 1000);
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
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.layout();
        System.out.println("a) root: " + root + " A: " + A);
        assertEquals(A.getLeft(), 0);
        assertEquals(A.getTop(), 0);
        assertEquals(A.getWidth(), 600);
        assertEquals(A.getHeight(), 600);
        A.setVerticalBiasPercent(1);
        root.layout();
        System.out.println("b) root: " + root + " A: " + A);
        assertEquals(A.getLeft(), 0);
        assertEquals(A.getTop(), 400);
        assertEquals(A.getWidth(), 600);
        assertEquals(A.getHeight(), 600);

        A.setVerticalBiasPercent(0);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_STANDARD);
        root.layout();
        System.out.println("c) root: " + root + " A: " + A);
        assertEquals(A.getLeft(), 0);
        assertEquals(A.getTop(), 0);
        assertEquals(A.getWidth(), 600);
        assertEquals(A.getHeight(), 600);
    }

    @Test
    public void testBasicRatio2() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 600);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        A.setDebugName("A");
        root.add(A);
        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        A.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        A.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        A.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        A.setDimensionRatio("1:1");
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.layout();
        System.out.println("a) root: " + root + " A: " + A);
        assertEquals(A.getLeft(), 450);
        assertEquals(A.getTop(), 250);
        assertEquals(A.getWidth(), 100);
        assertEquals(A.getHeight(), 100);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_STANDARD);
        root.layout();
        System.out.println("b) root: " + root + " A: " + A);
        assertEquals(A.getLeft(), 450);
        assertEquals(A.getTop(), 250);
        assertEquals(A.getWidth(), 100);
        assertEquals(A.getHeight(), 100);
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
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.setWidth(0);
        root.setHeight(0);
        root.layout();
        System.out.println("a) root: " + root + " A: " + A);
        assertEquals(root.getWidth(), 0);
        assertEquals(root.getHeight(), 0);
        A.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_SPREAD, 100, 0, 0);
        root.setWidth(0);
        root.setHeight(0);
        root.layout();
        System.out.println("b) root: " + root + " A: " + A);
        assertEquals(root.getWidth(), 100);
        assertEquals(root.getHeight(), 56);
        A.setVerticalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_SPREAD, 100, 0, 0);
        root.setWidth(0);
        root.setHeight(0);
        root.layout();
        System.out.println("c) root: " + root + " A: " + A);
        assertEquals(root.getWidth(), 178);
        assertEquals(root.getHeight(), 100);
    }

    @Test
    public void testRatioWithPercent() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 1000);
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
        A.setDimensionRatio("1:1");
        A.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_PERCENT, 0, 0, 0.7f);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.layout();
        System.out.println("a) root: " + root + " A: " + A);
        int w = (int) (0.7 * root.getWidth());
        assertEquals(A.getWidth(), w);
        assertEquals(A.getHeight(), w);
        assertEquals(A.getLeft(), (root.getWidth() - w) / 2);
        assertEquals(A.getTop(), (root.getHeight() - w) / 2);

        root.setOptimizationLevel(Optimizer.OPTIMIZATION_STANDARD);
        root.layout();
        System.out.println("b) root: " + root + " A: " + A);
        assertEquals(A.getWidth(), w);
        assertEquals(A.getHeight(), w);
        assertEquals(A.getLeft(), (root.getWidth() - w) / 2);
        assertEquals(A.getTop(), (root.getHeight() - w) / 2);
    }

    @Test
    public void testRatio() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 600);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        A.setDebugName("A");
        root.add(A);
        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        A.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        A.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        A.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        A.setDimensionRatio("16:9");
        root.layout();
        System.out.println("a) root: " + root + " A: " + A);
        assertEquals(A.getWidth(), 1067);
        assertEquals(A.getHeight(), 600);
    }

    @Test
    public void testDanglingRatio() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 600);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        A.setDebugName("A");
        root.add(A);
        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        A.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        A.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        A.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        A.setDimensionRatio("1:1");
//        root.layout();
        System.out.println("a) root: " + root + " A: " + A);
//        assertEquals(A.getWidth(), 1000);
//        assertEquals(A.getHeight(), 1000);
        A.setWidth(100);
        A.setHeight(20);
        A.setDimensionRatio("W,1:1");
        root.layout();
        System.out.println("b) root: " + root + " A: " + A);
        assertEquals(A.getWidth(), 1000);
        assertEquals(A.getHeight(), 1000);
    }

    @Test
    public void testDanglingRatio2() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 600);
        ConstraintWidget A = new ConstraintWidget(300, 200);
        ConstraintWidget B = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        A.setDebugName("A");
        root.add(A);
        B.setDebugName("B");
        root.add(B);
        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 20);
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 100);
        B.connect(ConstraintAnchor.Type.TOP, A, ConstraintAnchor.Type.TOP);
        B.connect(ConstraintAnchor.Type.BOTTOM, A, ConstraintAnchor.Type.BOTTOM);
        B.connect(ConstraintAnchor.Type.LEFT, A, ConstraintAnchor.Type.RIGHT, 15);
        B.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        B.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        B.setDimensionRatio("1:1");
        root.layout();
        System.out.println("a) root: " + root + " A: " + A + " B: " + B);
        assertEquals(B.getLeft(), 335);
        assertEquals(B.getTop(), 100);
        assertEquals(B.getWidth(), 200);
        assertEquals(B.getHeight(), 200);
    }

    @Test
    public void testDanglingRatio3() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 600);
        ConstraintWidget A = new ConstraintWidget(300, 200);
        ConstraintWidget B = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        A.setDebugName("A");
        root.add(A);
        B.setDebugName("B");
        root.add(B);
        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 20);
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 100);
        A.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        A.setDimensionRatio("h,1:1");
        B.connect(ConstraintAnchor.Type.TOP, A, ConstraintAnchor.Type.TOP);
        B.connect(ConstraintAnchor.Type.BOTTOM, A, ConstraintAnchor.Type.BOTTOM);
        B.connect(ConstraintAnchor.Type.LEFT, A, ConstraintAnchor.Type.RIGHT, 15);
        B.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        B.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        B.setDimensionRatio("w,1:1");
        root.layout();
        System.out.println("a) root: " + root + " A: " + A + " B: " + B);
        assertEquals(A.getLeft(), 20);
        assertEquals(A.getTop(), 100);
        assertEquals(A.getWidth(), 300);
        assertEquals(A.getHeight(), 300);
        assertEquals(B.getLeft(), 335);
        assertEquals(B.getTop(), 100);
        assertEquals(B.getWidth(), 300);
        assertEquals(B.getHeight(), 300);
    }

    @Test
    public void testChainRatio() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 600);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        ConstraintWidget B = new ConstraintWidget(300, 20);
        ConstraintWidget C = new ConstraintWidget(300, 20);
        root.setDebugName("root");
        A.setDebugName("A");
        B.setDebugName("B");
        C.setDebugName("C");
        root.add(A);
        root.add(B);
        root.add(C);
        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        A.connect(ConstraintAnchor.Type.RIGHT, B, ConstraintAnchor.Type.LEFT);
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        A.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        B.connect(ConstraintAnchor.Type.LEFT, A, ConstraintAnchor.Type.RIGHT);
        B.connect(ConstraintAnchor.Type.RIGHT, C, ConstraintAnchor.Type.LEFT);
        C.connect(ConstraintAnchor.Type.LEFT, B, ConstraintAnchor.Type.RIGHT);
        C.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        A.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        A.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        A.setDimensionRatio("1:1");
        root.layout();
        System.out.println("a) root: " + root + " A: " + A + " B: " + B + " C: " + C);
        assertEquals(A.getLeft(), 0);
        assertEquals(A.getTop(), 100);
        assertEquals(A.getWidth(), 400);
        assertEquals(A.getHeight(), 400);

        assertEquals(B.getLeft(), 400);
        assertEquals(B.getTop(), 0);
        assertEquals(B.getWidth(), 300);
        assertEquals(B.getHeight(), 20);

        assertEquals(C.getLeft(), 700);
        assertEquals(C.getTop(), 0);
        assertEquals(C.getWidth(), 300);
        assertEquals(C.getHeight(), 20);
    }

    @Test
    public void testChainRatio2() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 1000);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        ConstraintWidget B = new ConstraintWidget(100, 20);
        ConstraintWidget C = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        A.setDebugName("A");
        B.setDebugName("B");
        C.setDebugName("C");
        root.add(A);
        root.add(B);
        root.add(C);
        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        A.connect(ConstraintAnchor.Type.RIGHT, B, ConstraintAnchor.Type.LEFT);
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        A.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        B.connect(ConstraintAnchor.Type.LEFT, A, ConstraintAnchor.Type.RIGHT);
        B.connect(ConstraintAnchor.Type.RIGHT, C, ConstraintAnchor.Type.LEFT);
        C.connect(ConstraintAnchor.Type.LEFT, B, ConstraintAnchor.Type.RIGHT);
        C.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        A.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        A.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        A.setDimensionRatio("1:1");
        root.layout();
        System.out.println("a) root: " + root + " A: " + A + " B: " + B + " C: " + C);
        assertEquals(A.getLeft(), 0);
        assertEquals(A.getTop(), 300);
        assertEquals(A.getWidth(), 400);
        assertEquals(A.getHeight(), 400);

        assertEquals(B.getLeft(), 400);
        assertEquals(B.getTop(), 0);
        assertEquals(B.getWidth(), 100);
        assertEquals(B.getHeight(), 20);

        assertEquals(C.getLeft(), 500);
        assertEquals(C.getTop(), 0);
        assertEquals(C.getWidth(), 100);
        assertEquals(C.getHeight(), 20);
    }


    @Test
    public void testChainRatio3() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 1000);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        ConstraintWidget B = new ConstraintWidget(100, 20);
        ConstraintWidget C = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        A.setDebugName("A");
        B.setDebugName("B");
        C.setDebugName("C");
        root.add(A);
        root.add(B);
        root.add(C);
        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        A.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        A.connect(ConstraintAnchor.Type.BOTTOM, B, ConstraintAnchor.Type.TOP);
        B.connect(ConstraintAnchor.Type.TOP, A, ConstraintAnchor.Type.BOTTOM);
        B.connect(ConstraintAnchor.Type.BOTTOM, C, ConstraintAnchor.Type.TOP);
        C.connect(ConstraintAnchor.Type.TOP, B, ConstraintAnchor.Type.BOTTOM);
        C.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        A.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        A.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        A.setDimensionRatio("1:1");
        root.layout();
        System.out.println("a) root: " + root + " A: " + A + " B: " + B + " C: " + C);
        assertEquals(A.getLeft(), 0);
        assertEquals(A.getTop(), 90);
        assertEquals(A.getWidth(), 600);
        assertEquals(A.getHeight(), 600);

        assertEquals(B.getLeft(), 0);
        assertEquals(B.getTop(), 780);
        assertEquals(B.getWidth(), 100);
        assertEquals(B.getHeight(), 20);

        assertEquals(C.getLeft(), 0);
        assertEquals(C.getTop(), 890);
        assertEquals(C.getWidth(), 100);
        assertEquals(C.getHeight(), 20);
    }

    @Test
    public void testChainRatio4() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 600);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        ConstraintWidget B = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        A.setDebugName("A");
        B.setDebugName("B");
        root.add(A);
        root.add(B);
        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        A.connect(ConstraintAnchor.Type.RIGHT, B, ConstraintAnchor.Type.LEFT);
        A.connect(ConstraintAnchor.Type.TOP, B, ConstraintAnchor.Type.TOP);
        A.connect(ConstraintAnchor.Type.BOTTOM, B, ConstraintAnchor.Type.BOTTOM);
        B.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        B.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        B.connect(ConstraintAnchor.Type.LEFT, A, ConstraintAnchor.Type.RIGHT);
        B.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        A.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        A.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        B.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        B.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        B.setDimensionRatio("4:3");
        root.layout();
        System.out.println("a) root: " + root + " A: " + A + " B: " + B);
        assertEquals(A.getLeft(), 0);
        assertEquals(A.getTop(), 113, 1);
        assertEquals(A.getWidth(), 500);
        assertEquals(A.getHeight(), 375);

        assertEquals(B.getLeft(), 500);
        assertEquals(B.getTop(), 113, 1);
        assertEquals(B.getWidth(), 500);
        assertEquals(B.getHeight(), 375);
    }
}
