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

import android.support.constraint.solver.LinearEquation;
import android.support.constraint.solver.LinearSystem;
import android.support.constraint.solver.widgets.Animator;
import android.support.constraint.solver.widgets.ConstraintAnchor;
import android.support.constraint.solver.widgets.ConstraintHorizontalLayout;
import android.support.constraint.solver.widgets.ConstraintTableLayout;
import android.support.constraint.solver.widgets.ConstraintWidget;
import android.support.constraint.solver.widgets.ConstraintWidgetContainer;
import android.support.constraint.solver.widgets.Guideline;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;

import static org.testng.Assert.assertEquals;

public class WidgetsPositioningTest {

    LinearSystem s = new LinearSystem();

    @BeforeMethod
    public void setUp() {
        s = new LinearSystem();
        LinearEquation.resetNaming();
    }

    @Test
    public void testWidgetCenterPositioning() {
        final int x = 20;
        final int y = 30;
        final ConstraintWidget rootWidget = new ConstraintWidget(x, y, 600, 400);
        final ConstraintWidget centeredWidget = new ConstraintWidget(100, 20);
        ArrayList<ConstraintWidget> widgets = new ArrayList<ConstraintWidget>();
        widgets.add(rootWidget);
        widgets.add(centeredWidget);

        centeredWidget.connect(ConstraintAnchor.Type.CENTER_X, rootWidget, ConstraintAnchor.Type.CENTER_X);
        centeredWidget.connect(ConstraintAnchor.Type.CENTER_Y, rootWidget, ConstraintAnchor.Type.CENTER_Y);

        runTestOnWidgets(widgets, new Runnable() {
            @Override
            public void run() {
                int left = centeredWidget.getLeft();
                int top = centeredWidget.getTop();
                int right = centeredWidget.getRight();
                int bottom = centeredWidget.getBottom();
                assertEquals(left, x + 250);
                assertEquals(right, x + 350);
                assertEquals(top, y + 190);
                assertEquals(bottom, y + 210);
            }
        });
    }

    @Test
    public void testBaselinePositioning() {
        final ConstraintWidget A = new ConstraintWidget(20, 230, 200, 70);
        final ConstraintWidget B = new ConstraintWidget(200, 60, 200, 100);
        ArrayList<ConstraintWidget> widgets = new ArrayList<ConstraintWidget>();
        widgets.add(A);
        widgets.add(B);
        A.setBaselineDistance(40);
        B.setBaselineDistance(60);
        B.connect(ConstraintAnchor.Type.BASELINE, A, ConstraintAnchor.Type.BASELINE);
        runTestOnWidgets(widgets, new Runnable() {
            @Override
            public void run() {
                assertEquals(B.getTop() + B.getBaselineDistance(),
                        A.getTop() + A.getBaselineDistance());
            }
        });
    }

    @Test
    public void testWidgetTopRightPositioning() {
        // Easy to tweak numbers to test larger systems
        int numLoops = 10;
        int numWidgets = 100;

        for (int j = 0; j < numLoops; j++) {
            s.reset();
            ArrayList<ConstraintWidget> widgets = new ArrayList();
            int w = 100 + j;
            int h = 20 + j;
            ConstraintWidget first = new ConstraintWidget(w, h);
            widgets.add(first);
            ConstraintWidget previous = first;
            int margin = 20;
            for (int i = 0; i < numWidgets; i++) {
                ConstraintWidget widget = new ConstraintWidget(w, h);
                widget.connect(ConstraintAnchor.Type.LEFT, previous, ConstraintAnchor.Type.RIGHT, margin);
                widget.connect(ConstraintAnchor.Type.TOP, previous, ConstraintAnchor.Type.BOTTOM, margin);
                widgets.add(widget);
                previous = widget;
            }
            for (ConstraintWidget widget : widgets) {
                widget.addToSolver(s);
            }
            try {
                s.minimize();
            } catch (Exception e) {
                e.printStackTrace();
            }
            for (int i = 0; i < widgets.size(); i++) {
                ConstraintWidget widget = widgets.get(i);
                widget.updateFromSolver(s);
                int left = widget.getLeft();
                int top = widget.getTop();
                int right = widget.getRight();
                int bottom = widget.getBottom();
                assertEquals(left, i * (w + margin));
                assertEquals(right, i * (w + margin) + w);
                assertEquals(top, i * (h + margin));
                assertEquals(bottom, i * (h + margin) + h);
            }
        }
    }

    @Test
    public void testHorizontalLayout() {
        final ConstraintHorizontalLayout layout = new ConstraintHorizontalLayout(500, 400);
        final int n = 5;
        final int w = 100;
        int h = 20;
        final ArrayList<ConstraintWidget> widgets = new ArrayList<ConstraintWidget>();
        for (int i = 0; i < n; i++) {
            ConstraintWidget widget = new ConstraintWidget(w, h);
            layout.add(widget);
            widgets.add(widget);
        }
        layout.addToSolver(s);
        for (int i = 0; i < n; i++) {
            ConstraintWidget widget = widgets.get(i);
//            System.out.println("[" + i + "] -> " + widget);
        }
        try {
            s.minimize();
        } catch (Exception e) {
            e.printStackTrace();
        }
        layout.updateFromSolver(s);
//        System.out.println("After minimize, layout " + layout);
        runTestOnWidgets(widgets, new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < n; i++) {
                    ConstraintWidget widget = widgets.get(i);
//            System.out.println("[" +i + "] -> " + widget);
                    int left = widget.getLeft();
                    int top = widget.getTop();
                    int right = widget.getRight();
                    int bottom = widget.getBottom();
                    float m = (layout.getWidth() - (n * w)) / (float) (n + 1);
                    float pos = (float) (m + (i * w) + (i * m));
                    if (pos < 0) {
                        pos = 0;
                    }
                    assertEquals((float)left, pos, 1);
                }
            }
        });
    }

    @Test
    public void testWrapContent() {
        int numTests = 200;
        long[] numTestsResults = new long[numTests];
        ConstraintHorizontalLayout layout = new ConstraintHorizontalLayout(20, 30, 400, 400);
        int n = 10;
        int w = 100;
        int h = 20;
        ArrayList<ConstraintWidget> widgets = new ArrayList<ConstraintWidget>();
        for (int i = 0; i < n; i++) {
            ConstraintWidget widget = new ConstraintWidget(w, h);
            layout.add(widget);
            widgets.add(widget);
        }
        layout.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        layout.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        for (int j = 0; j < numTests; j++) {
            s.reset();
            long startTime = System.currentTimeMillis();
            layout.addToSolver(s);
            try {
                s.minimize();
            } catch (Exception e) {
                e.printStackTrace();
            }
            s.displaySystemInformations();
            layout.updateFromSolver(s);
            long endTime = System.currentTimeMillis();
            assertEquals(layout.getWidth(), 100 * n, 1);
            assertEquals(layout.getHeight(), 20);
            numTestsResults[j] = endTime - startTime;
            System.out.println("Test " + j + " run in " + numTestsResults[j] + " ms");
        }
        long total = 0;
        for (int j = 0; j < numTests; j++) {
            total += numTestsResults[j];
        }
        total /= numTests;
        System.out.println("Test took in average " + total + " ms");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testWrapSimpleWrapContent() {
        final ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 1000);
        final ConstraintWidget A = new ConstraintWidget(0, 0, 200, 20);
        ArrayList<ConstraintWidget> widgets = new ArrayList<ConstraintWidget>();
        widgets.add(root);
        widgets.add(A);

        root.setDebugSolverName(s, "root");
        A.setDebugSolverName(s, "A");

        root.add(A);
        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        A.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        A.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);

        runTestOnWidgets(widgets, new Runnable() {
            @Override
            public void run() {
                System.out.println("Simple Wrap: " + root + ", " + A);
                assertEquals(root.getWidth(), A.getWidth());
                assertEquals(root.getHeight(), A.getHeight());
                assertEquals(A.getWidth(), 200);
                assertEquals(A.getHeight(), 20);
            }
        });
    }

    @Test
    public void testMatchConstraint() {
        final ConstraintWidgetContainer root = new ConstraintWidgetContainer(50, 50, 500, 500);
        final ConstraintWidget A = new ConstraintWidget(10, 20, 100, 30);
        final ConstraintWidget B = new ConstraintWidget(150, 200, 100, 30);
        final ConstraintWidget C = new ConstraintWidget(50, 50);
        ArrayList<ConstraintWidget> widgets = new ArrayList<ConstraintWidget>();
        A.setDebugName("A");
        B.setDebugName("B");
        C.setDebugName("C");
        root.setDebugName("root");
        root.add(A);
        root.add(B);
        root.add(C);
        widgets.add(root);
        widgets.add(A);
        widgets.add(B);
        widgets.add(C);

        C.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.ANY);
        C.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.ANY);
        C.connect(ConstraintAnchor.Type.LEFT, A, ConstraintAnchor.Type.RIGHT);
        C.connect(ConstraintAnchor.Type.RIGHT, B, ConstraintAnchor.Type.LEFT);
        C.connect(ConstraintAnchor.Type.TOP, A, ConstraintAnchor.Type.BOTTOM);
        C.connect(ConstraintAnchor.Type.BOTTOM, B, ConstraintAnchor.Type.TOP);
        runTestOnWidgets(widgets, new Runnable() {
            @Override
            public void run() {
                root.updateDrawPosition();
                assertEquals(C.getX(), A.getRight());
                assertEquals(C.getRight(), B.getX());
                assertEquals(C.getY(), A.getBottom());
                assertEquals(C.getBottom(), B.getY());
            }
        });
    }

    @Test
    public void testWidgetStrengthPositioning() {
        final ConstraintWidget root = new ConstraintWidget(400, 400);
        final ConstraintWidget A = new ConstraintWidget(20, 20);
        ArrayList<ConstraintWidget> widgets = new ArrayList<ConstraintWidget>();
        widgets.add(root);
        widgets.add(A);

        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        A.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        A.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        System.out.println("Widget A centered inside Root");
        runTestOnWidgets(widgets, new Runnable() {
            @Override
            public void run() {
                assertEquals(A.getLeft(), 190);
                assertEquals(A.getRight(), 210);
                assertEquals(A.getTop(), 190);
                assertEquals(A.getBottom(), 210);
            }
        });
        System.out.println("Widget A weak left, should move to the right");
        A.getAnchor(ConstraintAnchor.Type.LEFT).setStrength(ConstraintAnchor.Strength.WEAK);
        runTestOnWidgets(widgets, new Runnable() {
            @Override
            public void run() {
                assertEquals(A.getLeft(), 380);
                assertEquals(A.getRight(), 400);
            }
        });
        System.out.println("Widget A weak right, should go back to center");
        A.getAnchor(ConstraintAnchor.Type.RIGHT).setStrength(ConstraintAnchor.Strength.WEAK);
        runTestOnWidgets(widgets, new Runnable() {
            @Override
            public void run() {
                assertEquals(A.getLeft(), 190);
                assertEquals(A.getRight(), 210);
            }
        });
        System.out.println("Widget A strong left, should move to the left");
        A.getAnchor(ConstraintAnchor.Type.LEFT).setStrength(ConstraintAnchor.Strength.STRONG);
        runTestOnWidgets(widgets, new Runnable() {
            @Override
            public void run() {
                assertEquals(A.getLeft(), 0);
                assertEquals(A.getRight(), 20);
                assertEquals(root.getWidth(), 400);
            }
        });
    }

    @Test
    public void testWidgetPositionMove() {
        final ConstraintWidget A = new ConstraintWidget(0, 0, 100, 20);
        final ConstraintWidget B = new ConstraintWidget(0, 30, 200, 20);
        final ConstraintWidget C = new ConstraintWidget(0, 60, 100, 20);
        ArrayList<ConstraintWidget> widgets = new ArrayList<ConstraintWidget>();
        widgets.add(A);
        widgets.add(B);
        widgets.add(C);
        A.setDebugSolverName(s, "A");
        B.setDebugSolverName(s, "B");
        C.setDebugSolverName(s, "C");

        B.connect(ConstraintAnchor.Type.LEFT, A, ConstraintAnchor.Type.RIGHT);
        C.setOrigin(200, 0);
        B.connect(ConstraintAnchor.Type.RIGHT, C, ConstraintAnchor.Type.RIGHT);

        Runnable check = new Runnable() {
            @Override
            public void run() {
                assertEquals(A.getWidth(), 100);
                assertEquals(B.getWidth(), 200);
                assertEquals(C.getWidth(), 100);
            }
        };
        runTestOnWidgets(widgets, check);
        System.out.println("A: " + A + " B: " + B + " C: " + C);
        C.setOrigin(100, 0);
//        runTestOnUIWidgets(widgets);
        runTestOnWidgets(widgets, check);
        System.out.println("A: " + A + " B: " + B + " C: " + C);
        C.setOrigin(50, 0);
        runTestOnWidgets(widgets, check);
        System.out.println("A: " + A + " B: " + B + " C: " + C);
    }

    @Test
    public void testTable2() {
        final ConstraintWidget root = new ConstraintWidget(200, 100, 700, 600);
        final ConstraintTableLayout table = new ConstraintTableLayout(768, 400);
//        final ConstraintWidget A = new ConstraintWidget(50, 20);
//        final ConstraintWidget B = new ConstraintWidget(140, 40);
//        final ConstraintWidget C = new ConstraintWidget(80, 20);
//        final ConstraintWidget D = new ConstraintWidget(80, 20);
        final ConstraintWidget A = new ConstraintWidget(100, 40);
        final ConstraintWidget B = new ConstraintWidget(280, 80);
        final ConstraintWidget C = new ConstraintWidget(160, 40);
        final ConstraintWidget D = new ConstraintWidget(160, 40);
        // null(40, 180) - (100 x 40) null(130, 160) - (280 x 80) null(10, 580) - (160 x 40) null(190, 580) - (160 x 40)

        table.setDebugSolverName(s, "table");
        root.setDebugSolverName(s, "root");
        A.setDebugSolverName(s, "A");
        B.setDebugSolverName(s, "B");
        C.setDebugSolverName(s, "C");
        D.setDebugSolverName(s, "D");
        table.add(A);
        table.add(B);
        table.add(C);
        table.add(D);


        table.setNumCols(3);
        table.setTableDimensions();
        table.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        table.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        table.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
//        table.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        table.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        table.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);

        ArrayList<ConstraintWidget> widgets = new ArrayList<ConstraintWidget>();
        widgets.add(table);
        widgets.add(A);
        widgets.add(B);
        widgets.add(C);
        widgets.add(D);
        widgets.add(root);
//        runTestOnUIWidgets(widgets);
        runTestOnWidgets(widgets, new Runnable() {
            @Override
            public void run() {
                System.out.println("\nTable WxH: " + table.getWidth() + "x" + table.getHeight()
                        + " A: " + A + " B: " + B
                        + " C: " + C + " D: " + D
                );
                ArrayList<Guideline> v = table.getVerticalGuidelines();
                for (Guideline g : v) {
                    System.out.print(" " + g);
                }
                System.out.println();
//                s.displayVariablesReadableRows();
                assertEquals(A.getWidth(), 100, 1, "A");
                assertEquals(B.getWidth(), 280, 1, "B");
                assertEquals(C.getWidth(), 160, 1, "C");
                assertEquals(D.getWidth(), 160, 1, "D");
                assertEquals(A.getHeight(), 40, "A");
                assertEquals(B.getHeight(), 80, "B");
                assertEquals(C.getHeight(), 40, "C");
                assertEquals(D.getHeight(), 40, "D");
                A.setDimension(100, 40);
                B.setDimension(280, 80);
                C.setDimension(160, 40);
                D.setDimension(160, 40);
//                assertEquals(A.getDrawY(), 280, 2f, "A");
//                assertEquals(B.getDrawY(), 260, 2f, "B");
//                assertEquals(C.getDrawY(), 280, 2f, "C");
//                assertEquals(D.getDrawY(), 480, 2f, "D");
            }
        });
    }

    @Test
    public void testWrapProblem() {
        final ConstraintWidgetContainer root = new ConstraintWidgetContainer(400, 400);
        final ConstraintWidget A = new ConstraintWidget(80, 300);
        final ConstraintWidget B = new ConstraintWidget(250, 80);
        final ArrayList<ConstraintWidget> widgets = new ArrayList<ConstraintWidget>();
        widgets.add(root);
        widgets.add(B);
        widgets.add(A);
        A.setParent(root);
        B.setParent(root);
        root.setDebugSolverName(s, "root");
        A.setDebugSolverName(s, "A");
        B.setDebugSolverName(s, "B");

        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        A.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        B.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        B.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        B.getAnchor(ConstraintAnchor.Type.TOP).setStrength(ConstraintAnchor.Strength.WEAK);

        runTestOnWidgets(widgets, new Runnable() {
            @Override
            public void run() {
                assertEquals(A.getWidth(), 80);
                assertEquals(A.getHeight(), 300);
                assertEquals(B.getWidth(), 250);
                assertEquals(B.getHeight(), 80);
                assertEquals(A.getY(), 0);
                assertEquals(B.getY(), 220);
            }
        });
    }

    //@Test
    public void testTable() {
        final ConstraintTableLayout table = new ConstraintTableLayout(400, 400);
        final ConstraintWidget A = new ConstraintWidget(100, 20);
        final ConstraintWidget B = new ConstraintWidget(120, 30);
        final ConstraintWidget C = new ConstraintWidget(80, 10);

        A.setDebugSolverName(s, "A");
        B.setDebugSolverName(s, "B");
        C.setDebugSolverName(s, "C");
        table.add(A);
        table.add(B);
        table.add(C);

        table.setTableDimensions();
        table.setDebugSolverName(s, "Table");

        ArrayList<ConstraintWidget> widgets = new ArrayList<ConstraintWidget>();
        widgets.add(table);
        widgets.add(A);
        widgets.add(B);
        widgets.add(C);
        runTestOnWidgets(widgets, new Runnable() {
            @Override
            public void run() {
                assertEquals(A.getWidth(), 100);
                assertEquals(B.getWidth(), 120);
                assertEquals(C.getWidth(), 80);
                assertEquals(A.getHeight(), 20);
                assertEquals(B.getHeight(), 30);
                assertEquals(C.getHeight(), 10);
                assertEquals(A.getX(), table.getX() + 50);
                assertEquals(B.getX(), table.getX() + 240);
                assertEquals(C.getX(), table.getX() + 60);
                assertEquals(A.getY(), table.getY() + 90);
                assertEquals(B.getY(), table.getY() + 85);
                assertEquals(C.getY(), table.getY() + 295);
                s.displaySystemInformations();
            }
        });
        table.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        runTestOnWidgets(widgets, new Runnable() {
            @Override
            public void run() {
//                System.out.println("Run done - " + table.getWidth() + " B.right: " + B.getRight());
//                s.displayVariablesReadableRows();
                assertEquals(A.getWidth(), 100);
                assertEquals(B.getWidth(), 120);
                assertEquals(C.getWidth(), 80);
                assertEquals(A.getHeight(), 20);
                assertEquals(B.getHeight(), 30);
                assertEquals(C.getHeight(), 10);
                assertEquals(A.getX(), table.getX());
                assertEquals(B.getX(), table.getX() + 100);
                assertEquals(C.getX(), table.getX() + 10);
                assertEquals(A.getY(), table.getY() + 90);
                assertEquals(B.getY(), table.getY() + 85);
                assertEquals(C.getY(), table.getY() + 295);
                assertEquals(table.getWidth(), 220);
                assertEquals(table.getHeight(), 400);
                s.displaySystemInformations();
            }
        });
        table.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        runTestOnWidgets(widgets, new Runnable() {
            @Override
            public void run() {
//                System.out.println("Run done - " + table.getWidth() + " B.right: " + B.getRight());
//                s.displayVariablesReadableRows();
                assertEquals(A.getWidth(), 100);
                assertEquals(B.getWidth(), 120);
                assertEquals(C.getWidth(), 80);
                assertEquals(A.getHeight(), 20);
                assertEquals(B.getHeight(), 30);
                assertEquals(C.getHeight(), 10);
                assertEquals(A.getX(), table.getX());
                assertEquals(B.getX(), table.getX() + 100);
                assertEquals(C.getX(), table.getX() + 10);
                assertEquals(A.getY(), table.getY() + 5);
                assertEquals(B.getY(), table.getY() + 0);
                assertEquals(C.getY(), table.getY() + 30);
                assertEquals(table.getWidth(), 220);
                assertEquals(table.getHeight(), 40);
                s.displaySystemInformations();
            }
        });
    }

    //@Test
    public void testGuideline() {
        final ConstraintWidget root = new ConstraintWidget(400, 400);
        final ConstraintWidget A = new ConstraintWidget(100, 20);
        final Guideline guideline = new Guideline();
        guideline.setParent(root);
        guideline.setRelativePercent(50);
        root.setDebugSolverName(s, "root");
        A.setDebugSolverName(s, "A");
        guideline.setDebugSolverName(s, "guideline");

        ArrayList<ConstraintWidget> widgets = new ArrayList<ConstraintWidget>();
        widgets.add(root);
        widgets.add(A);
        widgets.add(guideline);

        A.connect(ConstraintAnchor.Type.LEFT, guideline, ConstraintAnchor.Type.LEFT);
        Runnable check = new Runnable() {
            @Override
            public void run() {
                assertEquals(A.getWidth(), 100);
                assertEquals(A.getHeight(), 20);
                assertEquals(A.getX(), 200);
            }
        };
        runTestOnWidgets(widgets, check);
        System.out.println("" + root + " " + A + " " + guideline);
//        s.displayReadableRows();
        guideline.setRelativePercent(0);
        runTestOnWidgets(widgets, new Runnable() {
            @Override
            public void run() {
                assertEquals(A.getWidth(), 100);
                assertEquals(A.getHeight(), 20);
                assertEquals(A.getX(), 0);
            }
        });
        guideline.setRelativeBegin(150);
        runTestOnWidgets(widgets, new Runnable() {
            @Override
            public void run() {
                assertEquals(A.getWidth(), 100);
                assertEquals(A.getHeight(), 20);
                assertEquals(A.getX(), 150);
            }
        });
        System.out.println("" + root + " " + A + " " + guideline);
        guideline.setRelativeEnd(150);
        runTestOnWidgets(widgets, new Runnable() {
            @Override
            public void run() {
                assertEquals(A.getWidth(), 100);
                assertEquals(A.getHeight(), 20);
                assertEquals(A.getX(), 250);
            }
        });
        System.out.println("" + root + " " + A + " " + guideline);
        guideline.setOrientation(Guideline.VERTICAL);
        A.resetAnchors();
        A.connect(ConstraintAnchor.Type.TOP, guideline, ConstraintAnchor.Type.TOP);
        guideline.setRelativeBegin(150);
        runTestOnWidgets(widgets, new Runnable() {
            @Override
            public void run() {
                assertEquals(A.getWidth(), 100);
                assertEquals(A.getHeight(), 20);
                assertEquals(A.getY(), 150);
            }
        });
        System.out.println("" + root + " " + A + " " + guideline);
        A.resetAnchors();
        A.connect(ConstraintAnchor.Type.TOP, guideline, ConstraintAnchor.Type.BOTTOM);
        runTestOnWidgets(widgets, new Runnable() {
            @Override
            public void run() {
                assertEquals(A.getWidth(), 100);
                assertEquals(A.getHeight(), 20);
                assertEquals(A.getY(), 150);
            }
        });
        System.out.println("" + root + " " + A + " " + guideline);
    }

    @Test
    public void testWidgetInfeasiblePosition() {
        final ConstraintWidget A = new ConstraintWidget(100, 20);
        final ConstraintWidget B = new ConstraintWidget(100, 20);
        ArrayList<ConstraintWidget> widgets = new ArrayList<ConstraintWidget>();
        widgets.add(B);
        widgets.add(A);

        A.connect(ConstraintAnchor.Type.RIGHT, B, ConstraintAnchor.Type.LEFT);
        B.connect(ConstraintAnchor.Type.RIGHT, A, ConstraintAnchor.Type.LEFT);
        runTestOnWidgets(widgets, new Runnable() {
            @Override
            public void run() {
                // TODO: this fail -- need to figure the best way to fix this.
//                assertEquals(A.getWidth(), 100);
//                assertEquals(B.getWidth(), 100);
            }
        });
    }

    @Test
    public void testWidgetMultipleDependentPositioning() {
        final ConstraintWidget root = new ConstraintWidget(400, 400);
        final ConstraintWidget A = new ConstraintWidget(100, 20);
        final ConstraintWidget B = new ConstraintWidget(100, 20);
        root.setDebugSolverName(s, "root");
        A.setDebugSolverName(s, "A");
        B.setDebugSolverName(s, "B");
        ArrayList<ConstraintWidget> widgets = new ArrayList<ConstraintWidget>();
        widgets.add(root);
        widgets.add(B);
        widgets.add(A);

        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 10);
        A.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, 10);
        B.connect(ConstraintAnchor.Type.TOP, A, ConstraintAnchor.Type.BOTTOM);
        B.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);

        runTestOnWidgets(widgets, new Runnable() {
            @Override
            public void run() {
//                System.out.println("" + root + " " + A + " " + B);
                assertEquals(root.getHeight(), 400);
                assertEquals(root.getHeight(), 400);
                assertEquals(A.getHeight(), 20);
                assertEquals(B.getHeight(), 20);
                assertEquals(A.getTop() - root.getTop(), root.getBottom() - A.getBottom());
                assertEquals(B.getTop() - A.getBottom(), root.getBottom() - B.getBottom());
            }
        });
    }
     /*
     * Insert the widgets in all permutations
     * (to test that the insert order
     * doesn't impact the resolution)
     */

    private void runTestOnWidgets(ArrayList<ConstraintWidget> widgets, Runnable check) {
        ArrayList<Integer> tail = new ArrayList<Integer>();
        for (int i = 0; i < widgets.size(); i++) {
            tail.add(i);
        }
        addToSolverWithPermutation(widgets, new ArrayList<Integer>(), tail, check);
    }

    private void runTestOnUIWidgets(ArrayList<ConstraintWidget> widgets) {
        for (int i = 0; i < widgets.size(); i++) {
            ConstraintWidget widget = widgets.get(i);
            if (widget.getDebugName() != null) {
                widget.setDebugSolverName(s, widget.getDebugName());
            }
            widget.addToSolver(s);
        }
        try {
            s.minimize();
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (int j = 0; j < widgets.size(); j++) {
            ConstraintWidget w = widgets.get(j);
            w.updateFromSolver(s);
            System.out.println(" " + w);
        }
    }

    private void addToSolverWithPermutation(ArrayList<ConstraintWidget> widgets,
            ArrayList<Integer> list, ArrayList<Integer> tail, Runnable check) {
        if (tail.size() > 0) {
            int n = tail.size();
            for (int i = 0; i < n; i++) {
                list.add(tail.get(i));
                ArrayList<Integer> permuted = new ArrayList<Integer>(tail);
                permuted.remove(i);
                addToSolverWithPermutation(widgets, list, permuted, check);
                list.remove(list.size() - 1);
            }
        } else {
//            System.out.print("Adding widgets in order: ");
            s.reset();
            for (int i = 0; i < list.size(); i++) {
                int index = list.get(i);
//                System.out.print(" " + index);
                ConstraintWidget widget = widgets.get(index);
                if (widget.getDebugName() != null) {
                    widget.setDebugSolverName(s, widget.getDebugName());
                }
                widget.addToSolver(s);
            }
//            System.out.println("");
//            s.displayReadableRows();
            try {
                s.minimize();
            } catch (Exception e) {
                e.printStackTrace();
            }
            for (int j = 0; j < widgets.size(); j++) {
                ConstraintWidget w = widgets.get(j);
                w.updateFromSolver(s);
            }
            try {
                Animator.setAnimationEnabled(false);
                check.run();
            } catch (AssertionError e) {
                System.out.println("Assertion error: " + e);
                runTestOnUIWidgets(widgets);
            }
        }
    }

}
