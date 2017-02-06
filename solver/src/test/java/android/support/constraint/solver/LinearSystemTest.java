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

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class LinearSystemTest {

    LinearSystem s;

    @BeforeMethod
    public void setUp() {
        s = new LinearSystem();
        LinearEquation.resetNaming();
    }

    void add(LinearEquation equation) {
        ArrayRow row1 = LinearEquation.createRowFromEquation(s, equation);
        s.addConstraint(row1);
    }

    void add(LinearEquation equation, int strength) {
        ArrayRow row1 = LinearEquation.createRowFromEquation(s, equation);
        SolverVariable e1 = s.createErrorVariable();
        SolverVariable e2 = s.createErrorVariable();
        e1.strength = strength;
        e2.strength = strength;
        row1.addError(e1, e2);
        s.addConstraint(row1);
    }

    @Test
    public void testPriorityBasic() {
        add(new LinearEquation(s).var(2, "Xm").equalsTo().var("Xl").plus("Xr"));
        add(new LinearEquation(s).var("Xl").plus(10).lowerThan().var("Xr"));
        add(new LinearEquation(s).var("Xl").greaterThan().var(-10));
        add(new LinearEquation(s).var("Xr").lowerThan().var(100));
        add(new LinearEquation(s).var("Xm").equalsTo().var(50), 2);
        add(new LinearEquation(s).var("Xl").equalsTo().var(30), 1);
        add(new LinearEquation(s).var("Xr").equalsTo().var(60), 0);
        try {
            s.minimize();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Result: ");
        s.displayReadableRows();
        assertEquals(s.getValueFor("Xm"), 50.0f);
        assertEquals(s.getValueFor("Xl"), 30.0f);
        assertEquals(s.getValueFor("Xr"), 70.0f);
    }

    @Test
    public void testPriority() {
        for (int i = 0; i < 3; i++) {
            s.reset();
            LinearEquation eq1 = new LinearEquation(s);
            eq1.var("A").equalsTo().var(10);
            ArrayRow row1 = LinearEquation.createRowFromEquation(s, eq1);
            SolverVariable e1 = s.createErrorVariable();
            SolverVariable e2 = s.createErrorVariable();
            e1.strength = i % 3;
            e2.strength = i % 3;
            row1.addError(e1, e2);

            LinearEquation eq2 = new LinearEquation(s);
            eq2.var("A").equalsTo().var(100);
            ArrayRow row2 = LinearEquation.createRowFromEquation(s, eq2);
            SolverVariable e3 = s.createErrorVariable();
            SolverVariable e4 = s.createErrorVariable();
            e3.strength = (i + 1) % 3;
            e4.strength = (i + 1) % 3;
            row2.addError(e3, e4);

            LinearEquation eq3 = new LinearEquation(s);
            eq3.var("A").equalsTo().var(1000);
            ArrayRow row3 = LinearEquation.createRowFromEquation(s, eq3);
            SolverVariable e5 = s.createErrorVariable();
            SolverVariable e6 = s.createErrorVariable();
            e5.strength = (i + 2) % 3;
            e6.strength = (i + 2) % 3;
            row3.addError(e5, e6);

            s.addConstraint(row1);
            s.addConstraint(row2);
            s.addConstraint(row3);
            try {
                s.minimize();
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("Check at iteration " + i);
            s.displayReadableRows();
            if (i == 0) {
                assertEquals(s.getValueFor("A"), 1000.0f);
            } else if (i == 1) {
                assertEquals(s.getValueFor("A"), 100.0f);
            } else if (i == 2) {
                assertEquals(s.getValueFor("A"), 10.0f);
            }
        }
    }

    @Test
    public void testAddEquation1() {
        LinearEquation e1 = new LinearEquation(s);
        e1.var("W3.left").equalsTo().var(0);
        s.addConstraint(LinearEquation.createRowFromEquation(s, e1));
        s.rebuildGoalFromErrors();
        assertEquals(s.getGoal().toString(), "Goal: ");
        assertEquals(s.getRow(0).toReadableString(), "W3.left = 0.0");
    }

    @Test
    public void testAddEquation2() {
        LinearEquation e1 = new LinearEquation(s);
        e1.var("W3.left").equalsTo().var(0);
        LinearEquation e2 = new LinearEquation(s);
        e2.var("W3.right").equalsTo().var(600);
        s.addConstraint(LinearEquation.createRowFromEquation(s, e1));
        s.addConstraint(LinearEquation.createRowFromEquation(s, e2));
        s.rebuildGoalFromErrors();
        assertEquals(s.getGoal().toString(), "Goal: ");
        assertEquals(s.getRow(0).toReadableString(), "W3.left = 0.0");
        assertEquals(s.getRow(1).toReadableString(), "W3.right = 600.0");
    }

    @Test
    public void testAddEquation3() {
        LinearEquation e1 = new LinearEquation(s);
        e1.var("W3.left").equalsTo().var(0);
        LinearEquation e2 = new LinearEquation(s);
        e2.var("W3.right").equalsTo().var(600);
        LinearEquation left_constraint = new LinearEquation(s);
        left_constraint.var("W4.left").equalsTo().var("W3.left"); // left constraint
        s.addConstraint(LinearEquation.createRowFromEquation(s, e1));
        s.addConstraint(LinearEquation.createRowFromEquation(s, e2));
        s.addConstraint(LinearEquation.createRowFromEquation(s, left_constraint)); // left
        s.rebuildGoalFromErrors();
        assertEquals(s.getRow(0).toReadableString(), "W3.left = 0.0");
        assertEquals(s.getRow(1).toReadableString(), "W3.right = 600.0");
        assertEquals(s.getRow(2).toReadableString(), "W4.left = 0.0");
    }

    @Test
    public void testAddEquation4() {
        LinearEquation e1 = new LinearEquation(s);
        LinearEquation e2 = new LinearEquation(s);
        LinearEquation e3 = new LinearEquation(s);
        LinearEquation e4 = new LinearEquation(s);
        e1.var(2, "Xm").equalsTo().var("Xl").plus("Xr");
        s.addConstraint(LinearEquation.createRowFromEquation(s, e1)); // 2 Xm = Xl + Xr
        assertEquals(s.getRow(0).toReadableString(), "Xm = 0.5 Xl + 0.5 Xr");
        e2.var("Xl").plus(10).lowerThan().var("Xr");
        s.addConstraint(LinearEquation.createRowFromEquation(s, e2)); // Xl + 10 <= Xr

        assertEquals(s.getRow(0).toReadableString(), "Xm = 5.0 + Xl + 0.5 s1");
        assertEquals(s.getRow(1).toReadableString(), "Xr = 10.0 + Xl + s1");
        e3.var("Xl").greaterThan().var(-10);
        s.addConstraint(LinearEquation.createRowFromEquation(s, e3)); // Xl >= -10
        assertEquals(s.getRow(0).toReadableString(), "Xm = -5.0 + 0.5 s1 + s2");
        assertEquals(s.getRow(1).toReadableString(), "Xr = s1 + s2");
        assertEquals(s.getRow(2).toReadableString(), "Xl = -10.0 + s2");
        e4.var("Xr").lowerThan().var(100);
        s.addConstraint(LinearEquation.createRowFromEquation(s, e4)); // Xr <= 100
        assertEquals(s.getRow(0).toReadableString(), "Xm = 45.0 + 0.5 s2 - 0.5 s3");
        assertEquals(s.getRow(1).toReadableString(), "Xr = 100.0 - s3");
        assertEquals(s.getRow(2).toReadableString(), "Xl = -10.0 + s2");
        assertEquals(s.getRow(3).toReadableString(), "s1 = 100.0 - s2 - s3");
        s.rebuildGoalFromErrors();
        assertEquals(s.getGoal().toString(), "Goal: ");
        try {
            s.minimizeGoal(s.getGoal());
        } catch (Exception e) {
            e.printStackTrace();
        }
        int xl = (int) s.getValueFor("Xl");
        int xm = (int) s.getValueFor("Xm");
        int xr = (int) s.getValueFor("Xr");
        assertEquals(xl, -10);
        assertEquals(xm, 45);
        assertEquals(xr, 100);
        LinearEquation e5 = new LinearEquation(s);
        e5.var("Xm").equalsTo().var(50);
        s.addConstraint(LinearEquation.createRowFromEquation(s, e5));
        try {
            s.minimizeGoal(s.getGoal());
        } catch (Exception e) {
            e.printStackTrace();
        }
        xl = (int) s.getValueFor("Xl");
        xm = (int) s.getValueFor("Xm");
        xr = (int) s.getValueFor("Xr");
        assertEquals(xl, 0);
        assertEquals(xm, 50);
        assertEquals(xr, 100);
    }

}
