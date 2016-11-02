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

public class ChainWrapContentTest {

    @Test
    public void testVertWrapContentChain() {
        testVertWrapContentChain(false);
        testVertWrapContentChain(true);
    }

    public void testVertWrapContentChain(boolean directResolution) {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        root.setDirectResolution(directResolution);
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
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        B.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 10);
        A.connect(ConstraintAnchor.Type.BOTTOM, B, ConstraintAnchor.Type.TOP);
        B.connect(ConstraintAnchor.Type.TOP, A, ConstraintAnchor.Type.BOTTOM);
        B.connect(ConstraintAnchor.Type.BOTTOM, C, ConstraintAnchor.Type.TOP);
        C.connect(ConstraintAnchor.Type.TOP, B, ConstraintAnchor.Type.BOTTOM);
        C.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, 32);
        root.layout();
        System.out.println("res: " + directResolution + " root: " + root
                + " A: " + A + " B: " + B + " C: " + C);
        assertEquals(A.getTop(), 10);
        assertEquals(B.getTop(), 30);
        assertEquals(C.getTop(), 30);
        assertEquals(root.getHeight(), 82);
    }

    @Test
    public void testHorizWrapContentChain() {
        testHorizWrapContentChain(false);
        testHorizWrapContentChain(true);
    }

    public void testHorizWrapContentChain(boolean directResolution) {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        root.setDirectResolution(directResolution);
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
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        B.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 10);
        A.connect(ConstraintAnchor.Type.RIGHT, B, ConstraintAnchor.Type.LEFT);
        B.connect(ConstraintAnchor.Type.LEFT, A, ConstraintAnchor.Type.RIGHT);
        B.connect(ConstraintAnchor.Type.RIGHT, C, ConstraintAnchor.Type.LEFT);
        C.connect(ConstraintAnchor.Type.LEFT, B, ConstraintAnchor.Type.RIGHT);
        C.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 32);
        root.layout();
        System.out.println("res: " + directResolution + " root: " + root
                + " A: " + A + " B: " + B + " C: " + C);
        assertEquals(A.getLeft(), 10);
        assertEquals(B.getLeft(), 110);
        assertEquals(C.getLeft(), 110);
        assertEquals(root.getWidth(), 242);
        root.setMinWidth(400);
        root.layout();
        System.out.println("res: " + directResolution + " root: " + root
                + " A: " + A + " B: " + B + " C: " + C);
        assertEquals(A.getLeft(), 10);
        assertEquals(B.getLeft(), 110);
        assertEquals(C.getLeft(), 268);
        assertEquals(root.getWidth(), 400);
    }

    @Test
    public void testVertWrapContentChain3Elts() {
        testVertWrapContentChain3Elts(false);
        testVertWrapContentChain3Elts(true);
    }

    public void testVertWrapContentChain3Elts(boolean directResolution) {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        root.setDirectResolution(directResolution);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        ConstraintWidget B = new ConstraintWidget(100, 20);
        ConstraintWidget C = new ConstraintWidget(100, 20);
        ConstraintWidget D = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        A.setDebugName("A");
        B.setDebugName("B");
        C.setDebugName("C");
        D.setDebugName("D");
        root.add(A);
        root.add(B);
        root.add(C);
        root.add(D);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        B.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        C.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 10);
        A.connect(ConstraintAnchor.Type.BOTTOM, B, ConstraintAnchor.Type.TOP);
        B.connect(ConstraintAnchor.Type.TOP, A, ConstraintAnchor.Type.BOTTOM);
        B.connect(ConstraintAnchor.Type.BOTTOM, C, ConstraintAnchor.Type.TOP);
        C.connect(ConstraintAnchor.Type.TOP, B, ConstraintAnchor.Type.BOTTOM);
        C.connect(ConstraintAnchor.Type.BOTTOM, D, ConstraintAnchor.Type.TOP);
        D.connect(ConstraintAnchor.Type.TOP, C, ConstraintAnchor.Type.BOTTOM);
        D.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, 32);
        root.layout();
        System.out.println("res: " + directResolution + " root: " + root
                + " A: " + A + " B: " + B + " C: " + C + " D: " + D);
        assertEquals(A.getTop(), 10);
        assertEquals(B.getTop(), 30);
        assertEquals(C.getTop(), 30);
        assertEquals(D.getTop(), 30);
        assertEquals(root.getHeight(), 82);
        root.setMinHeight(300);
        root.layout();
        System.out.println("res: " + directResolution + " root: " + root
                + " A: " + A + " B: " + B + " C: " + C + " D: " + D);
        assertEquals(A.getTop(), 10);
        assertEquals(B.getTop(), 30);
        assertEquals(C.getTop(), 139);
        assertEquals(D.getTop(), 248);
        assertEquals(root.getHeight(), 300);
        root.setHeight(600);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        root.layout();
        System.out.println("res: " + directResolution + " root: " + root
                + " A: " + A + " B: " + B + " C: " + C + " D: " + D);
        assertEquals(A.getTop(), 10);
        assertEquals(B.getTop(), 30);
        assertEquals(C.getTop(), 289);
        assertEquals(D.getTop(), 548);
        assertEquals(root.getHeight(), 600);
    }

    @Test
    public void testHorizWrapContentChain3Elts() {
        testHorizWrapContentChain3Elts(false);
        testHorizWrapContentChain3Elts(true);
    }

    public void testHorizWrapContentChain3Elts(boolean directResolution) {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        root.setDirectResolution(directResolution);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        ConstraintWidget B = new ConstraintWidget(100, 20);
        ConstraintWidget C = new ConstraintWidget(100, 20);
        ConstraintWidget D = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        A.setDebugName("A");
        B.setDebugName("B");
        C.setDebugName("C");
        D.setDebugName("D");
        root.add(A);
        root.add(B);
        root.add(C);
        root.add(D);
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        B.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        C.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 10);
        A.connect(ConstraintAnchor.Type.RIGHT, B, ConstraintAnchor.Type.LEFT);
        B.connect(ConstraintAnchor.Type.LEFT, A, ConstraintAnchor.Type.RIGHT);
        B.connect(ConstraintAnchor.Type.RIGHT, C, ConstraintAnchor.Type.LEFT);
        C.connect(ConstraintAnchor.Type.LEFT, B, ConstraintAnchor.Type.RIGHT);
        C.connect(ConstraintAnchor.Type.RIGHT, D, ConstraintAnchor.Type.LEFT);
        D.connect(ConstraintAnchor.Type.LEFT, C, ConstraintAnchor.Type.RIGHT);
        D.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 32);
        root.layout();
        System.out.println("res: " + directResolution + " root: " + root
                + " A: " + A + " B: " + B + " C: " + C + " D: " + D);
        assertEquals(A.getLeft(), 10);
        assertEquals(B.getLeft(), 110);
        assertEquals(C.getLeft(), 110);
        assertEquals(D.getLeft(), 110);
        assertEquals(root.getWidth(), 242);
        root.setMinWidth(300);
        root.layout();
        System.out.println("res: " + directResolution + " root: " + root
                + " A: " + A + " B: " + B + " C: " + C + " D: " + D);
        assertEquals(A.getLeft(), 10);
        assertEquals(B.getLeft(), 110);
        assertEquals(C.getLeft(), 139);
        assertEquals(D.getLeft(), 168);
        assertEquals(root.getWidth(), 300);
        root.setWidth(600);
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        root.layout();
        System.out.println("res: " + directResolution + " root: " + root
                + " A: " + A + " B: " + B + " C: " + C + " D: " + D);
        assertEquals(A.getLeft(), 10);
        assertEquals(B.getLeft(), 110);
        assertEquals(C.getLeft(), 289);
        assertEquals(D.getLeft(), 468);
        assertEquals(root.getWidth(), 600);
    }
}
