package android.support.constraint.solver;

import android.support.constraint.solver.widgets.Barrier;
import android.support.constraint.solver.widgets.ConstraintAnchor;
import android.support.constraint.solver.widgets.ConstraintWidget;
import android.support.constraint.solver.widgets.ConstraintWidgetContainer;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Tests for Barriers
 */
public class BarrierTest {

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

        root.setOptimizationLevel(ConstraintWidgetContainer.OPTIMIZATION_NONE);
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
}