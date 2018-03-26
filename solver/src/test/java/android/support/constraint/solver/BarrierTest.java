package android.support.constraint.solver;

import android.support.constraint.solver.widgets.*;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Tests for Barriers
 */
public class BarrierTest {

    @Test
    public void barrierImage() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        ConstraintWidget B = new ConstraintWidget(200, 20);
        ConstraintWidget C = new ConstraintWidget(60, 60);
        Barrier barrier = new Barrier();

        root.setDebugSolverName(root.getSystem(), "root");
        A.setDebugSolverName(root.getSystem(), "A");
        B.setDebugSolverName(root.getSystem(), "B");
        C.setDebugSolverName(root.getSystem(), "C");
        barrier.setDebugSolverName(root.getSystem(), "Barrier");
        barrier.setBarrierType(Barrier.RIGHT);

        barrier.add(A);
        barrier.add(B);

        root.add(A);
        root.add(B);
        root.add(C);
        root.add(barrier);

        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        A.connect(ConstraintAnchor.Type.BOTTOM, B, ConstraintAnchor.Type.TOP);

        B.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        B.connect(ConstraintAnchor.Type.TOP, A, ConstraintAnchor.Type.BOTTOM);
        B.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);

        A.setVerticalChainStyle(ConstraintWidget.CHAIN_SPREAD_INSIDE);

        C.setHorizontalBiasPercent(1);
        C.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        C.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM);
        C.connect(ConstraintAnchor.Type.LEFT, barrier, ConstraintAnchor.Type.RIGHT);
        C.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);

        root.setOptimizationLevel(Optimizer.OPTIMIZATION_STANDARD);
        root.layout();
        System.out.println("A: " + A + " B: " + B + " C: " + C + " barrier: " + barrier);
        assertEquals(A.getLeft(), 0);
        assertEquals(A.getTop(), 0);
        assertEquals(B.getLeft(), 0);
        assertEquals(B.getTop(), 580);
        assertEquals(C.getLeft(), 740);
        assertEquals(C.getTop(), 270);
        assertEquals(barrier.getLeft(), 200);
    }

    @Test
    public void barrierTooStrong() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget A = new ConstraintWidget(60, 60);
        ConstraintWidget B = new ConstraintWidget(100, 200);
        ConstraintWidget C = new ConstraintWidget(100, 20);
        Barrier barrier = new Barrier();

        root.setDebugSolverName(root.getSystem(), "root");
        A.setDebugSolverName(root.getSystem(), "A");
        B.setDebugSolverName(root.getSystem(), "B");
        C.setDebugSolverName(root.getSystem(), "C");
        barrier.setDebugSolverName(root.getSystem(), "Barrier");
        barrier.setBarrierType(Barrier.BOTTOM);

        barrier.add(B);

        root.add(A);
        root.add(B);
        root.add(C);
        root.add(barrier);

        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        A.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);

        B.connect(ConstraintAnchor.Type.TOP, C, ConstraintAnchor.Type.BOTTOM);
        B.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_PARENT);

        C.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_PARENT);
        C.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        C.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        C.connect(ConstraintAnchor.Type.BOTTOM, A, ConstraintAnchor.Type.BOTTOM);

        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.layout();
        System.out.println("A: " + A + " B: " + B + " C: " + C + " barrier: " + barrier);
        assertEquals(A.getLeft(), 740);
        assertEquals(A.getTop(), 0);
        assertEquals(B.getLeft(), 0);
        assertEquals(B.getTop(), 60);
        assertEquals(B.getWidth(), 800);
        assertEquals(B.getHeight(), 200);
        assertEquals(C.getLeft(), 0);
        assertEquals(C.getTop(), 0);
        assertEquals(C.getWidth(), 800);
        assertEquals(C.getHeight(), 60);
        assertEquals(barrier.getBottom(), 260);
    }

    @Test
    public void barrierMax() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        ConstraintWidget B = new ConstraintWidget(150, 20);
        Barrier barrier = new Barrier();

        root.setDebugSolverName(root.getSystem(), "root");
        A.setDebugSolverName(root.getSystem(), "A");
        B.setDebugSolverName(root.getSystem(), "B");
        barrier.setDebugSolverName(root.getSystem(), "Barrier");

        barrier.add(A);

        root.add(A);
        root.add(barrier);
        root.add(B);

        barrier.setBarrierType(Barrier.RIGHT);

        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        B.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP);
        B.connect(ConstraintAnchor.Type.LEFT, barrier, ConstraintAnchor.Type.LEFT);
        B.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        B.setHorizontalBiasPercent(0);
        B.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        B.setHorizontalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_SPREAD, 0, 150, 1);
        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.layout();

        System.out.println("A: " + A + " B: " + B + " barrier: " + barrier);
        assertEquals(A.getLeft(), 0);
        assertEquals(barrier.getLeft(), 100);
        assertEquals(B.getLeft(), 100);
        assertEquals(B.getWidth(), 150);
    }

    @Test
    public void basic() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        ConstraintWidget B = new ConstraintWidget(150, 20);
        Barrier barrier = new Barrier();

        root.setDebugSolverName(root.getSystem(), "root");
        A.setDebugSolverName(root.getSystem(), "A");
        B.setDebugSolverName(root.getSystem(), "B");
        barrier.setDebugSolverName(root.getSystem(), "Barrier");

        root.add(A);
        root.add(B);
        root.add(barrier);

        barrier.setBarrierType(Barrier.LEFT);

        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        A.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 50);

        B.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        B.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        B.connect(ConstraintAnchor.Type.TOP, A, ConstraintAnchor.Type.BOTTOM, 20);

        barrier.add(A);
        barrier.add(B);

        root.setOptimizationLevel(Optimizer.OPTIMIZATION_NONE);
        root.layout();

        System.out.println("A: " + A + " B: " + B + " barrier: " + barrier);
        assertEquals(barrier.getLeft(), B.getLeft());

        barrier.setBarrierType(Barrier.RIGHT);
        root.layout();
        System.out.println("A: " + A + " B: " + B + " barrier: " + barrier);
        assertEquals(barrier.getRight(), B.getRight());

        barrier.setBarrierType(Barrier.LEFT);
        B.setWidth(10);
        root.layout();
        System.out.println("A: " + A + " B: " + B + " barrier: " + barrier);
        assertEquals(barrier.getLeft(), A.getLeft());
    }

    @Test
    public void growArray() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        ConstraintWidget B = new ConstraintWidget(150, 20);
        ConstraintWidget C = new ConstraintWidget(175, 20);
        ConstraintWidget D = new ConstraintWidget(200, 20);
        ConstraintWidget E = new ConstraintWidget(125, 20);
        Barrier barrier = new Barrier();

        root.setDebugSolverName(root.getSystem(), "root");
        A.setDebugSolverName(root.getSystem(), "A");
        B.setDebugSolverName(root.getSystem(), "B");
        C.setDebugSolverName(root.getSystem(), "C");
        D.setDebugSolverName(root.getSystem(), "D");
        E.setDebugSolverName(root.getSystem(), "E");
        barrier.setDebugSolverName(root.getSystem(), "Barrier");

        root.add(A);
        root.add(B);
        root.add(C);
        root.add(D);
        root.add(E);
        root.add(barrier);

        barrier.setBarrierType(Barrier.LEFT);

        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        A.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 50);

        B.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        B.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        B.connect(ConstraintAnchor.Type.TOP, A, ConstraintAnchor.Type.BOTTOM, 20);

        C.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        C.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        C.connect(ConstraintAnchor.Type.TOP, B, ConstraintAnchor.Type.BOTTOM, 20);

        D.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        D.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        D.connect(ConstraintAnchor.Type.TOP, C, ConstraintAnchor.Type.BOTTOM, 20);


        E.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        E.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        E.connect(ConstraintAnchor.Type.TOP, D, ConstraintAnchor.Type.BOTTOM, 20);

        barrier.add(A);
        barrier.add(B);
        barrier.add(C);
        barrier.add(D);
        barrier.add(E);

        root.layout();

        System.out.println("A: " + A + " B: " + B + " C: " + C + " D: " + D + " E: " + E + " barrier: " + barrier);
        assertEquals(barrier.getLeft(), D.getLeft());
    }

    @Test
    public void connection() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        ConstraintWidget B = new ConstraintWidget(150, 20);
        ConstraintWidget C = new ConstraintWidget(100, 20);
        Barrier barrier = new Barrier();

        root.setDebugSolverName(root.getSystem(), "root");
        A.setDebugSolverName(root.getSystem(), "A");
        B.setDebugSolverName(root.getSystem(), "B");
        C.setDebugSolverName(root.getSystem(), "C");
        barrier.setDebugSolverName(root.getSystem(), "Barrier");

        root.add(A);
        root.add(B);
        root.add(C);
        root.add(barrier);

        barrier.setBarrierType(Barrier.LEFT);

        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        A.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 50);

        B.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT);
        B.connect(ConstraintAnchor.Type.RIGHT, root, ConstraintAnchor.Type.RIGHT);
        B.connect(ConstraintAnchor.Type.TOP, A, ConstraintAnchor.Type.BOTTOM, 20);

        C.connect(ConstraintAnchor.Type.LEFT, barrier, ConstraintAnchor.Type.LEFT, 0);
        barrier.add(A);
        barrier.add(B);

        root.layout();

        System.out.println("A: " + A + " B: " + B + " C: " + C + " barrier: " + barrier);
        assertEquals(barrier.getLeft(), B.getLeft());
        assertEquals(C.getLeft(), barrier.getLeft());

    }

    @Test
    public void withGuideline() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        Barrier barrier = new Barrier();
        Guideline guideline = new Guideline();

        root.setDebugSolverName(root.getSystem(), "root");
        A.setDebugSolverName(root.getSystem(), "A");
        barrier.setDebugSolverName(root.getSystem(), "Barrier");
        guideline.setDebugSolverName(root.getSystem(), "Guideline");

        guideline.setOrientation(ConstraintWidget.VERTICAL);
        guideline.setGuideBegin(200);
        barrier.setBarrierType(Barrier.RIGHT);

        root.add(A);
        root.add(barrier);
        root.add(guideline);

        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 10);
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 50);

        barrier.add(A);
        barrier.add(guideline);

        root.layout();

        System.out.println("A: " + A + " guideline: " + guideline + " barrier: " + barrier);
        assertEquals(barrier.getLeft(), guideline.getLeft());
    }

    @Test
    public void wrapIssue() {
        ConstraintWidgetContainer root = new ConstraintWidgetContainer(0, 0, 800, 600);
        ConstraintWidget A = new ConstraintWidget(100, 20);
        ConstraintWidget B = new ConstraintWidget(100, 20);
        Barrier barrier = new Barrier();
        root.setDebugSolverName(root.getSystem(), "root");
        A.setDebugSolverName(root.getSystem(), "A");
        B.setDebugSolverName(root.getSystem(), "B");
        barrier.setDebugSolverName(root.getSystem(), "Barrier");
        barrier.setBarrierType(Barrier.BOTTOM);

        root.add(A);
        root.add(B);
        root.add(barrier);

        A.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 0);
        A.connect(ConstraintAnchor.Type.TOP, root, ConstraintAnchor.Type.TOP, 0);

        barrier.add(A);

        B.connect(ConstraintAnchor.Type.LEFT, root, ConstraintAnchor.Type.LEFT, 0);
        B.connect(ConstraintAnchor.Type.TOP, barrier, ConstraintAnchor.Type.BOTTOM, 0);
        B.connect(ConstraintAnchor.Type.BOTTOM, root, ConstraintAnchor.Type.BOTTOM, 0);

        root.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        root.layout();
        System.out.println("1/ root: " + root + " A: " + A + " B: " + B + " barrier: " + barrier);

        assertEquals(barrier.getTop(), A.getBottom());
        assertEquals(B.getTop(), barrier.getBottom());
        assertEquals(root.getHeight(), A.getHeight() + B.getHeight());

        A.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        A.setVerticalMatchStyle(ConstraintWidget.MATCH_CONSTRAINT_WRAP, 0, 0, 0);

        root.layout();
        System.out.println("2/ root: " + root + " A: " + A + " B: " + B + " barrier: " + barrier);

        assertEquals(barrier.getTop(), A.getBottom());
        assertEquals(B.getTop(), barrier.getBottom());
        assertEquals(root.getHeight(), A.getHeight() + B.getHeight());
    }
}
