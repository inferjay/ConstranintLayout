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

public class ChainTest {

    @Test
    public void testBasicChain() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        ConstraintWidget B = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        A.setDebugName("A");
        B.setDebugName("B");
        ArrayList<ConstraintWidget> widgets = new ArrayList<ConstraintWidget>();
        widgets.add(A);
        widgets.add(B);
        widgets.add(root);
        root.add(A);
        root.add(B);
        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        A.connect(ConstraintAnchor.Type.RIGHT, B, ConstraintAnchor.Type.LEFT);
        B.connect(ConstraintAnchor.Type.LEFT, A, ConstraintAnchor.Type.RIGHT);
        B.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        root.layout();
        System.out.println("a) A: " + A + " B: " + B);
        assertEquals(A.getWidth(), B.getWidth(), 1);
        assertEquals(A.getLeft() - root.getLeft(), root.getRight() - B.getRight(), 1);
        assertEquals(A.getLeft() - root.getLeft(), B.getLeft() - A.getRight(), 1);
        A.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        root.layout();
        System.out.println("b) A: " + A + " B: " + B);
        assertEquals(A.getWidth(), root.getWidth() - B.getWidth());
        assertEquals(B.getWidth(), 100);
        A.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        A.setWidth(100);
        B.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        root.layout();
        System.out.println("c) A: " + A + " B: " + B);
        assertEquals(B.getWidth(), root.getWidth() - A.getWidth());
        assertEquals(A.getWidth(), 100);
    }

    @Test
    public void testBasicVerticalChain() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 600, 600);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        ConstraintWidget B = new ConstraintWidget(100, 20);
        A.setDebugName("A");
        B.setDebugName("B");
        ArrayList<ConstraintWidget> widgets = new ArrayList<ConstraintWidget>();
        widgets.add(A);
        widgets.add(B);
        widgets.add(root);
        root.add(A);
        root.add(B);
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        A.connect(ConstraintAnchor.Type.BOTTOM, B, ConstraintAnchor.Type.TOP);
        B.connect(ConstraintAnchor.Type.TOP, A, ConstraintAnchor.Type.BOTTOM);
        B.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        root.layout();
        System.out.println("a) A: " + A + " B: " + B);
        assertEquals(A.getHeight(), B.getHeight(), 1);
        assertEquals(A.getTop() - root.getTop(), root.getBottom() - B.getBottom(), 1);
        assertEquals(A.getTop() - root.getTop(), B.getTop() - A.getBottom(), 1);
        A.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        root.layout();
        System.out.println("b) A: " + A + " B: " + B);
        assertEquals(A.getHeight(), root.getHeight() - B.getHeight());
        assertEquals(B.getHeight(), 20);
        A.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        A.setHeight(20);
        B.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        root.layout();
        System.out.println("c) A: " + A + " B: " + B);
        assertEquals(B.getHeight(), root.getHeight() - A.getHeight());
        assertEquals(A.getHeight(), 20);
    }

    @Test
    public void testBasicChainThreeElements() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        ConstraintWidget B = new ConstraintWidget(100, 20);
        ConstraintWidget C = new ConstraintWidget(100, 20);
        int marginL = 7;
        int marginR = 27;
        root.setDebugSolverName(root.getSystem(), "root");
        A.setDebugSolverName(root.getSystem(), "A");
        B.setDebugSolverName(root.getSystem(), "B");
        C.setDebugSolverName(root.getSystem(), "C");
        ArrayList<ConstraintWidget> widgets = new ArrayList<ConstraintWidget>();
        widgets.add(A);
        widgets.add(B);
        widgets.add(C);
        widgets.add(root);
        root.add(A);
        root.add(B);
        root.add(C);
        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 0);
        A.connect(ConstraintAnchor.Type.RIGHT, B, ConstraintAnchor.Type.LEFT, 0);
        B.connect(ConstraintAnchor.Type.LEFT, A, ConstraintAnchor.Type.RIGHT, 0);
        B.connect(ConstraintAnchor.Type.RIGHT, C, ConstraintAnchor.Type.LEFT, 0);
        C.connect(ConstraintAnchor.Type.LEFT, B, ConstraintAnchor.Type.RIGHT, 0);
        C.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 0);
        root.layout();
        System.out.println("a) A: " + A + " B: " + B + " C: " + C);
        // all elements spread equally
        assertEquals(A.getWidth(), B.getWidth(), 1);
        assertEquals(B.getWidth(), C.getWidth(), 1);
        assertEquals(A.getLeft() - root.getLeft(), root.getRight() - C.getRight(), 1);
        assertEquals(A.getLeft() - root.getLeft(), B.getLeft() - A.getRight(), 1);
        assertEquals(B.getLeft() - A.getRight(), C.getLeft() - B.getRight(), 1);
        // A marked as 0dp, B == C, A takes the rest
        A.getAnchor(ConstraintAnchor.Type.LEFT).setMargin(marginL);
        A.getAnchor(ConstraintAnchor.Type.RIGHT).setMargin(marginR);
        B.getAnchor(ConstraintAnchor.Type.LEFT).setMargin(marginL);
        B.getAnchor(ConstraintAnchor.Type.RIGHT).setMargin(marginR);
        C.getAnchor(ConstraintAnchor.Type.LEFT).setMargin(marginL);
        C.getAnchor(ConstraintAnchor.Type.RIGHT).setMargin(marginR);
        A.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        root.layout();
        System.out.println("b) A: " + A + " B: " + B + " C: " + C);
        assertEquals(A.getLeft(), root.getLeft() + marginL);
        assertEquals(C.getRight(), root.getRight() - marginR);
        assertEquals(B.getRight(), C.getLeft() - marginL - marginR);
        assertEquals(A.getWidth(), root.getWidth() - B.getWidth() - C.getWidth() - 3*marginL - 3*marginR);
        assertEquals(B.getWidth(), C.getWidth());
        assertEquals(B.getWidth(), 100);
        checkPositions(A, B, C);
        // B marked as 0dp, A == C, B takes the rest
        A.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        A.setWidth(100);
        B.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        root.layout();
        System.out.println("c) A: " + A + " B: " + B + " C: " + C);
        assertEquals(B.getWidth(), root.getWidth() - A.getWidth() - C.getWidth() - 3*marginL - 3*marginR);
        assertEquals(A.getWidth(), C.getWidth());
        assertEquals(A.getWidth(), 100);
        checkPositions(A, B, C);
        // C marked as 0dp, A == B, C takes the rest
        B.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        B.setWidth(100);
        C.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        root.layout();
        System.out.println("d) A: " + A + " B: " + B + " C: " + C);
        assertEquals(C.getWidth(), root.getWidth() - A.getWidth() - B.getWidth() - 3*marginL - 3*marginR);
        assertEquals(A.getWidth(), B.getWidth());
        assertEquals(A.getWidth(), 100);
        checkPositions(A, B, C);
        // A & B marked as 0dp, C == 100
        C.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        C.setWidth(100);
        A.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        B.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        root.layout();
        System.out.println("e) A: " + A + " B: " + B + " C: " + C);
        assertEquals(C.getWidth(), 100);
        assertEquals(A.getWidth(), B.getWidth()); // L
        assertEquals(A.getWidth(), (root.getWidth() - C.getWidth() - 3*marginL - 3*marginR) / 2 , 1);
        checkPositions(A, B, C);
        // A & C marked as 0dp, B == 100
        C.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        B.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        B.setWidth(100);
        root.layout();
        System.out.println("f) A: " + A + " B: " + B + " C: " + C);
        assertEquals(B.getWidth(), 100);
        assertEquals(A.getWidth(), C.getWidth());
        assertEquals(A.getWidth(), (root.getWidth() - B.getWidth() - 3*marginL - 3*marginR) / 2, 1);
        checkPositions(A, B, C);
        // B & C marked as 0dp, A == 100
        B.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        A.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        A.setWidth(100);
        root.layout();
        System.out.println("g) A: " + A + " B: " + B + " C: " + C);
        assertEquals(A.getWidth(), 100);
        assertEquals(B.getWidth(), C.getWidth());
        assertEquals(B.getWidth(), (root.getWidth() - A.getWidth() - 3*marginL - 3*marginR) / 2, 1);
        checkPositions(A, B, C);
    }

    private void checkPositions(ConstraintWidget A, ConstraintWidget B, ConstraintWidget C) {
        assertEquals(A.getLeft() <= A.getRight(), true);
        assertEquals(A.getRight() <= B.getLeft(), true);
        assertEquals(B.getLeft() <= B.getRight(), true);
        assertEquals(B.getRight() <= C.getLeft(), true);
        assertEquals(C.getLeft() <= C.getRight(), true);
    }

    @Test
    public void testBasicVerticalChainThreeElements() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        ConstraintWidget B = new ConstraintWidget(100, 20);
        ConstraintWidget C = new ConstraintWidget(100, 20);
        int marginT = 7;
        int marginB = 27;
        root.setDebugSolverName(root.getSystem(), "root");
        A.setDebugSolverName(root.getSystem(), "A");
        B.setDebugSolverName(root.getSystem(), "B");
        C.setDebugSolverName(root.getSystem(), "C");
        ArrayList<ConstraintWidget> widgets = new ArrayList<ConstraintWidget>();
        widgets.add(A);
        widgets.add(B);
        widgets.add(C);
        widgets.add(root);
        root.add(A);
        root.add(B);
        root.add(C);
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 0);
        A.connect(ConstraintAnchor.Type.BOTTOM, B, ConstraintAnchor.Type.TOP, 0);
        B.connect(ConstraintAnchor.Type.TOP, A, ConstraintAnchor.Type.BOTTOM, 0);
        B.connect(ConstraintAnchor.Type.BOTTOM, C, ConstraintAnchor.Type.TOP, 0);
        C.connect(ConstraintAnchor.Type.TOP, B, ConstraintAnchor.Type.BOTTOM, 0);
        C.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, 0);
        root.layout();
        System.out.println("a) A: " + A + " B: " + B + " C: " + C);
        // all elements spread equally
        assertEquals(A.getHeight(), B.getHeight(), 1);
        assertEquals(B.getHeight(), C.getHeight(), 1);
        assertEquals(A.getTop() - root.getTop(), root.getBottom() - C.getBottom(), 1);
        assertEquals(A.getTop() - root.getTop(), B.getTop() - A.getBottom(), 1);
        assertEquals(B.getTop() - A.getBottom(), C.getTop() - B.getBottom(), 1);
        // A marked as 0dp, B == C, A takes the rest
        A.getAnchor(ConstraintAnchor.Type.TOP).setMargin(marginT);
        A.getAnchor(ConstraintAnchor.Type.BOTTOM).setMargin(marginB);
        B.getAnchor(ConstraintAnchor.Type.TOP).setMargin(marginT);
        B.getAnchor(ConstraintAnchor.Type.BOTTOM).setMargin(marginB);
        C.getAnchor(ConstraintAnchor.Type.TOP).setMargin(marginT);
        C.getAnchor(ConstraintAnchor.Type.BOTTOM).setMargin(marginB);
        A.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        root.layout();
        System.out.println("b) A: " + A + " B: " + B + " C: " + C);
        assertEquals(A.getTop(), root.getTop() + marginT);
        assertEquals(C.getBottom(), root.getBottom() - marginB);
        assertEquals(B.getBottom(), C.getTop() - marginT - marginB);
        assertEquals(A.getHeight(), root.getHeight() - B.getHeight() - C.getHeight() - 3*marginT - 3*marginB);
        assertEquals(B.getHeight(), C.getHeight());
        assertEquals(B.getHeight(), 20);
        checkVerticalPositions(A, B, C);
        // B marked as 0dp, A == C, B takes the rest
        A.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        A.setHeight(20);
        B.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        root.layout();
        System.out.println("c) A: " + A + " B: " + B + " C: " + C);
        assertEquals(B.getHeight(), root.getHeight() - A.getHeight() - C.getHeight() - 3*marginT - 3*marginB);
        assertEquals(A.getHeight(), C.getHeight());
        assertEquals(A.getHeight(), 20);
        checkVerticalPositions(A, B, C);
        // C marked as 0dp, A == B, C takes the rest
        B.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        B.setHeight(20);
        C.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        root.layout();
        System.out.println("d) A: " + A + " B: " + B + " C: " + C);
        assertEquals(C.getHeight(), root.getHeight() - A.getHeight() - B.getHeight() - 3*marginT - 3*marginB);
        assertEquals(A.getHeight(), B.getHeight());
        assertEquals(A.getHeight(), 20);
        checkVerticalPositions(A, B, C);
        // A & B marked as 0dp, C == 20
        C.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        C.setHeight(20);
        A.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        B.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        root.layout();
        System.out.println("e) A: " + A + " B: " + B + " C: " + C);
        assertEquals(C.getHeight(), 20);
        assertEquals(A.getHeight(), B.getHeight()); // L
        assertEquals(A.getHeight(), (root.getHeight() - C.getHeight() - 3*marginT - 3*marginB) / 2 , 1);
        checkVerticalPositions(A, B, C);
        // A & C marked as 0dp, B == 20
        C.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        B.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        B.setHeight(20);
        root.layout();
        System.out.println("f) A: " + A + " B: " + B + " C: " + C);
        assertEquals(B.getHeight(), 20);
        assertEquals(A.getHeight(), C.getHeight());
        assertEquals(A.getHeight(), (root.getHeight() - B.getHeight() - 3*marginT - 3*marginB) / 2, 1);
        checkVerticalPositions(A, B, C);
        // B & C marked as 0dp, A == 20
        B.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        A.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        A.setHeight(20);
        root.layout();
        System.out.println("g) A: " + A + " B: " + B + " C: " + C);
        assertEquals(A.getHeight(), 20);
        assertEquals(B.getHeight(), C.getHeight());
        assertEquals(B.getHeight(), (root.getHeight() - A.getHeight() - 3*marginT - 3*marginB) / 2, 1);
        checkVerticalPositions(A, B, C);
    }

    private void checkVerticalPositions(ConstraintWidget A, ConstraintWidget B, ConstraintWidget C) {
        assertEquals(A.getTop() <= A.getBottom(), true);
        assertEquals(A.getBottom() <= B.getTop(), true);
        assertEquals(B.getTop() <= B.getBottom(), true);
        assertEquals(B.getBottom() <= C.getTop(), true);
        assertEquals(C.getTop() <= C.getBottom(), true);
    }

    @Test
    public void testHorizontalChainWeights() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        ConstraintWidget B = new ConstraintWidget(100, 20);
        ConstraintWidget C = new ConstraintWidget(100, 20);
        int marginL = 7;
        int marginR = 27;
        root.setDebugSolverName(root.getSystem(), "root");
        A.setDebugSolverName(root.getSystem(), "A");
        B.setDebugSolverName(root.getSystem(), "B");
        C.setDebugSolverName(root.getSystem(), "C");
        ArrayList<ConstraintWidget> widgets = new ArrayList<>();
        widgets.add(A);
        widgets.add(B);
        widgets.add(C);
        widgets.add(root);
        root.add(A);
        root.add(B);
        root.add(C);
        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, marginL);
        A.connect(ConstraintAnchor.Type.RIGHT, B, ConstraintAnchor.Type.LEFT, marginR);
        B.connect(ConstraintAnchor.Type.LEFT, A, ConstraintAnchor.Type.RIGHT, marginL);
        B.connect(ConstraintAnchor.Type.RIGHT, C, ConstraintAnchor.Type.LEFT, marginR);
        C.connect(ConstraintAnchor.Type.LEFT, B, ConstraintAnchor.Type.RIGHT, marginL);
        C.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, marginR);
        A.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        B.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        C.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        A.setHorizontalWeight(1);
        B.setHorizontalWeight(1);
        C.setHorizontalWeight(1);
        root.layout();
        System.out.println("a) A: " + A + " B: " + B + " C: " + C);
        assertEquals(A.getWidth(), B.getWidth(), 1);
        assertEquals(B.getWidth(), C.getWidth(), 1);
        A.setHorizontalWeight(1);
        B.setHorizontalWeight(2);
        C.setHorizontalWeight(1);
        root.layout();
        System.out.println("b) A: " + A + " B: " + B + " C: " + C);
        assertEquals(2 * (A.getWidth() + marginL + marginR), B.getWidth() + marginL + marginR, 1);
        assertEquals(A.getWidth(), C.getWidth(), 1);
    }

    @Test
    public void testVerticalChainWeights() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        ConstraintWidget B = new ConstraintWidget(100, 20);
        ConstraintWidget C = new ConstraintWidget(100, 20);
        int marginT = 7;
        int marginB = 27;
        root.setDebugSolverName(root.getSystem(), "root");
        A.setDebugSolverName(root.getSystem(), "A");
        B.setDebugSolverName(root.getSystem(), "B");
        C.setDebugSolverName(root.getSystem(), "C");
        ArrayList<ConstraintWidget> widgets = new ArrayList<>();
        widgets.add(A);
        widgets.add(B);
        widgets.add(C);
        widgets.add(root);
        root.add(A);
        root.add(B);
        root.add(C);
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, marginT);
        A.connect(ConstraintAnchor.Type.BOTTOM, B, ConstraintAnchor.Type.TOP, marginB);
        B.connect(ConstraintAnchor.Type.TOP, A, ConstraintAnchor.Type.BOTTOM, marginT);
        B.connect(ConstraintAnchor.Type.BOTTOM, C, ConstraintAnchor.Type.TOP, marginB);
        C.connect(ConstraintAnchor.Type.TOP, B, ConstraintAnchor.Type.BOTTOM, marginT);
        C.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, marginB);
        A.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        B.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        C.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        A.setVerticalWeight(1);
        B.setVerticalWeight(1);
        C.setVerticalWeight(1);
        root.layout();
        System.out.println("a) A: " + A + " B: " + B + " C: " + C);
        assertEquals(A.getHeight(), B.getHeight(), 1);
        assertEquals(B.getHeight(), C.getHeight(), 1);
        A.setVerticalWeight(1);
        B.setVerticalWeight(2);
        C.setVerticalWeight(1);
        root.layout();
        System.out.println("b) A: " + A + " B: " + B + " C: " + C);
        assertEquals(2 * (A.getHeight() + marginT + marginB), B.getHeight() + marginT + marginB, 1);
        assertEquals(A.getHeight(), C.getHeight(), 1);
    }

    @Test
    public void testHorizontalChainPacked() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        ConstraintWidget B = new ConstraintWidget(100, 20);
        ConstraintWidget C = new ConstraintWidget(100, 20);
        int marginL = 7;
        int marginR = 27;
        root.setDebugSolverName(root.getSystem(), "root");
        A.setDebugSolverName(root.getSystem(), "A");
        B.setDebugSolverName(root.getSystem(), "B");
        C.setDebugSolverName(root.getSystem(), "C");
        ArrayList<ConstraintWidget> widgets = new ArrayList<>();
        widgets.add(A);
        widgets.add(B);
        widgets.add(C);
        widgets.add(root);
        root.add(A);
        root.add(B);
        root.add(C);
        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, marginL);
        A.connect(ConstraintAnchor.Type.RIGHT, B, ConstraintAnchor.Type.LEFT, marginR);
        B.connect(ConstraintAnchor.Type.LEFT, A, ConstraintAnchor.Type.RIGHT, marginL);
        B.connect(ConstraintAnchor.Type.RIGHT, C, ConstraintAnchor.Type.LEFT, marginR);
        C.connect(ConstraintAnchor.Type.LEFT, B, ConstraintAnchor.Type.RIGHT, marginL);
        C.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, marginR);
        A.setHorizontalChainPacked(true);
        root.layout();
        System.out.println("a) A: " + A + " B: " + B + " C: " + C);
        assertEquals(A.getLeft() - root.getLeft() - marginL, root.getRight() - marginR - C.getRight(), 1);
    }

    @Test
    public void testVerticalChainPacked() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        ConstraintWidget B = new ConstraintWidget(100, 20);
        ConstraintWidget C = new ConstraintWidget(100, 20);
        int marginT = 7;
        int marginB = 27;
        root.setDebugSolverName(root.getSystem(), "root");
        A.setDebugSolverName(root.getSystem(), "A");
        B.setDebugSolverName(root.getSystem(), "B");
        C.setDebugSolverName(root.getSystem(), "C");
        ArrayList<ConstraintWidget> widgets = new ArrayList<>();
        widgets.add(A);
        widgets.add(B);
        widgets.add(C);
        widgets.add(root);
        root.add(A);
        root.add(B);
        root.add(C);
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, marginT);
        A.connect(ConstraintAnchor.Type.BOTTOM, B, ConstraintAnchor.Type.TOP, marginB);
        B.connect(ConstraintAnchor.Type.TOP, A, ConstraintAnchor.Type.BOTTOM, marginT);
        B.connect(ConstraintAnchor.Type.BOTTOM, C, ConstraintAnchor.Type.TOP, marginB);
        C.connect(ConstraintAnchor.Type.TOP, B, ConstraintAnchor.Type.BOTTOM, marginT);
        C.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, marginB);
        A.setVerticalChainPacked(true);
        root.layout();
        System.out.println("a) A: " + A + " B: " + B + " C: " + C);
        assertEquals(A.getTop() - root.getTop() - marginT, root.getBottom() - marginB - C.getBottom(), 1);
    }

    @Test
    public void testHorizontalChainComplex() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 600);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        ConstraintWidget B = new ConstraintWidget(100, 20);
        ConstraintWidget C = new ConstraintWidget(100, 20);
        ConstraintWidget D = new ConstraintWidget(50, 20);
        ConstraintWidget E = new ConstraintWidget(50, 20);
        ConstraintWidget F = new ConstraintWidget(50, 20);
        int marginL = 7;
        int marginR = 19;
        root.setDebugSolverName(root.getSystem(), "root");
        A.setDebugSolverName(root.getSystem(), "A");
        B.setDebugSolverName(root.getSystem(), "B");
        C.setDebugSolverName(root.getSystem(), "C");
        D.setDebugSolverName(root.getSystem(), "D");
        E.setDebugSolverName(root.getSystem(), "E");
        F.setDebugSolverName(root.getSystem(), "F");
        root.add(A);
        root.add(B);
        root.add(C);
        root.add(D);
        root.add(E);
        root.add(F);
        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, marginL);
        A.connect(ConstraintAnchor.Type.RIGHT, B, ConstraintAnchor.Type.LEFT, marginR);
        B.connect(ConstraintAnchor.Type.LEFT, A, ConstraintAnchor.Type.RIGHT, marginL);
        B.connect(ConstraintAnchor.Type.RIGHT, C, ConstraintAnchor.Type.LEFT, marginR);
        C.connect(ConstraintAnchor.Type.LEFT, B, ConstraintAnchor.Type.RIGHT, marginL);
        C.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, marginR);
        A.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        B.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        C.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        D.connect(ConstraintAnchor.Type.LEFT, A, ConstraintAnchor.Type.LEFT, 0);
        D.connect(ConstraintAnchor.Type.RIGHT, A, ConstraintAnchor.Type.RIGHT, 0);
        E.connect(ConstraintAnchor.Type.LEFT, B, ConstraintAnchor.Type.LEFT, 0);
        E.connect(ConstraintAnchor.Type.RIGHT, B, ConstraintAnchor.Type.RIGHT, 0);
        F.connect(ConstraintAnchor.Type.LEFT, A, ConstraintAnchor.Type.LEFT, 0);
        F.connect(ConstraintAnchor.Type.RIGHT, A, ConstraintAnchor.Type.RIGHT, 0);
        root.layout();
        System.out.println("a) A: " + A + " B: " + B + " C: " + C);
        System.out.println("a) D: " + D + " E: " + E + " F: " + F);
        assertEquals(A.getWidth(), B.getWidth(), 1);
        assertEquals(B.getWidth(), C.getWidth(), 1);
        assertEquals(A.getWidth(), 307);
    }

    @Test
    public void testVerticalChainComplex() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 600);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        ConstraintWidget B = new ConstraintWidget(100, 20);
        ConstraintWidget C = new ConstraintWidget(100, 20);
        ConstraintWidget D = new ConstraintWidget(50, 20);
        ConstraintWidget E = new ConstraintWidget(50, 20);
        ConstraintWidget F = new ConstraintWidget(50, 20);
        int marginT = 7;
        int marginB = 19;
        root.setDebugSolverName(root.getSystem(), "root");
        A.setDebugSolverName(root.getSystem(), "A");
        B.setDebugSolverName(root.getSystem(), "B");
        C.setDebugSolverName(root.getSystem(), "C");
        D.setDebugSolverName(root.getSystem(), "D");
        E.setDebugSolverName(root.getSystem(), "E");
        F.setDebugSolverName(root.getSystem(), "F");
        root.add(A);
        root.add(B);
        root.add(C);
        root.add(D);
        root.add(E);
        root.add(F);
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, marginT);
        A.connect(ConstraintAnchor.Type.BOTTOM, B, ConstraintAnchor.Type.TOP, marginB);
        B.connect(ConstraintAnchor.Type.TOP, A, ConstraintAnchor.Type.BOTTOM, marginT);
        B.connect(ConstraintAnchor.Type.BOTTOM, C, ConstraintAnchor.Type.TOP, marginB);
        C.connect(ConstraintAnchor.Type.TOP, B, ConstraintAnchor.Type.BOTTOM, marginT);
        C.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, marginB);
        A.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        B.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        C.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        D.connect(ConstraintAnchor.Type.TOP, A, ConstraintAnchor.Type.TOP, 0);
        D.connect(ConstraintAnchor.Type.BOTTOM, A, ConstraintAnchor.Type.BOTTOM, 0);
        E.connect(ConstraintAnchor.Type.TOP, B, ConstraintAnchor.Type.TOP, 0);
        E.connect(ConstraintAnchor.Type.BOTTOM, B, ConstraintAnchor.Type.BOTTOM, 0);
        F.connect(ConstraintAnchor.Type.TOP, A, ConstraintAnchor.Type.TOP, 0);
        F.connect(ConstraintAnchor.Type.BOTTOM, A, ConstraintAnchor.Type.BOTTOM, 0);
        root.layout();
        System.out.println("a) A: " + A + " B: " + B + " C: " + C);
        System.out.println("a) D: " + D + " E: " + E + " F: " + F);
        assertEquals(A.getHeight(), B.getHeight(), 1);
        assertEquals(B.getHeight(), C.getHeight(), 1);
        assertEquals(A.getHeight(), 174);
    }


    @Test
    public void testHorizontalChainComplex2() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 379, 591);
        ConstraintWidget A = new ConstraintWidget(100, 185);
        ConstraintWidget B = new ConstraintWidget(100, 185);
        ConstraintWidget C = new ConstraintWidget(100, 185);
        ConstraintWidget D = new ConstraintWidget(53, 17);
        ConstraintWidget E = new ConstraintWidget(42, 17);
        ConstraintWidget F = new ConstraintWidget(47, 17);
        int marginL = 0;
        int marginR = 0;
        root.setDebugSolverName(root.getSystem(), "root");
        A.setDebugSolverName(root.getSystem(), "A");
        B.setDebugSolverName(root.getSystem(), "B");
        C.setDebugSolverName(root.getSystem(), "C");
        D.setDebugSolverName(root.getSystem(), "D");
        E.setDebugSolverName(root.getSystem(), "E");
        F.setDebugSolverName(root.getSystem(), "F");
        root.add(A);
        root.add(B);
        root.add(C);
        root.add(D);
        root.add(E);
        root.add(F);
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 16);
        A.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, 16);
        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, marginL);
        A.connect(ConstraintAnchor.Type.RIGHT, B, ConstraintAnchor.Type.LEFT, marginR);
        B.connect(ConstraintAnchor.Type.LEFT, A, ConstraintAnchor.Type.RIGHT, marginL);
        B.connect(ConstraintAnchor.Type.RIGHT, C, ConstraintAnchor.Type.LEFT, marginR);
        B.connect(ConstraintAnchor.Type.TOP, A, ConstraintAnchor.Type.TOP, 0);
        C.connect(ConstraintAnchor.Type.LEFT, B, ConstraintAnchor.Type.RIGHT, marginL);
        C.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, marginR);
        C.connect(ConstraintAnchor.Type.TOP, A, ConstraintAnchor.Type.TOP, 0);
        A.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        B.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        C.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        D.connect(ConstraintAnchor.Type.LEFT, A, ConstraintAnchor.Type.LEFT, 0);
        D.connect(ConstraintAnchor.Type.RIGHT, A, ConstraintAnchor.Type.RIGHT, 0);
        D.connect(ConstraintAnchor.Type.TOP, A, ConstraintAnchor.Type.BOTTOM, 0);
        E.connect(ConstraintAnchor.Type.LEFT, B, ConstraintAnchor.Type.LEFT, 0);
        E.connect(ConstraintAnchor.Type.RIGHT, B, ConstraintAnchor.Type.RIGHT, 0);
        E.connect(ConstraintAnchor.Type.TOP, A, ConstraintAnchor.Type.BOTTOM, 0);
        F.connect(ConstraintAnchor.Type.LEFT, A, ConstraintAnchor.Type.LEFT, 0);
        F.connect(ConstraintAnchor.Type.RIGHT, A, ConstraintAnchor.Type.RIGHT, 0);
        F.connect(ConstraintAnchor.Type.TOP, A, ConstraintAnchor.Type.BOTTOM, 0);
        root.layout();
        System.out.println("a) A: " + A + " B: " + B + " C: " + C);
        System.out.println("a) D: " + D + " E: " + E + " F: " + F);
        assertEquals(A.getWidth(), B.getWidth(), 1);
        assertEquals(B.getWidth(), C.getWidth(), 1);
        assertEquals(A.getWidth(), 126);
    }

    @Test
    public void testVerticalChainBaseline() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        ConstraintWidget B = new ConstraintWidget(100, 20);
        ConstraintWidget C = new ConstraintWidget(100, 20);
        root.add(A);
        root.add(B);
        root.setDebugSolverName(root.getSystem(), "root");
        A.setDebugSolverName(root.getSystem(), "A");
        B.setDebugSolverName(root.getSystem(), "B");
        C.setDebugSolverName(root.getSystem(), "C");
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 0);
        A.connect(ConstraintAnchor.Type.BOTTOM, B, ConstraintAnchor.Type.TOP, 0);
        B.connect(ConstraintAnchor.Type.TOP, A, ConstraintAnchor.Type.BOTTOM, 0);
        B.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, 0);
        root.layout();
        System.out.println("a) root: " + root + " A: " + A + " B: " + B);
        int Ay = A.getTop();
        int By = B.getTop();
        assertEquals(A.getTop() - root.getTop(), root.getBottom() - B.getBottom(), 1);
        assertEquals(B.getTop() - A.getBottom(), A.getTop() - root.getTop(), 1);
        root.add(C);
        A.setBaselineDistance(7);
        C.setBaselineDistance(7);
        C.connect(ConstraintAnchor.Type.BASELINE, A, ConstraintAnchor.Type.BASELINE, 0);
        A.setVerticalChainPacked(true);
        root.layout();
        System.out.println("b) root: " + root + " A: " + A + " B: " + B + " C: " + C);
        assertEquals(Ay, C.getTop(), 1);
    }

    @Test
    public void testWrapHorizontalChain() {
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
        A.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, 0);
        B.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 0);
        B.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, 0);
        C.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 0);
        C.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, 0);

        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 0);
        A.connect(ConstraintAnchor.Type.RIGHT, B, ConstraintAnchor.Type.LEFT, 0);
        B.connect(ConstraintAnchor.Type.LEFT, A, ConstraintAnchor.Type.RIGHT, 0);
        B.connect(ConstraintAnchor.Type.RIGHT, C, ConstraintAnchor.Type.LEFT, 0);
        C.connect(ConstraintAnchor.Type.LEFT, B, ConstraintAnchor.Type.RIGHT, 0);
        C.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 0);
        root.layout();
        System.out.println("a) root: " + root + " A: " + A + " B: " + B);
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();
        System.out.println("b) root: " + root + " A: " + A + " B: " + B);
        assertEquals(root.getHeight(), A.getHeight());
        assertEquals(root.getHeight(), B.getHeight());
        assertEquals(root.getHeight(), C.getHeight());
        assertEquals(root.getWidth(), A.getWidth() + B.getWidth() + C.getWidth());
    }

    @Test
    public void testWrapVerticalChain() {
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
        A.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 0);
        B.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 0);
        B.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 0);
        C.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 0);
        C.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT, 0);

        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 0);
        A.connect(ConstraintAnchor.Type.BOTTOM, B, ConstraintAnchor.Type.TOP, 0);
        B.connect(ConstraintAnchor.Type.TOP, A, ConstraintAnchor.Type.BOTTOM, 0);
        B.connect(ConstraintAnchor.Type.BOTTOM, C, ConstraintAnchor.Type.TOP, 0);
        C.connect(ConstraintAnchor.Type.TOP, B, ConstraintAnchor.Type.BOTTOM, 0);
        C.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, 0);
        root.layout();
        System.out.println("a) root: " + root + " A: " + A + " B: " + B);
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();
        System.out.println("b) root: " + root + " A: " + A + " B: " + B);
        assertEquals(root.getWidth(), A.getWidth());
        assertEquals(root.getWidth(), B.getWidth());
        assertEquals(root.getWidth(), C.getWidth());
        assertEquals(root.getHeight(), A.getHeight() + B.getHeight() + C.getHeight());
    }

    @Test
    public void testPackNPE() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        ConstraintWidget B = new ConstraintWidget(100, 20);
        ConstraintWidget C = new ConstraintWidget(100, 20);
        ConstraintWidget D = new ConstraintWidget(100, 20);
        root.add(A);
        root.add(B);
        root.add(C);
        root.add(D);
        root.setDebugSolverName(root.getSystem(), "root");
        A.setDebugSolverName(root.getSystem(), "A");
        B.setDebugSolverName(root.getSystem(), "B");
        C.setDebugSolverName(root.getSystem(), "C");
        D.setDebugSolverName(root.getSystem(), "D");
        A.setBaselineDistance(7);
        B.setBaselineDistance(7);
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 100);
        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        A.connect(ConstraintAnchor.Type.RIGHT, B, ConstraintAnchor.Type.LEFT);
        B.connect(ConstraintAnchor.Type.LEFT, A, ConstraintAnchor.Type.RIGHT);
        B.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        B.connect(ConstraintAnchor.Type.BASELINE, A, ConstraintAnchor.Type.BASELINE);
        C.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        C.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        D.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        D.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        C.connect(ConstraintAnchor.Type.TOP, A, ConstraintAnchor.Type.BOTTOM);
        C.connect(ConstraintAnchor.Type.BOTTOM, D, ConstraintAnchor.Type.TOP);
        D.connect(ConstraintAnchor.Type.TOP, C, ConstraintAnchor.Type.BOTTOM);
        D.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        A.setHorizontalChainPacked(true);
        C.setVerticalChainPacked(true);
        root.layout();
        System.out.println("a) root: " + root + " A: " + A + " B: " + B);
        System.out.println("a) root: " + root + " C: " + C + " D: " + D);
        C.getAnchor(ConstraintAnchor.Type.TOP).reset();
        root.layout();
    }
}
