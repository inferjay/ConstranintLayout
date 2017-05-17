package android.support.constraint.solver;

import android.support.constraint.solver.widgets.ConstraintAnchor;
import android.support.constraint.solver.widgets.ConstraintWidget;
import android.support.constraint.solver.widgets.ConstraintWidgetContainer;
import android.support.constraint.solver.widgets.Optimizer;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Basic wrap test
 */
public class WrapTest {

    @Test
    public void testBasic() {
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

        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);

        root.layout();
        System.out.println("a) root: " + root + " A: " + A);

        A.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_SPREAD, 0, 100, 0);
        A.setVerticalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_SPREAD, 0, 60, 0);
        root.layout();
        System.out.println("b) root: " + root + " A: " + A);
    }

    @Test
    public void testBasic2() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 600);
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
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        B.connect(ConstraintAnchor.Type.LEFT, A, ConstraintAnchor.Type.RIGHT);
        B.connect(ConstraintAnchor.Type.RIGHT, C, ConstraintAnchor.Type.LEFT);
        B.connect(ConstraintAnchor.Type.TOP, A, ConstraintAnchor.Type.BOTTOM);
        B.connect(ConstraintAnchor.Type.BOTTOM, C, ConstraintAnchor.Type.TOP);
        C.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        C.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        B.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        B.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        B.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_SPREAD, 0, 100, 0);
        B.setVerticalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_SPREAD, 0, 60, 0);

        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);

        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.layout();
        System.out.println("root: " + root + " A: " + A + " B: " + B + " C: " + C);
        assertEquals(root.getWidth(), 200);
        assertEquals(root.getHeight(), 40);

        B.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_SPREAD, 20, 100, 0);
        B.setVerticalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_SPREAD, 30, 60, 0);
        root.setWidth(0);
        root.setHeight(0);
        root.layout();
        System.out.println("root: " + root + " A: " + A + " B: " + B + " C: " + C);
        assertEquals(root.getWidth(), 220);
        assertEquals(root.getHeight(), 70);
    }

    @Test
    public void testRatioWrap() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 100, 600);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        A.setDebugName("A");
        root.add(A);
        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        A.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        A.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);

        A.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        A.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        A.setDimensionRatio("1:1");

        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);

        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.layout();
        System.out.println("root: " + root + " A: " + A);
        assertEquals(root.getWidth(), 100);
        assertEquals(root.getHeight(), 100);

        root.setHeight(600);
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
        root.layout();
        System.out.println("root: " + root + " A: " + A);
        assertEquals(root.getWidth(), 600);
        assertEquals(root.getHeight(), 600);

        root.setWidth(100);
        root.setHeight(600);
        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);

        root.layout();
        System.out.println("root: " + root + " A: " + A);
        assertEquals(root.getWidth(), 0);
        assertEquals(root.getHeight(), 0);
    }

    @Test
    public void testRatioWrap2() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 1000, 600);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        ConstraintWidget B = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        A.setDebugName("A");
        B.setDebugName("B");
        root.add(A);
        root.add(B);
        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        B.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        B.connect(ConstraintAnchor.Type.TOP, A, ConstraintAnchor.Type.BOTTOM);
        B.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        B.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);

        B.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        B.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        B.setDimensionRatio("1:1");

        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);

        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.layout();
        System.out.println("root: " + root + " A: " + A + " B: " + B);
        assertEquals(root.getWidth(), 100);
        assertEquals(root.getHeight(), 120);
    }

    @Test
    public void testRatioWrap3() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 500, 600);
        ConstraintWidget A = new ConstraintWidget(100, 60);
        ConstraintWidget B = new ConstraintWidget(100, 20);
        ConstraintWidget C = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        A.setDebugName("A");
        B.setDebugName("B");
        C.setDebugName("C");
        root.add(A);
        root.add(B);
        root.add(C);

        A.setBaselineDistance(100);
        B.setBaselineDistance(10);
        C.setBaselineDistance(10);

        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        A.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        A.connect(ConstraintAnchor.Type.RIGHT, B, ConstraintAnchor.Type.LEFT);
        A.setVerticalBiasPercent(0);

        B.connect(ConstraintAnchor.Type.LEFT, A, ConstraintAnchor.Type.RIGHT);
        B.connect(ConstraintAnchor.Type.BASELINE, A, ConstraintAnchor.Type.BASELINE);
        B.connect(ConstraintAnchor.Type.RIGHT, C, ConstraintAnchor.Type.LEFT);

        C.connect(ConstraintAnchor.Type.LEFT, B, ConstraintAnchor.Type.RIGHT);
        C.connect(ConstraintAnchor.Type.BASELINE, B, ConstraintAnchor.Type.BASELINE);
        C.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);

        A.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        A.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        A.setDimensionRatio("1:1");

        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.layout();
        System.out.println("root: " + root + " A: " + A + " B: " + B + " C: " + C);

        assertEquals(A.getWidth(), 300);
        assertEquals(A.getHeight(), 300);
        assertEquals(B.getLeft(), 300);
        assertEquals(B.getTop(), 90);
        assertEquals(B.getWidth(), 100);
        assertEquals(B.getHeight(), 20);
        assertEquals(C.getLeft(), 400);
        assertEquals(C.getTop(), 90);
        assertEquals(C.getWidth(), 100);
        assertEquals(C.getHeight(), 20);

        root.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        A.setBaselineDistance(10);

        root.layout();
        System.out.println("root: " + root + " A: " + A + " B: " + B + " C: " + C);
        assertEquals(root.getWidth(), 220);
        assertEquals(root.getHeight(), 20);
    }

    @Test
    public void testGoneChainWrap() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 500, 600);
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

        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        B.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        C.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        D.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);

        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        A.connect(ConstraintAnchor.Type.BOTTOM, B, ConstraintAnchor.Type.TOP);
        B.connect(ConstraintAnchor.Type.TOP, A, ConstraintAnchor.Type.BOTTOM);
        B.connect(ConstraintAnchor.Type.BOTTOM, C, ConstraintAnchor.Type.TOP);
        C.connect(ConstraintAnchor.Type.TOP, B, ConstraintAnchor.Type.BOTTOM);
        C.connect(ConstraintAnchor.Type.BOTTOM, D, ConstraintAnchor.Type.TOP);
        D.connect(ConstraintAnchor.Type.TOP, C, ConstraintAnchor.Type.BOTTOM);
        D.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);

        B.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        D.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);

        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.layout();
        System.out.println("root: " + root + " A: " + A + " B: " + B + " C: " + C + " D: " + D);
        assertEquals(root.getHeight(), 40);

        A.setVerticalChainStyle(ConstraintWidget.CHAIN_PACKED);
        root.layout();
        System.out.println("root: " + root + " A: " + A + " B: " + B + " C: " + C + " D: " + D);
        assertEquals(root.getHeight(), 40);
    }

    @Test
    public void testWrap() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 500, 600);
        ConstraintWidget A = new ConstraintWidget(100, 0);
        ConstraintWidget B = new ConstraintWidget(100, 20);
        ConstraintWidget C = new ConstraintWidget(100, 20);
        ConstraintWidget D = new ConstraintWidget(100, 40);
        ConstraintWidget E = new ConstraintWidget(100, 20);
        root.setDebugName("root");
        A.setDebugName("A");
        B.setDebugName("B");
        C.setDebugName("C");
        D.setDebugName("D");
        E.setDebugName("E");
        root.add(A);
        root.add(B);
        root.add(C);
        root.add(D);
        root.add(E);

        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        B.connect(ConstraintAnchor.Type.LEFT, A, ConstraintAnchor.Type.RIGHT);
        C.connect(ConstraintAnchor.Type.LEFT, A, ConstraintAnchor.Type.RIGHT);
        D.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);

        A.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        A.connect(ConstraintAnchor.Type.TOP, B, ConstraintAnchor.Type.TOP);
        A.connect(ConstraintAnchor.Type.BOTTOM, C, ConstraintAnchor.Type.BOTTOM);
        B.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        C.connect(ConstraintAnchor.Type.TOP, B, ConstraintAnchor.Type.BOTTOM);
        D.connect(ConstraintAnchor.Type.TOP, A, ConstraintAnchor.Type.BOTTOM);

        E.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        E.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        E.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);

        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.layout();
        System.out.println("root: " + root + " A: " + A + " B: " + B + " C: " + C + " D: " + D + " E: " + E);
        assertEquals(root.getHeight(), 80);
        assertEquals(E.getTop(), 30);
    }
}
