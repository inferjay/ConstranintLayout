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
package android.support.constraint.solver;

import android.support.constraint.solver.widgets.ConstraintAnchor;
import android.support.constraint.solver.widgets.ConstraintWidget;
import android.support.constraint.solver.widgets.ConstraintWidgetContainer;
import org.testng.annotations.Test;

import java.util.ArrayList;

import static org.testng.Assert.assertEquals;

public class CenterWrapTest {

    @Test
    public void testRatioCenter() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        ConstraintWidget B = new ConstraintWidget(100, 20);
        root.setDebugName("Root");
        A.setDebugName("A");
        B.setDebugName("B");
        root.add(A);
        root.add(B);

        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        A.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        A.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        A.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        A.setDimensionRatio(0.3f, ConstraintWidget.VERTICAL);

        B.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        B.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        B.connect(ConstraintAnchor.Type.TOP, A, ConstraintAnchor.Type.BOTTOM);
        B.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        B.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        B.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        B.setDimensionRatio(1f, ConstraintWidget.VERTICAL);
//        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setOptimizationLevel(0);
        root.layout();
        System.out.println("root: " + root + " A: " + A);
    }

    @Test
    public void testSimpleWrap() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        root.setDebugName("Root");
        A.setDebugName("A");
        root.add(A);
        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        A.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        A.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setOptimizationLevel(0);
        root.layout();
        System.out.println("root: " + root + " A: " + A);
        assertEquals(A.getWidth(), 100);
        assertEquals(A.getHeight(), 20);
        assertEquals(root.getWidth(), 100);
        assertEquals(root.getHeight(), 20);
    }

    @Test
    public void testSimpleWrap2() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        root.setDebugName("Root");
        A.setDebugName("A");
        root.add(A);
        A.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setOptimizationLevel(0);
        root.layout();
        System.out.println("root: " + root + " A: " + A);
        assertEquals(A.getWidth(), 100);
        assertEquals(A.getHeight(), 20);
        assertEquals(root.getWidth(), 100);
        assertEquals(root.getHeight(), 20);
    }

    @Test
    public void testWrap() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        ConstraintWidget B = new ConstraintWidget(100, 20);
        ConstraintWidget C = new ConstraintWidget(100, 20);
        root.setDebugName("Root");
        A.setDebugName("A");
        B.setDebugName("B");
        C.setDebugName("C");
        root.add(A);
        root.add(B);
        root.add(C);

        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        A.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        A.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        B.connect(ConstraintAnchor.Type.LEFT, A, ConstraintAnchor.Type.RIGHT);
        B.connect(ConstraintAnchor.Type.TOP, A, ConstraintAnchor.Type.TOP);
        C.connect(ConstraintAnchor.Type.LEFT, B, ConstraintAnchor.Type.RIGHT);
        C.connect(ConstraintAnchor.Type.TOP, B, ConstraintAnchor.Type.BOTTOM);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setOptimizationLevel(0);
        root.layout();
        System.out.println("root: " + root + " A: " + A + " B: " + B + " C: " + C);
        assertEquals(A.getWidth(), 100);
        assertEquals(B.getWidth(), 100);
        assertEquals(C.getWidth(), 100);
        assertEquals(A.getWidth(), 100);
        assertEquals(A.getHeight(), 20);
        assertEquals(B.getHeight(), 20);
        assertEquals(C.getHeight(), 20);
    }

    @Test
    public void testWrapHeight() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget TL = new ConstraintWidget(100, 20);
        ConstraintWidget TRL = new ConstraintWidget(100, 20);
        ConstraintWidget TBL = new ConstraintWidget(100, 20);
        ConstraintWidget IMG = new ConstraintWidget(100, 100);

        root.setDebugName("root");
        TL.setDebugName("TL");
        TRL.setDebugName("TRL");
        TBL.setDebugName("TBL");
        IMG.setDebugName("IMG");

        // vertical

        TL.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        TL.connect(ConstraintAnchor.Type.BOTTOM, TBL, ConstraintAnchor.Type.BOTTOM);
        TRL.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        //TRL.connect(ConstraintAnchor.Type.BOTTOM, TBL, ConstraintAnchor.Type.TOP);
        TBL.connect(ConstraintAnchor.Type.TOP, TRL, ConstraintAnchor.Type.BOTTOM);

        IMG.connect(ConstraintAnchor.Type.TOP, TBL, ConstraintAnchor.Type.BOTTOM);
        IMG.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);

        root.add(TL);
        root.add(TRL);
        root.add(TBL);
        root.add(IMG);

        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();
        System.out.println("a) root: " + root + " TL: " + TL + " TRL: " + TRL + " TBL: " + TBL + " IMG: " + IMG);
        assertEquals(root.getHeight(), 140);
    }

    @Test
    public void testComplexLayout() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget IMG = new ConstraintWidget(100, 100);

        int margin = 16;

        ConstraintWidget BUTTON = new ConstraintWidget(50, 50);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        ConstraintWidget B = new ConstraintWidget(100, 20);
        ConstraintWidget C = new ConstraintWidget(100, 20);

        IMG.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        IMG.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        IMG.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        IMG.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);

        BUTTON.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, margin);
        BUTTON.connect(ConstraintAnchor.Type.TOP, IMG, ConstraintAnchor.Type.BOTTOM);
        BUTTON.connect(ConstraintAnchor.Type.BOTTOM, IMG, ConstraintAnchor.Type.BOTTOM);

        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, margin);
        A.connect(ConstraintAnchor.Type.TOP, BUTTON, ConstraintAnchor.Type.BOTTOM, margin);

        B.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, margin);
        B.connect(ConstraintAnchor.Type.TOP, BUTTON, ConstraintAnchor.Type.BOTTOM, margin);

        C.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, margin);
        C.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, margin);
        C.connect(ConstraintAnchor.Type.TOP, A, ConstraintAnchor.Type.BOTTOM, margin);
        C.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);

        root.add(IMG);
        root.add(BUTTON);
        root.add(A);
        root.add(B);
        root.add(C);

        root.setDebugName("root");
        IMG.setDebugName("IMG");
        BUTTON.setDebugName("BUTTON");
        A.setDebugName("A");
        B.setDebugName("B");
        C.setDebugName("C");

        root.layout();
        System.out.println("a) root: " + root + " IMG: " + IMG + " BUTTON: " + BUTTON + " A: " + A + " B: " + B + " C: " + C);
        assertEquals(root.getWidth(), 800);
        assertEquals(root.getHeight(), 600);
        assertEquals(IMG.getWidth(), root.getWidth());
        assertEquals(BUTTON.getWidth(), 50);
        assertEquals(A.getWidth(), 100);
        assertEquals(B.getWidth(), 100);
        assertEquals(C.getWidth(), 100);
        assertEquals(IMG.getHeight(), 100);
        assertEquals(BUTTON.getHeight(), 50);
        assertEquals(A.getHeight(), 20);
        assertEquals(B.getHeight(), 20);
        assertEquals(C.getHeight(), 20);
        assertEquals(IMG.getLeft(), 0);
        assertEquals(IMG.getRight(), root.getRight());
        assertEquals(BUTTON.getLeft(), 734);
        assertEquals(BUTTON.getTop(), IMG.getBottom() - BUTTON.getHeight() / 2);
        assertEquals(A.getLeft(), margin);
        assertEquals(A.getTop(), BUTTON.getBottom() + margin);
        assertEquals(B.getRight(), root.getRight() - margin);
        assertEquals(B.getTop(), A.getTop());
        assertEquals(C.getLeft(), 350);
        assertEquals(C.getRight(), 450);
        assertEquals(C.getTop(), 379, 1);

        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();
        root.setOptimizationLevel(0);
        System.out.println("b) root: " + root + " IMG: " + IMG + " BUTTON: " + BUTTON + " A: " + A + " B: " + B + " C: " + C);
        assertEquals(root.getWidth(), 800);
        assertEquals(root.getHeight(), 197);
        assertEquals(IMG.getWidth(), root.getWidth());
        assertEquals(BUTTON.getWidth(), 50);
        assertEquals(A.getWidth(), 100);
        assertEquals(B.getWidth(), 100);
        assertEquals(C.getWidth(), 100);
        assertEquals(IMG.getHeight(), 100);
        assertEquals(BUTTON.getHeight(), 50);
        assertEquals(A.getHeight(), 20);
        assertEquals(B.getHeight(), 20);
        assertEquals(C.getHeight(), 20);
        assertEquals(IMG.getLeft(), 0);
        assertEquals(IMG.getRight(), root.getRight());
        assertEquals(BUTTON.getLeft(), 734);
        assertEquals(BUTTON.getTop(), IMG.getBottom() - BUTTON.getHeight() / 2);
        assertEquals(A.getLeft(), margin);
        assertEquals(A.getTop(), BUTTON.getBottom() + margin);
        assertEquals(B.getRight(), root.getRight() - margin);
        assertEquals(B.getTop(), A.getTop());
        assertEquals(C.getLeft(), 350);
        assertEquals(C.getRight(), 450);
        assertEquals(C.getTop(), A.getBottom() + margin);
    }
}
