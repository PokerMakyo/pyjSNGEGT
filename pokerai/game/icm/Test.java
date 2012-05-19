package pokerai.game.icm;

/*
    @Author: Indiana

    See: http://pokerai.org/pf3/viewtopic.php?f=58&t=836
    

    This file is part of SNGEGT.

    SNGEGT is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 2 of the License, or
    (at your option) any later version.

    SNGEGT is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with SNGEGT.  If not, see <http://www.gnu.org/licenses/>.
*/


public class Test {

  public static void main(String[] args) {
    CalcRanges cr = new CalcRanges();

    // Example 1
    int players = 7;
    double[] payouts = {0.50, 0.30, 0.20, 0, 0, 0, 0};
    int[] stacks = {5000, 5000, 5000, 5000, 5000, 5000, 5000};
    int[] ranges = new int[players]; // stores the result
    int raisor = 2;  // my position when there is no raised before me
    int BB = 300;
    int ante = 0;
    boolean noSmallBlind = false;
    double evTreshold = 0;
    cr.calc(players, stacks, raisor, BB, ante, noSmallBlind, evTreshold, payouts, ranges);
    System.out.println("Push Example (#1): ");
    for (int i = 0; i < players; i++) System.out.println("#" + i + ": " + ranges[i] + "% ");
    System.out.println();
    
    // Example 2
    players = 7;
    payouts = new double[]{0.50, 0.30, 0.20, 0, 0, 0, 0};
    stacks = new int[]{5000, 5000, 5000, 5000, 5000, 5000, 5000};
    ranges = new int[players]; // stores the result
    raisor = 5;  // my position when there is no raised before me
    BB = 300;
    ante = 0;
    noSmallBlind = false;
    evTreshold = 0;
    cr.calc(players, stacks, raisor, BB, ante, noSmallBlind, evTreshold, payouts, ranges);
    System.out.println("Call Example (#2): ");
    for (int i = 0; i < players; i++) System.out.println("#" + i + ": " + ranges[i] + "% ");
    System.out.println();
  }

}

