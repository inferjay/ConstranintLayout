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
package android.constraint.solver;

import android.constraint.solver.LinearSystem;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class LinearSystemTest {

    LinearSystem s;

    @BeforeMethod
    public void setUp() {
        s = new LinearSystem();
        android.constraint.solver.LinearEquation.resetNaming();
    }

    @Test
    public void testAddEquation1() {
        android.constraint.solver.LinearEquation e1 = new android.constraint.solver.LinearEquation(s);
        e1.var("W3.left").equalsTo().var(0);
        s.addConstraint(e1);
        s.rebuildGoalFromErrors();
        assertEquals(s.getGoal().toReadableString(), "0 = 0.0");
        assertEquals(s.getRow(0).toReadableString(), "W3.left = 0.0");
    }

    @Test
    public void testAddEquation2() {
        android.constraint.solver.LinearEquation e1 = new android.constraint.solver.LinearEquation(s);
        e1.var("W3.left").equalsTo().var(0);
        android.constraint.solver.LinearEquation e2 = new android.constraint.solver.LinearEquation(s);
        e2.var("W3.right").equalsTo().var(600);
        s.addConstraint(e1);
        s.addConstraint(e2);
        s.rebuildGoalFromErrors();
        assertEquals(s.getGoal().toReadableString(), "0 = 0.0");
        assertEquals(s.getRow(0).toReadableString(), "W3.left = 0.0");
        assertEquals(s.getRow(1).toReadableString(), "W3.right = 600.0");
    }

    @Test
    public void testAddEquation3() {
        android.constraint.solver.LinearEquation e1 = new android.constraint.solver.LinearEquation(s);
        e1.var("W3.left").equalsTo().var(0);
        android.constraint.solver.LinearEquation e2 = new android.constraint.solver.LinearEquation(s);
        e2.var("W3.right").equalsTo().var(600);
        android.constraint.solver.LinearEquation left_constraint = new android.constraint.solver.LinearEquation(s);
        left_constraint.var("W4.left").equalsTo().var("W3.left"); // left constraint
        s.addConstraint(e1);
        s.addConstraint(e2);
        s.addConstraint(left_constraint); // left
        s.rebuildGoalFromErrors();
        assertEquals(s.getRow(0).toReadableString(), "W3.left = 0.0");
        assertEquals(s.getRow(1).toReadableString(), "W3.right = 600.0");
        assertEquals(s.getRow(2).toReadableString(), "W4.left = 0.0");
    }

    @Test
    public void testAddEquation4() {
        android.constraint.solver.LinearEquation e1 = new android.constraint.solver.LinearEquation(s);
        android.constraint.solver.LinearEquation e2 = new android.constraint.solver.LinearEquation(s);
        android.constraint.solver.LinearEquation e3 = new android.constraint.solver.LinearEquation(s);
        android.constraint.solver.LinearEquation e4 = new android.constraint.solver.LinearEquation(s);
        e1.var(2, "Xm").equalsTo().var("Xl").plus("Xr");
        s.addConstraint(e1); // 2 Xm = Xl + Xr
        assertEquals(s.getRow(0).toReadableString(), "Xm = 0.5 Xl + 0.5 Xr");
        e2.var("Xl").plus(10).lowerThan().var("Xr");
        s.addConstraint(e2); // Xl + 10 <= Xr

        assertEquals(s.getRow(0).toReadableString(), "Xm = 5.0 + Xl + 0.5 s1");
        assertEquals(s.getRow(1).toReadableString(), "Xr = 10.0 + Xl + s1");
        e3.var("Xl").greaterThan().var(-10);
        s.addConstraint(e3); // Xl >= -10
        assertEquals(s.getRow(0).toReadableString(), "Xm = -5.0 + 0.5 s1 + s2");
        assertEquals(s.getRow(1).toReadableString(), "Xr = s1 + s2");
        assertEquals(s.getRow(2).toReadableString(), "Xl = -10.0 + s2");
        e4.var("Xr").lowerThan().var(100);
        s.addConstraint(e4); // Xr <= 100
        assertEquals(s.getRow(0).toReadableString(), "Xm = 45.0 + 0.5 s2 - 0.5 s3");
        assertEquals(s.getRow(1).toReadableString(), "Xr = 100.0 - s3");
        assertEquals(s.getRow(2).toReadableString(), "Xl = -10.0 + s2");
        assertEquals(s.getRow(3).toReadableString(), "s1 = 100.0 - s2 - s3");
        s.rebuildGoalFromErrors();
        assertEquals(s.getGoal().toReadableString(), "0 = 0.0");
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
        android.constraint.solver.LinearEquation e5 = new android.constraint.solver.LinearEquation(s);
        e5.var("Xm").equalsTo().var(50);
        s.addConstraint(e5);
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
