/*
 * Copyright (C) 2015 The Android Open Source Project
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
import android.support.constraint.solver.widgets.Guideline;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class OptimizationsTest {

    @Test
    public void testGuideline() {
        testVerticalGuideline(ConstraintWidgetContainer.OPTIMIZATION_NONE);
        testVerticalGuideline(ConstraintWidgetContainer.OPTIMIZATION_ALL);
        testHorizontalGuideline(ConstraintWidgetContainer.OPTIMIZATION_NONE);
        testHorizontalGuideline(ConstraintWidgetContainer.OPTIMIZATION_ALL);
    }

    public void testVerticalGuideline(int directResolution) {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        root.setOptimizationLevel(directResolution);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        Guideline guideline = new Guideline();
        guideline.setOrientation(Guideline.VERTICAL);
        root.setDebugName("root");
        A.setDebugName("A");
        guideline.setDebugName("guideline");
        root.add(A);
        root.add(guideline);
        A.connect(ConstraintAnchor.Type.LEFT, guideline, ConstraintAnchor.Type.LEFT, 16);
        guideline.setGuideBegin(100);
        root.layout();
        System.out.println("res: " + directResolution + " root: " + root + " A: " + A + " guideline: " + guideline);
        assertEquals(guideline.getLeft(), 100);
        assertEquals(A.getLeft(), 116);
        assertEquals(A.getWidth(), 100);
        assertEquals(A.getHeight(), 20);
        assertEquals(A.getTop(), 0);
        guideline.setGuidePercent(0.5f);
        root.layout();
        System.out.println("res: " + directResolution + " root: " + root + " A: " + A + " guideline: " + guideline);
        assertEquals(guideline.getLeft(), root.getWidth() / 2);
        assertEquals(A.getLeft(), 316);
        assertEquals(A.getWidth(), 100);
        assertEquals(A.getHeight(), 20);
        assertEquals(A.getTop(), 0);
        guideline.setGuideEnd(100);
        root.layout();
        System.out.println("res: " + directResolution + " root: " + root + " A: " + A + " guideline: " + guideline);
        assertEquals(guideline.getLeft(), 500);
        assertEquals(A.getLeft(), 516);
        assertEquals(A.getWidth(), 100);
        assertEquals(A.getHeight(), 20);
        assertEquals(A.getTop(), 0);
    }

    public void testHorizontalGuideline(int directResolution) {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        root.setOptimizationLevel(directResolution);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        Guideline guideline = new Guideline();
        guideline.setOrientation(Guideline.HORIZONTAL);
        root.setDebugName("root");
        A.setDebugName("A");
        guideline.setDebugName("guideline");
        root.add(A);
        root.add(guideline);
        A.connect(ConstraintAnchor.Type.TOP, guideline, ConstraintAnchor.Type.TOP, 16);
        guideline.setGuideBegin(100);
        root.layout();
        System.out.println("res: " + directResolution + " root: " + root + " A: " + A + " guideline: " + guideline);
        assertEquals(guideline.getTop(), 100);
        assertEquals(A.getTop(), 116);
        assertEquals(A.getWidth(), 100);
        assertEquals(A.getHeight(), 20);
        assertEquals(A.getLeft(), 0);
        guideline.setGuidePercent(0.5f);
        root.layout();
        System.out.println("res: " + directResolution + " root: " + root + " A: " + A + " guideline: " + guideline);
        assertEquals(guideline.getTop(), root.getHeight() / 2);
        assertEquals(A.getTop(), 316);
        assertEquals(A.getWidth(), 100);
        assertEquals(A.getHeight(), 20);
        assertEquals(A.getLeft(), 0);
        guideline.setGuideEnd(100);
        root.layout();
        System.out.println("res: " + directResolution + " root: " + root + " A: " + A + " guideline: " + guideline);
        assertEquals(guideline.getTop(), 500);
        assertEquals(A.getTop(), 516);
        assertEquals(A.getWidth(), 100);
        assertEquals(A.getHeight(), 20);
        assertEquals(A.getLeft(), 0);
    }

    @Test
    public void testDependency() {
        testDependency(ConstraintWidgetContainer.OPTIMIZATION_NONE);
        testDependency(ConstraintWidgetContainer.OPTIMIZATION_ALL);
    }

    public void testDependency(int directResolution) {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        root.setOptimizationLevel(directResolution);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        ConstraintWidget B = new ConstraintWidget(100, 20);
        ConstraintWidget C = new ConstraintWidget(100, 20);
        A.setDebugName("A");
        B.setDebugName("B");
        C.setDebugName("C");
        root.add(A);
        root.add(B);
        root.add(C);
        A.setBaselineDistance(8);
        B.setBaselineDistance(8);
        C.setBaselineDistance(8);
        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 10);
        A.connect(ConstraintAnchor.Type.BASELINE, B, ConstraintAnchor.Type.BASELINE);
        B.connect(ConstraintAnchor.Type.LEFT, A, ConstraintAnchor.Type.RIGHT, 16);
        B.connect(ConstraintAnchor.Type.BASELINE, C, ConstraintAnchor.Type.BASELINE);
        C.connect(ConstraintAnchor.Type.LEFT, B, ConstraintAnchor.Type.RIGHT, 48);
        C.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 32);
        root.layout();
        System.out.println("res: " + directResolution + " root: " + root
            + " A: " + A + " B: " + B + " C: " + C);
        assertEquals(A.getLeft(), 10);
        assertEquals(A.getTop(), 32);
        assertEquals(B.getLeft(), 126);
        assertEquals(B.getTop(), 32);
        assertEquals(C.getLeft(), 274);
        assertEquals(C.getTop(), 32);
    }

    @Test
    public void testDependency2() {
        testDependency2(ConstraintWidgetContainer.OPTIMIZATION_NONE);
        testDependency2(ConstraintWidgetContainer.OPTIMIZATION_ALL);
    }

    public void testDependency2(int directResolution) {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        root.setOptimizationLevel(directResolution);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        ConstraintWidget B = new ConstraintWidget(100, 20);
        ConstraintWidget C = new ConstraintWidget(100, 20);
        A.setDebugName("A");
        B.setDebugName("B");
        C.setDebugName("C");
        root.add(A);
        root.add(B);
        root.add(C);
        A.setBaselineDistance(8);
        B.setBaselineDistance(8);
        C.setBaselineDistance(8);
        A.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        A.connect(ConstraintAnchor.Type.LEFT, B, ConstraintAnchor.Type.LEFT);
        B.connect(ConstraintAnchor.Type.BOTTOM, A, ConstraintAnchor.Type.TOP);
        B.connect(ConstraintAnchor.Type.LEFT, C, ConstraintAnchor.Type.LEFT);
        C.connect(ConstraintAnchor.Type.BOTTOM, B, ConstraintAnchor.Type.TOP);
        C.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 12);
        root.layout();
        System.out.println("res: " + directResolution + " root: " + root
                + " A: " + A + " B: " + B + " C: " + C);
        assertEquals(A.getLeft(), 12);
        assertEquals(A.getTop(), 580);
        assertEquals(B.getLeft(), 12);
        assertEquals(B.getTop(), 560);
        assertEquals(C.getLeft(), 12);
        assertEquals(C.getTop(), 540);
    }

    @Test
    public void testUnconstrainedDependency() {
        testUnconstrainedDependency(ConstraintWidgetContainer.OPTIMIZATION_NONE);
        testUnconstrainedDependency(ConstraintWidgetContainer.OPTIMIZATION_ALL);
    }

    public void testUnconstrainedDependency(int directResolution) {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        root.setOptimizationLevel(directResolution);
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
        A.setBaselineDistance(8);
        B.setBaselineDistance(8);
        C.setBaselineDistance(8);
        A.setFrame(142, 96, 242, 130);
        B.connect(ConstraintAnchor.Type.LEFT, A, ConstraintAnchor.Type.RIGHT, 10);
        B.connect(ConstraintAnchor.Type.TOP, A, ConstraintAnchor.Type.TOP, 100);
        C.connect(ConstraintAnchor.Type.RIGHT, A, ConstraintAnchor.Type.LEFT);
        C.connect(ConstraintAnchor.Type.BASELINE, A, ConstraintAnchor.Type.BASELINE);
        root.layout();
        System.out.println("res: " + directResolution + " root: " + root
                + " A: " + A + " B: " + B + " C: " + C);
        assertEquals(A.getLeft(), 142);
        assertEquals(A.getTop(), 96);
        assertEquals(A.getWidth(), 100);
        assertEquals(A.getHeight(), 34);
        assertEquals(B.getLeft(), 252);
        assertEquals(B.getTop(), 196);
        assertEquals(C.getLeft(), 42);
        assertEquals(C.getTop(), 96);
    }

    @Test
    public void testFullLayout() {
        testFullLayout(ConstraintWidgetContainer.OPTIMIZATION_NONE);
        testFullLayout(ConstraintWidgetContainer.OPTIMIZATION_ALL);
    }

    public void testFullLayout(int directResolution) {
        // Horizontal :
        // r <- A
        // r <- B <- C <- D
        //      B <- E
        // r <- F
        // r <- G
        // Vertical:
        // r <- A <- B <- C <- D <- E
        // r <- F <- G
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        root.setOptimizationLevel(directResolution);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        ConstraintWidget B = new ConstraintWidget(100, 20);
        ConstraintWidget C = new ConstraintWidget(100, 20);
        ConstraintWidget D = new ConstraintWidget(100, 20);
        ConstraintWidget E = new ConstraintWidget(100, 20);
        ConstraintWidget F = new ConstraintWidget(100, 20);
        ConstraintWidget G = new ConstraintWidget(100, 20);
        A.setDebugName("A");
        B.setDebugName("B");
        C.setDebugName("C");
        D.setDebugName("D");
        E.setDebugName("E");
        F.setDebugName("F");
        G.setDebugName("G");
        root.add(G);
        root.add(A);
        root.add(B);
        root.add(E);
        root.add(C);
        root.add(D);
        root.add(F);
        A.setBaselineDistance(8);
        B.setBaselineDistance(8);
        C.setBaselineDistance(8);
        D.setBaselineDistance(8);
        E.setBaselineDistance(8);
        F.setBaselineDistance(8);
        G.setBaselineDistance(8);
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 20);
        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        A.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        B.connect(ConstraintAnchor.Type.TOP, A, ConstraintAnchor.Type.BOTTOM, 40);
        B.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 16);
        C.connect(ConstraintAnchor.Type.LEFT, B, ConstraintAnchor.Type.RIGHT, 16);
        C.connect(ConstraintAnchor.Type.BASELINE, B, ConstraintAnchor.Type.BASELINE);
        D.connect(ConstraintAnchor.Type.TOP, C, ConstraintAnchor.Type.BOTTOM);
        D.connect(ConstraintAnchor.Type.LEFT, C, ConstraintAnchor.Type.LEFT);
        E.connect(ConstraintAnchor.Type.RIGHT, B, ConstraintAnchor.Type.RIGHT);
        E.connect(ConstraintAnchor.Type.BASELINE, D, ConstraintAnchor.Type.BASELINE);
        F.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        F.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        G.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 16);
        G.connect(ConstraintAnchor.Type.BASELINE, F, ConstraintAnchor.Type.BASELINE);
        root.layout();

        System.out.println(" direct: " + directResolution + " -> A: " + A + " B: " + B + " C: " + C + " D: " + D + " E: " + E + " F: " + F + " G: " + G);
        assertEquals(A.getLeft(), 250);
        assertEquals(A.getTop(), 20);
        assertEquals(B.getLeft(), 16);
        assertEquals(B.getTop(), 80);
        assertEquals(C.getLeft(), 132);
        assertEquals(C.getTop(), 80);
        assertEquals(D.getLeft(), 132);
        assertEquals(D.getTop(), 100);
        assertEquals(E.getLeft(), 16);
        assertEquals(E.getTop(), 100);
        assertEquals(F.getLeft(), 500);
        assertEquals(F.getTop(), 580);
        assertEquals(G.getLeft(), 16);
        assertEquals(G.getTop(), 580);
    }
}
