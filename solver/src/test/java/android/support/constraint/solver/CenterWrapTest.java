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

import java.util.ArrayList;

import static org.testng.Assert.assertEquals;

public class AdvancedChainTest {

    @Test
    public void testSimpleHorizontalChainPacked() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        ConstraintWidget B = new ConstraintWidget(100, 20);

        root.setDebugSolverName(root.getSystem(), "root");
        A.setDebugSolverName(root.getSystem(), "A");
        B.setDebugSolverName(root.getSystem(), "B");
        ArrayList<ConstraintWidget> widgets = new ArrayList<>();
        widgets.add(A);
        widgets.add(B);
        widgets.add(root);
        root.add(A);
        root.add(B);
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 20);
        B.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 20);

        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 0);
        A.connect(ConstraintAnchor.Type.RIGHT, B, ConstraintAnchor.Type.LEFT, 0);
        B.connect(ConstraintAnchor.Type.LEFT, A, ConstraintAnchor.Type.RIGHT, 0);
        B.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 0);
        A.setHorizontalChainStyle(ConstraintWidget.CHAIN_PACKED);
        root.layout();
        System.out.println("a) A: " + A + " B: " + B );
        assertEquals(A.getLeft() - root.getLeft(), root.getRight() - B.getRight(), 1);
        assertEquals(B.getLeft() - A.getRight(), 0, 1);
    }

    @Test
    public void testSimpleVerticalTChainPacked() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        ConstraintWidget B = new ConstraintWidget(100, 20);

        root.setDebugSolverName(root.getSystem(), "root");
        A.setDebugSolverName(root.getSystem(), "A");
        B.setDebugSolverName(root.getSystem(), "B");
        ArrayList<ConstraintWidget> widgets = new ArrayList<>();
        widgets.add(A);
        widgets.add(B);
        widgets.add(root);
        root.add(A);
        root.add(B);
        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 20);
        B.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 20);

        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 0);
        A.connect(ConstraintAnchor.Type.BOTTOM, B, ConstraintAnchor.Type.TOP, 0);
        B.connect(ConstraintAnchor.Type.TOP, A, ConstraintAnchor.Type.BOTTOM, 0);
        B.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, 0);
        A.setVerticalChainStyle(ConstraintWidget.CHAIN_PACKED);
        root.layout();
        System.out.println("a) A: " + A + " B: " + B );
        assertEquals(A.getTop() - root.getTop(), root.getBottom() - B.getBottom(), 1);
        assertEquals(B.getTop() - A.getBottom(), 0, 1);
    }

    @Test
    public void testHorizontalChainStyles() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        ConstraintWidget B = new ConstraintWidget(100, 20);
        ConstraintWidget C = new ConstraintWidget(100, 20);
        root.add(A);
        root.add(B);
        root.add(C);
        root.setDebugSolverName(root.getSystem(), "root");
        A.setDebugSolverName(root.getSystem(), "A");
        B.setDebugSolverName(root.getSystem(), "B");
        C.setDebugSolverName(root.getSystem(), "C");
        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 0);
        A.connect(ConstraintAnchor.Type.RIGHT, B, ConstraintAnchor.Type.LEFT, 0);
        B.connect(ConstraintAnchor.Type.LEFT, A, ConstraintAnchor.Type.RIGHT, 0);
        B.connect(ConstraintAnchor.Type.RIGHT, C, ConstraintAnchor.Type.LEFT, 0);
        C.connect(ConstraintAnchor.Type.LEFT, B, ConstraintAnchor.Type.RIGHT, 0);
        C.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 0);
        root.layout();
        System.out.println("       spread) root: " + root + " A: " + A + " B: " + B + " C: " + C);
        int gap = (root.getWidth() - A.getWidth() - B.getWidth() - C.getWidth()) / 4;
        int size = 100;
        assertEquals(A.getWidth(), size);
        assertEquals(B.getWidth(), size);
        assertEquals(C.getWidth(), size);
        assertEquals(gap, A.getLeft());
        assertEquals(A.getRight() + gap, B.getLeft());
        assertEquals(root.getWidth() - gap - C.getWidth(), C.getLeft());
        A.setHorizontalChainStyle(ConstraintWidget.CHAIN_SPREAD_INSIDE);
        root.layout();
        System.out.println("spread inside) root: " + root + " A: " + A + " B: " + B + " C: " + C);
        gap = (root.getWidth() - A.getWidth() - B.getWidth() - C.getWidth()) / 2;
        assertEquals(A.getWidth(), size);
        assertEquals(B.getWidth(), size);
        assertEquals(C.getWidth(), size);
        assertEquals(A.getLeft(), 0);
        assertEquals(A.getRight() + gap, B.getLeft());
        assertEquals(root.getWidth(), C.getRight());
        A.setHorizontalChainStyle(ConstraintWidget.CHAIN_PACKED);
        root.layout();
        System.out.println("       packed) root: " + root + " A: " + A + " B: " + B + " C: " + C);
        assertEquals(A.getWidth(), size);
        assertEquals(B.getWidth(), size);
        assertEquals(C.getWidth(), size);
        assertEquals(A.getLeft(), gap);
        assertEquals(root.getWidth() - gap, C.getRight());
    }

    @Test
    public void testVerticalChainStyles() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        ConstraintWidget B = new ConstraintWidget(100, 20);
        ConstraintWidget C = new ConstraintWidget(100, 20);
        root.add(A);
        root.add(B);
        root.add(C);
        root.setDebugSolverName(root.getSystem(), "root");
        A.setDebugSolverName(root.getSystem(), "A");
        B.setDebugSolverName(root.getSystem(), "B");
        C.setDebugSolverName(root.getSystem(), "C");
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 0);
        A.connect(ConstraintAnchor.Type.BOTTOM, B, ConstraintAnchor.Type.TOP, 0);
        B.connect(ConstraintAnchor.Type.TOP, A, ConstraintAnchor.Type.BOTTOM, 0);
        B.connect(ConstraintAnchor.Type.BOTTOM, C, ConstraintAnchor.Type.TOP, 0);
        C.connect(ConstraintAnchor.Type.TOP, B, ConstraintAnchor.Type.BOTTOM, 0);
        C.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, 0);
        root.layout();
        System.out.println("       spread) root: " + root + " A: " + A + " B: " + B + " C: " + C);
        int gap = (root.getHeight() - A.getHeight() - B.getHeight() - C.getHeight()) / 4;
        int size = 20;
        assertEquals(A.getHeight(), size);
        assertEquals(B.getHeight(), size);
        assertEquals(C.getHeight(), size);
        assertEquals(gap, A.getTop());
        assertEquals(A.getBottom() + gap, B.getTop());
        assertEquals(root.getHeight() - gap - C.getHeight(), C.getTop());
        A.setVerticalChainStyle(ConstraintWidget.CHAIN_SPREAD_INSIDE);
        root.layout();
        System.out.println("spread inside) root: " + root + " A: " + A + " B: " + B + " C: " + C);
        gap = (root.getHeight() - A.getHeight() - B.getHeight() - C.getHeight()) / 2;
        assertEquals(A.getHeight(), size);
        assertEquals(B.getHeight(), size);
        assertEquals(C.getHeight(), size);
        assertEquals(A.getTop(), 0);
        assertEquals(A.getBottom() + gap, B.getTop());
        assertEquals(root.getHeight(), C.getBottom());
        A.setVerticalChainStyle(ConstraintWidget.CHAIN_PACKED);
        root.layout();
        System.out.println("       packed) root: " + root + " A: " + A + " B: " + B + " C: " + C);
        assertEquals(A.getHeight(), size);
        assertEquals(B.getHeight(), size);
        assertEquals(C.getHeight(), size);
        assertEquals(A.getTop(), gap);
        assertEquals(root.getHeight() - gap, C.getBottom());    }
}
