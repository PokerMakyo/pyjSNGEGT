package pokerai.game.icm;

import java.io.RandomAccessFile;
import java.util.StringTokenizer;

/*
 
  @ Mod by Indiana
	
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


public class ICM2 {

  public static String data = "evs.txt";
  public static boolean DEBUGOMA = false;

  /**
   * ****************************************************************************************
   * stackEV.cpp
   * ****************************************************************************************
   */
  private int playersCount;

  private double[] ICMstacks = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
  private int PRIZES = 3;
  private double[] prize = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
  private double chipstotal = 0;
  private double[] prizeev = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
  private int[] playeratlevel = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
  private double[] probability = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

  private void calclLevel(int level, int player) {
    int i;
    double chipsatlevel;

    if (level >= PRIZES)
      return;
    if (ICMstacks[player] == 0)
      return;
    if (prize[level] == 0.0)
      return;
    chipsatlevel = chipstotal;
    for (i = 0; i < level; i++) {
      if (playeratlevel[i] == player)
        return;
      chipsatlevel -= ICMstacks[playeratlevel[i]];
    }

    playeratlevel[level] = player;
    probability[level] = ((level == 0) ? (double) 1 : probability[level - 1]) * ICMstacks[player] / chipsatlevel;
    prizeev[player] += prize[level] * probability[level];

    for (i = 0; i < 10; i++)
      calclLevel(level + 1, i);
  }


  private void calcICM(double[] iStacks, double[] iPrizeEV, int n, double[] ICMs, int ICMc) {
    int i = 0;

    // Initialize price table
    for (i = 0; i < ICMc; i++)
      prize[i] = ICMs[i];
    PRIZES = ICMc;

    // Clear global variables
    chipstotal = 0;
    for (i = 0; i < 10; i++) {
      ICMstacks[i] = 0;
      prizeev[i] = 0;
      playeratlevel[i] = 0;
      probability[i] = 0;
    }

    // Copy stack sizes and calculate sum
    for (i = 0; i < n; i++) {
      ICMstacks[i] = iStacks[i];
      chipstotal += ICMstacks[i];
    }

    // ICM -calculation
    for (i = 0; i < n; i++)
      calclLevel(0, i);

    // Result copying
    double sum = 0;
    for (i = 0; i < n; i++) {
      iPrizeEV[i] = prizeev[i];
      sum += prizeev[i];
    }

    /*
    Debug.WriteLine("\ncalcICM");
    for (int a = 0; a < 10; a++)
    {
        Debug.WriteLine("{0}\t{1}", iStacks[a], iPrizeEV[a]);
    }
    */

    // Calculations goes totally wrong without this correction if there is two players
    // and own stack goes to zero
    if (playersCount <= ICMc) {
      // reviewed, finally, that the restitution to EV's figure is as high as they ought to
      double sumInput = 0;
      double sumCalculated = 0;
      for (i = 0; i < playersCount; i++) {
        sumInput += ICMs[i];
        sumCalculated += iPrizeEV[i];
      }

      //Debug.WriteLine("sumInput={0}\tsumCalculated={1}", sumInput, sumCalculated);
      if (sumCalculated < sumInput) {
        for (i = 0; i < playersCount; i++) {
          if (iPrizeEV[i] == 0) {
            //Debug.WriteLine("iPrizeEV[{0}] = {1} - {2}", i, sumInput, sumCalculated);
            iPrizeEV[i] = sumInput - sumCalculated;
            break;
          }
        }
      }
    }

  }


  /**
   * ****************************************************************************************
   * stack.cpp
   * ****************************************************************************************
   */
  private static short HAND = 0;
  private static short OLDSTACK = 1;
  private static short NEWSTACK = 2;

  private void calcStacks(double[][] stacks, double blinds) {

    // If hands strength is equal
    if (stacks[0][HAND] == stacks[1][HAND]) {
      stacks[0][NEWSTACK] = stacks[0][OLDSTACK] + blinds / 2;
      stacks[1][NEWSTACK] = stacks[1][OLDSTACK] + blinds / 2;
    }

    // first one wins
    else if (stacks[0][HAND] > stacks[1][HAND]) {
      if (stacks[0][OLDSTACK] > stacks[1][OLDSTACK]) {
        stacks[0][NEWSTACK] = stacks[0][OLDSTACK] + stacks[1][OLDSTACK] + blinds;
        stacks[1][NEWSTACK] = 0;
      } else {
        stacks[0][NEWSTACK] = stacks[0][OLDSTACK] + stacks[0][OLDSTACK] + blinds;
        stacks[1][NEWSTACK] = stacks[1][OLDSTACK] - stacks[0][OLDSTACK];
      }
    }
    // Other one wins
    else {
      if (stacks[1][OLDSTACK] > stacks[0][OLDSTACK]) {
        stacks[1][NEWSTACK] = stacks[1][OLDSTACK] + stacks[0][OLDSTACK] + blinds;
        stacks[0][NEWSTACK] = 0;
      } else {
        stacks[1][NEWSTACK] = stacks[1][OLDSTACK] + stacks[1][OLDSTACK] + blinds;
        stacks[0][NEWSTACK] = stacks[0][OLDSTACK] - stacks[1][OLDSTACK];
      }
    }
  }


  /**
   * ****************************************************************************************
   * push.cpp
   * ****************************************************************************************
   */
  private static short STACK = 0;
  private static short BETS = 1;
  private static short CALLRANGE = 2;
  private static short HOLDPERC = 3;
  private static short CALLPERC = 4;
  private static short WINPERC = 5;
  private static short EVWIN = 6;
  private static short EVLOSE = 7;
  private static short PREPOST = 8;
  //private static   short  EVCALL    = 8;


  private void oppPercentages(int[] myHand, int oppIndex, double[][] playerData, double calledBefore) {
    int lkm = 0;
    int total = 0;
    int crash = 0;
    int myHandIndex = 0;
    int oppHandIndex = 0;
    double myWinPercent = 0;
    double oppWinPercent = 0;

    // suited
    if (myHand[0] / 13 == myHand[1] / 13) {
      if (myHand[0] > myHand[1])
        myHandIndex = indexArray[myHand[0] % 13][myHand[1] % 13];
      else
        myHandIndex = indexArray[myHand[1] % 13][myHand[0] % 13];
    }
    // offsuit
    else {
      if (myHand[0] % 13 > myHand[1] % 13)
        myHandIndex = indexArray[myHand[1] % 13][myHand[0] % 13];
      else
        myHandIndex = indexArray[myHand[0] % 13][myHand[1] % 13];
    }

    for (int i = 0; i < 51; i++) {
      for (int a = i + 1; a < 52; a++) {
        total++;
        if (playerData[oppIndex][CALLRANGE] >= handValues[i][a] && (myHand[0] != i && myHand[0] != a && myHand[1] != i && myHand[1] != a)) {
          lkm++;
          // suited
          if (i / 13 == a / 13) {
            if (i > a)
              oppHandIndex = indexArray[i % 13][a % 13];
            else
              oppHandIndex = indexArray[a % 13][i % 13];
          }
          // offsuit
          else {
            if (i % 13 > a % 13)
              oppHandIndex = indexArray[a % 13][i % 13];
            else
              oppHandIndex = indexArray[i % 13][a % 13];
          }

          myWinPercent += evs[myHandIndex][oppHandIndex];
          oppWinPercent += evs[oppHandIndex][myHandIndex];
        }
        if (myHand[0] == i || myHand[0] == a || myHand[1] == i || myHand[1] == a)
          crash++;
      }
    }

    playerData[oppIndex][HOLDPERC] = lkm / (1326.0 - crash);
    playerData[oppIndex][CALLPERC] = (1.0 - calledBefore) * playerData[oppIndex][ HOLDPERC];
    playerData[oppIndex][WINPERC] = myWinPercent / lkm;
  }


  private double calcEVfold(int myIndex, double[][] playerData, int chipsToindex, double[] ICMs, int ICMc) {
    // blinds +antes
    double betSum = 0;

    // Handle stacks in temporary array
    double[] stacks = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    for (int i = 0; i < 10; i++) {
      stacks[i] = playerData[i][ STACK]-playerData[i][ BETS];
      betSum += playerData[i][ BETS];
    }

    // parameter in bets from the index for
    stacks[chipsToindex] += betSum;

    if (DEBUGOMA) {
      System.out.println("\n********************************************************\n\nEV fold stacks:");
      for (int i = 0; i < 10; i++)
        System.out.println(i + "  " +stacks[i]);
    }

    double[] ICMfoldArray = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    calcICM(stacks, ICMfoldArray, 10, ICMs, ICMc);
    return ICMfoldArray[myIndex];
  }


  private double calcEVnoCall(int myIndex, double[][] playerData, double[] ICMs, int ICMc) {
    // blinds +antes
    double betSum = 0;

    // stäkkejä deals with a temporary table
    double[] stacks = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    for (int i = 0; i < 10; i++) {
      stacks[i] = playerData[i][ STACK]-playerData[i][ BETS];
      betSum += playerData[i][ BETS];
    }

    // Raise the awareness of himself, all on the table in bets
    stacks[myIndex] += betSum;
    if (DEBUGOMA) {
    System.out.println("\nEV push no call stacks:");

    for (int i = 0; i < 10; i++)
      System.out.println(i + " " + stacks[i]);
    }

    double[] ICMpushArray = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    calcICM(stacks, ICMpushArray, 10, ICMs, ICMc);
    return ICMpushArray[myIndex];
  }

  // here betSum was passed by reference!!!! We modified the function to return it.
  private double calcStackBeforePush(int myIndex, int oppIndex, double[] stacks, double[][] playerData, double betSum) {
    double oppStack = stacks[oppIndex];
    double oppBets = playerData[oppIndex][1];
    double myStack = stacks[myIndex];
    double myBets = playerData[myIndex][1];

    double alku = oppStack + myStack + (betSum);
    //printf("%.1lf + %.1lf + %.1lf = %.1lf\n", oppStack, myStack, *betSum, alku);

    // if the opponent has paid more than I do
    if (oppBets > myBets) {
      // if we are able to pay for the difference
      if (myStack > oppBets - myBets) {
        myStack -= oppBets - myBets;
        betSum += (oppBets - myBets);
        myBets += oppBets - myBets;
      }
      // if the balance may not be able to pay, so be credited to the opponent betSum variable
      else {
        double erotus = oppBets - (myBets + myStack);
        betSum += myStack;
        myStack = 0;
        betSum -= erotus;
        oppStack += erotus;
      }
    }
    // if the opponent has paid less than I do, so he stäkin will be forced to be just the end of
    else {
      double erotus = myBets - oppBets;
      betSum -= erotus;
      myStack += erotus;
    }

    double loppu = oppStack + myStack + (betSum);
    /*
    if(alku!=loppu)
    {
        printf("%.1lf != %.1lf\n", alku, loppu);
        printf("%.1lf + %.1lf + %.1lf = %.1lf\n", oppStack, myStack, *betSum, loppu);
    }
    */
    stacks[myIndex] = myStack;
    stacks[oppIndex] = oppStack;

    return betSum;
  }


  private void calcSinglePush(int myIndex, int oppIndex, double[][] playerData, double[] ICMs, int ICMc) {

    // players gathered in private, and the sum of the blinds
    double betSum = 0;

    // stäkkejä deals with a temporary table
    double[] stacks = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    // EV win
    for (int i = 0; i < 10; i++) {
      stacks[i] = playerData[i][ STACK]-playerData[i][ BETS];
      betSum += playerData[i][ BETS];
    }

    // TODO: betsum was passed by ref here
    betSum = calcStackBeforePush(myIndex, oppIndex, stacks, playerData, betSum);
    double[][] stacksWin = {{2, stacks[myIndex], 0}, {1, stacks[oppIndex], 0}};
    calcStacks(stacksWin, betSum);
    stacks[myIndex] = stacksWin[0][2];
    stacks[oppIndex] = stacksWin[1][2];
    double[] ICMwinArray = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    calcICM(stacks, ICMwinArray, 10, ICMs, ICMc);
    playerData[oppIndex][ EVWIN] = ICMwinArray[myIndex];

    // EV lose
    betSum = 0;
    for (int i = 0; i < 10; i++) {
      stacks[i] = playerData[i][ STACK]-playerData[i][ BETS];
      betSum += playerData[i][ BETS];
    }

    // TODO: betsum was passed by ref here
    betSum = calcStackBeforePush(myIndex, oppIndex, stacks, playerData, betSum);

    double[][] stacksLose = {{1, stacks[myIndex], 0}, {2, stacks[oppIndex], 0}};
    calcStacks(stacksLose, betSum);
    stacks[myIndex] = stacksLose[0][ 2];
    stacks[oppIndex] = stacksLose[1][ 2];
    double[] ICMloseArray = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    calcICM(stacks, ICMloseArray, 10, ICMs, ICMc);
    playerData[oppIndex][ EVLOSE] = ICMloseArray[myIndex];
  }


  public void calcPush(int players, int myHandIndex, int myIndex, double[][] playerData, double[] results, double[] ICMs, int ICMc) {
    playersCount = players;

    int[] myHand = {0, 0};
    handFromIndex(myHandIndex, myHand);

    //System.Windows.Forms.MessageBox.Show("myHand[0] = " + Convert.ToString(myHand[0]) + " myHand[1] = " + Convert.ToString(myHand[1]));

    double EVpush = 0;

    double EVfold = calcEVfold(myIndex, playerData, 0, ICMs, ICMc);

    double EVnoCall = calcEVnoCall(myIndex, playerData, ICMs, ICMc);

    if (DEBUGOMA) {
      System.out.println("EV fold: " + EVfold * 100);
      System.out.println("EV no call " + EVnoCall * 100);
    }

    double calledBefore = 0;
    for (int i = myIndex - 1; i >= 0; i--) {
      oppPercentages(myHand, i, playerData, calledBefore);
      calcSinglePush(myIndex, i, playerData, ICMs, ICMc);
      calledBefore += playerData[i][ CALLPERC];
      EVpush += playerData[i][ CALLPERC]*(playerData[i][ WINPERC]*playerData[i][ EVWIN]+(1 - playerData[i][ WINPERC])* playerData[i][ EVLOSE]);
    }


    if (DEBUGOMA) {
    System.out.println("Index\tRange\tHold\tCall\tWin\tEV Win\tEV lose");
    System.out.println("-------------------------------------------------------");
    for (int i = 0; i < myIndex; i++)
      System.out.println(i + " " + playerData[i][CALLRANGE] + " " +
        playerData[i][ HOLDPERC]*100 + " " + playerData[i][ CALLPERC]*100 + " " +
            playerData[i][ WINPERC]*100 + " " +  playerData[i][ EVWIN]*100 + " " + playerData[i][ EVLOSE]*100);
    }
    EVpush += (1 - calledBefore) * EVnoCall;

    results[0] = EVfold;
    results[1] = EVpush;

    if (DEBUGOMA) {
    System.out.println("\nEV fold:" + EVfold * 100);
    System.out.println("EV push:" + EVpush * 100);
    System.out.println("EV diff:" +  (EVpush * 100 - EVfold * 100));
    }

  }


  public double calcPush(int players, int[] myHand, int myIndex, double[][] playerData, double[] ICMs, int ICMc) {
    playersCount = players;

    //int[] myHand = { 0, 0 };
    //handFromIndex(myHandIndex, myHand);

    //System.Windows.Forms.MessageBox.Show("myHand[0] = " + Convert.ToString(myHand[0]) + " myHand[1] = " + Convert.ToString(myHand[1]));

    double EVpush = 0;

    double EVfold = calcEVfold(myIndex, playerData, 0, ICMs, ICMc);
    //System.out.println("EV fold:\t{0:F2}%", EVfold * 100);

    double EVnoCall = calcEVnoCall(myIndex, playerData, ICMs, ICMc);
    //System.out.println("EV no call:\t{0:F2}%\n", EVnoCall * 100);

    double calledBefore = 0;
    for (int i = myIndex - 1; i >= 0; i--) {
      oppPercentages(myHand, i, playerData, calledBefore);
      calcSinglePush(myIndex, i, playerData, ICMs, ICMc);
      calledBefore += playerData[i][ CALLPERC];
      EVpush += playerData[i][ CALLPERC]*(playerData[i][ WINPERC]*playerData[i][ EVWIN]+(1 - playerData[i][ WINPERC])*
      playerData[i][ EVLOSE]);
    }

    //System.out.println("Index\tRange\tHold\tCall\tWin\tEV Win\tEV lose");
    //System.out.println("-------------------------------------------------------");
    //for (int i = 0; i < myIndex; i++)
    //    System.out.println("{0}\t{1:F2}%\t{2:F2}%\t{3:F2}%\t{4:F2}%\t{5:F2}%\t{6:F2}%\t", i, playerData[i, CALLRANGE], playerData[i, HOLDPERC] * 100, playerData[i, CALLPERC] * 100, playerData[i, WINPERC] * 100, playerData[i, EVWIN] * 100, playerData[i, EVLOSE] * 100);

    EVpush += (1 - calledBefore) * EVnoCall;

    //results[0] = EVfold;
    //results[1] = EVpush;

    return EVpush * 100 - EVfold * 100;

    //System.out.println("\nEV fold:\t{0:F2}", EVfold * 100);
    //System.out.println("EV push:\t{0:F2}", EVpush * 100);
    //System.out.println("EV diff:\t{0:F2}", EVpush * 100 - EVfold * 100);
  }


  private double myWinPercentage(int[] myHand, int oppIndex, double[][] playerData) {
    int lkm = 0;
    int total = 0;
    int crash = 0;
    int myHandIndex = 0;
    int oppHandIndex = 0;
    double myWinPercent = 0;
    double oppWinPercent = 0;

    // suited
    if (myHand[0] / 13 == myHand[1] / 13) {
      if (myHand[0] > myHand[1])
        myHandIndex = indexArray[myHand[0] % 13][ myHand[1] % 13];
      else
      myHandIndex = indexArray[myHand[1] % 13][ myHand[0] % 13];
    }
    // offsuit
    else {
      if (myHand[0] % 13 > myHand[1] % 13)
        myHandIndex = indexArray[myHand[1] % 13][ myHand[0] % 13];
      else
      myHandIndex = indexArray[myHand[0] % 13][ myHand[1] % 13];
    }

    for (int i = 0; i < 51; i++) {
      for (int a = i + 1; a < 52; a++) {
        total++;
        if (playerData[oppIndex][CALLRANGE]>=handValues[i][a]&&
        (myHand[0] != i && myHand[0] != a && myHand[1] != i && myHand[1] != a))
        {
          lkm++;

          // suited
          if (i / 13 == a / 13) {
            if (i > a)
              oppHandIndex = indexArray[i % 13][ a % 13];
            else
            oppHandIndex = indexArray[a % 13][ i % 13];
          }
          // offsuit
          else {
            if (i % 13 > a % 13)
              oppHandIndex = indexArray[a % 13][ i % 13];
            else
            oppHandIndex = indexArray[i % 13][ a % 13];
          }

          myWinPercent += evs[myHandIndex][ oppHandIndex];
          oppWinPercent += evs[oppHandIndex][ myHandIndex];
        }
        if (myHand[0] == i || myHand[0] == a || myHand[1] == i || myHand[1] == a)
          crash++;
      }
    }

    return myWinPercent / lkm;
  }

  public void calcCall(int players, int myHandIndex, int myIndex, int oppIndex, double[][] playerData, double[] results, double[] ICMs, int ICMc) {
    playersCount = players;
    //System.out.println("\n*****************  calcCall  *******************\n");
    //System.out.println("oppIndex: {0}\tmyIndex: {1}", oppIndex, myIndex);

    int[] myHand = {0, 0};
    handFromIndex(myHandIndex, myHand);


    double betSum = 0;
    double[] stacks = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    double EVfold = calcEVfold(myIndex, playerData, oppIndex, ICMs, ICMc);
    System.out.println("EV fold: " + EVfold * 100);

    // EV win
    for (int i = 0; i < 10; i++) {
      stacks[i] = playerData[i][ STACK]-playerData[i][ BETS];
      betSum += playerData[i][ BETS];
    }

    // TODO: fix ref
    betSum = calcStackBeforePush(myIndex, oppIndex, stacks, playerData, betSum);
    double[][] stacksWin = {{2, stacks[myIndex], 0}, {1, stacks[oppIndex], 0}};
    calcStacks(stacksWin, betSum);
    stacks[myIndex] = stacksWin[0][ 2];
    stacks[oppIndex] = stacksWin[1][ 2];
    double[] ICMwinArray = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    calcICM(stacks, ICMwinArray, 10, ICMs, ICMc);
    double EVwin = ICMwinArray[myIndex];

    // EV lose
    betSum = 0;
    for (int i = 0; i < 10; i++) {
      stacks[i] = playerData[i][ STACK]-playerData[i][ BETS];
      betSum += playerData[i][ BETS];
    }
    /*
    System.out.println("\nstacks before calcStackBeforePush() betSum={0}", betSum);
    for (int i = 0; i < 10; i++)
        System.out.println("{0:F2}", stacks[i]);
    System.out.println();
    */
    calcStackBeforePush(myIndex, oppIndex, stacks, playerData, betSum);
    /*
    System.out.println("\nstacks after calcStackBeforePush()");
    for (int i = 0; i < 10; i++)
        System.out.println("{0:F2}", stacks[i]);
    System.out.println();
    */
    double[][] stacksLose = {{1, stacks[myIndex], 0}, {2, stacks[oppIndex], 0}};
    calcStacks(stacksLose, betSum);
    stacks[myIndex] = stacksLose[0][ 2];
    stacks[oppIndex] = stacksLose[1][ 2];
    double[] ICMloseArray = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    calcICM(stacks, ICMloseArray, 10, ICMs, ICMc);
    /*
    System.out.println("ICMloseArray");
    for (int i = 0; i < 10; i++)
        System.out.println("{0:F0}\t{1:F2}", stacks[i], ICMloseArray[i]);
    */
    double EVlose = ICMloseArray[myIndex];

    //printf("EV call lose:\t%.2lf%%\n", ICMloseArray[myIndex]*100);

    // voittotodennäköisyys fee is paid,
    double winPercentage = myWinPercentage(myHand, oppIndex, playerData);
    double EVpush = winPercentage * EVwin + (1.0 - winPercentage) * EVlose;

    //printf("winPercentages[0]=%.2lf\n", winPercentage);
    results[0] = EVfold;
    results[1] = EVpush;

    playerData[myIndex][ WINPERC]=winPercentage;
    playerData[myIndex][ EVWIN]=EVwin;
    playerData[myIndex][ EVLOSE]=EVlose;

    //printf("EV call:\t%.2lf%%\n", EVpush*100);
    //printf("EV diff:\t%.2lf%%\n", EVpush*100-EVfold*100);

    if (DEBUGOMA) {
    System.out.println("\nEV call win: " + ICMwinArray[myIndex] * 100);
    System.out.println("EV call lose: " + ICMloseArray[myIndex] * 100);
    System.out.println("EV call: " + EVpush * 100);
    System.out.println("EV diff: " + (EVpush * 100 - EVfold * 100));

    }

  }


  public double calcCall(int players, int[] myHand, int myIndex, int oppIndex, double[][] playerData, double[] ICMs, int ICMc) {
    playersCount = players;
    //System.out.println("\n*****************  calcCall  *******************\n");
    //System.out.println("oppIndex: {0}\tmyIndex: {1}", oppIndex, myIndex);

    //double[] results = { 0, 0 };
    //int[] myHand = { 0, 0 };
    //handFromIndex(myHandIndex, myHand);


    double betSum = 0;
    double[] stacks = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    double EVfold = calcEVfold(myIndex, playerData, oppIndex, ICMs, ICMc);
    //System.out.println("EV fold:\t{0:F2}", EVfold * 100);

    // EV win
    for (int i = 0; i < 10; i++) {
      stacks[i] = playerData[i][ STACK]-playerData[i][ BETS];
      betSum += playerData[i][ BETS];
    }

    // TODO: betSum
    betSum = calcStackBeforePush(myIndex, oppIndex, stacks, playerData, betSum);
    double[][] stacksWin = {{2, stacks[myIndex], 0}, {1, stacks[oppIndex], 0}};
    calcStacks(stacksWin, betSum);
    stacks[myIndex] = stacksWin[0][ 2];
    stacks[oppIndex] = stacksWin[1][ 2];
    double[] ICMwinArray = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    calcICM(stacks, ICMwinArray, 10, ICMs, ICMc);
    //System.out.println("\nEV call win:\t{0:F2}%", ICMwinArray[myIndex] * 100);
    double EVwin = ICMwinArray[myIndex];

    // EV lose
    betSum = 0;
    for (int i = 0; i < 10; i++) {
      stacks[i] = playerData[i][ STACK]-playerData[i][ BETS];
      betSum += playerData[i][ BETS];
    }
    /*
    System.out.println("\nstacks before calcStackBeforePush() betSum={0}", betSum);
    for (int i = 0; i < 10; i++)
        System.out.println("{0:F2}", stacks[i]);
    System.out.println();
    */

    // TODO: ref betSum
    betSum = calcStackBeforePush(myIndex, oppIndex, stacks, playerData, betSum);
    /*
    System.out.println("\nstacks after calcStackBeforePush()");
    for (int i = 0; i < 10; i++)
        System.out.println("{0:F2}", stacks[i]);
    System.out.println();
    */
    double[][] stacksLose = {{1, stacks[myIndex], 0}, {2, stacks[oppIndex], 0}};
    calcStacks(stacksLose, betSum);
    stacks[myIndex] = stacksLose[0][ 2];
    stacks[oppIndex] = stacksLose[1][ 2];
    double[] ICMloseArray = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    calcICM(stacks, ICMloseArray, 10, ICMs, ICMc);
    /*
    System.out.println("ICMloseArray");
    for (int i = 0; i < 10; i++)
        System.out.println("{0:F0}\t{1:F2}", stacks[i], ICMloseArray[i]);
    */
    double EVlose = ICMloseArray[myIndex];
    //System.out.println("EV call lose:\t{0:F2}%", ICMloseArray[myIndex] * 100);
    //printf("EV call lose:\t%.2lf%%\n", ICMloseArray[myIndex]*100);

    // voittotodennäköisyys fee is paid,
    double winPercentage = myWinPercentage(myHand, oppIndex, playerData);
    double EVpush = winPercentage * EVwin + (1.0 - winPercentage) * EVlose;

    //printf("winPercentages[0]=%.2lf\n", winPercentage);
    //results[0] = EVfold;
    //results[1] = EVpush;
    //System.out.println("EV call:\t{0:F2}%", EVpush * 100);
    //System.out.println("EV diff:\t{0:F2}%", EVpush * 100 - EVfold * 100);

    playerData[myIndex][ WINPERC]=winPercentage;
    playerData[myIndex][ EVWIN]=EVwin;
    playerData[myIndex][ EVLOSE]=EVlose;

    //printf("EV call:\t%.2lf%%\n", EVpush*100);
    //printf("EV diff:\t%.2lf%%\n", EVpush*100-EVfold*100);

    return EVpush * 100 - EVfold * 100;
  }


  private void handFromIndex(int index, int[] cards) {
    //for (int i = 0; i < 169; i++)
    //{
    for (int a = 0; a < 13; a++) {
      for (int b = 0; b < 13; b++) {
        if (indexArray[a][b]==index)
        {
          // suited
          if (a > b) {
            cards[0] = a;
            cards[1] = b;
          } else if (b > a) {
            cards[0] = b;
            cards[1] = a + 13;
          } else if (a == b) {
            cards[0] = a;
            cards[1] = b + 13;
          } else
            System.out.println("Nyt joku kusi pahasti korteissa!");
          //System.out.println("{0} = [{1}][{2}]", i, a, b);
          //System.out.println("{0} = [{1}][{2}]", index, a, b);
          return;
        }
      }
    }
    //}
  }


  public void prepost(int players, double[][] playerData, double[] ICMs, int ICMc) {
    double[] stacks = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    for (int i = 0; i < 10; i++)
      stacks[i] = playerData[i][ STACK];

    double[] ICMprepostArray = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    calcICM(stacks, ICMprepostArray, 10, ICMs, ICMc);
    for (int i = 0; i < 10; i++)
      playerData[i][ PREPOST]=ICMprepostArray[i];
  }


  public ICM2() {
    if (evs == null) initData();
  }

  public static void main(String[] args) {
   ICM2 icm = new ICM2();

   // stackEV.cpp
   double[] iStacks = {10000,5000,5000,1000};
   double[] iPrizeEV = {0,0,0,0};
   int n = 4;
   double[] ICMs = {0.5,0.3,0.2};
   int ICMc = 3;
   icm.calcICM(iStacks, iPrizeEV, n, ICMs, ICMc);
   System.out.println("PrizeEVs: ");
   System.out.println("0: " + iPrizeEV[0] + " 1: " + iPrizeEV[1]+ " 2: " +  iPrizeEV[2]+ " 3: " +  iPrizeEV[3]);

   // stack.cpp
   int blinds = 100;
   double [][] iStacks2 = { {1,3000,0 }, {2,2000,0 } };
   icm.calcStacks(iStacks2, blinds);
   //System.out.println("{0:F2} {1:F2}", iStacks2[0,2], iStacks2[1,2]);

   // push.cpp
   int players = 4;
   int myHandIndex = 1;
   int myIndex = 3;
   double [][] playerDataPush = {
     {5000, 600, 40,0,0,0,0,0},
     {5000, 300, 10,0,0,0,0,0},
     {5000, 0, 25,0,0,0,0,0},
     {5000, 0, 10,0,0,0,0,0},
     {0, 0, 0,0,0,0,0,0},
     {0, 0, 0,0,0,0,0,0},
     {0, 0, 0,0,0,0,0,0},
     {0, 0, 0,0,0,0,0,0},
     {0, 0, 0,0,0,0,0,0},
     {0, 0, 0,0,0,0,0,0}};
     double[] results = {0,0};
     //double[] ICMs2 = {0.5, 0.3, 0.2};
     //int ICMc2 = 3;
   icm.calcPush(players, myHandIndex, myIndex, playerDataPush, results, ICMs, ICMc);
   System.out.println("EV Fold: " + (results[0] * 100) + " EV Push: " + (results[1] * 100) + " Diff: " +  ((results[1] - results[0]) * 100));

    myIndex = 0;
    int oppIndex = 1;
    double [][] playerDataCall = {
       {5000, 2075, 40,0,0,0,0,0},
       {5000, 5000, 50,0,0,0,0,0},
       {5000, 75, 25,0,0,0,0,0},
       {5000, 75, 10,0,0,0,0,0},
       {0, 0, 0,0,0,0,0,0},
       {0, 0, 0,0,0,0,0,0},
       {0, 0, 0,0,0,0,0,0},
       {0, 0, 0,0,0,0,0,0},
       {0, 0, 0,0,0,0,0,0},
       {0, 0, 0,0,0,0,0,0}};

 //   icm.calcCall(players, myHandIndex, myIndex, oppIndex, playerDataCall, results, ICMs, ICMc);
  }


  // this table is considered an index below presented evs-table
  private static short[][] indexArray = {
          {168, 167, 164, 159, 152, 143, 132, 119, 104, 87, 68, 47, 24},
          {166, 165, 162, 157, 150, 141, 130, 117, 102, 85, 66, 45, 22},
          {163, 161, 160, 155, 148, 139, 128, 115, 100, 83, 64, 43, 20},
          {158, 156, 154, 153, 146, 137, 126, 113, 98, 81, 62, 41, 18},
          {151, 149, 147, 145, 144, 135, 124, 111, 96, 79, 60, 39, 16},
          {142, 140, 138, 136, 134, 133, 122, 109, 94, 77, 58, 37, 14},
          {131, 129, 127, 125, 123, 121, 120, 107, 92, 75, 56, 35, 12},
          {118, 116, 114, 112, 110, 108, 106, 105, 90, 73, 54, 33, 10},
          {103, 101, 99, 97, 95, 93, 91, 89, 88, 71, 52, 31, 8},
          {86, 84, 82, 80, 78, 76, 74, 72, 70, 69, 50, 29, 6},
          {67, 65, 63, 61, 59, 57, 55, 53, 51, 49, 48, 27, 4},
          {46, 44, 42, 40, 38, 36, 34, 32, 30, 28, 26, 25, 2},
          {23, 21, 19, 17, 15, 13, 11, 9, 7, 5, 3, 1, 0}
  };

  // 52x52 -table, in order to get the hand is worth a scale of 1-100. This is on top of esitellyllä sklansky [13] [13] values of the table, using
  private static short[][] handValues = {
          {23, 95, 91, 89, 90, 87, 81, 76, 66, 56, 45, 32, 17, 23, 100, 99, 96, 98, 96, 91, 85, 77, 69, 55, 40, 23, 23, 100, 99, 96, 98, 96, 91, 85, 77, 69, 55, 40, 23, 23, 100, 99, 96, 98, 96, 91, 85, 77, 69, 55, 40, 23},
          {95, 15, 86, 84, 84, 81, 79, 73, 64, 54, 43, 32, 16, 100, 15, 95, 93, 94, 92, 90, 82, 74, 65, 52, 39, 22, 100, 15, 95, 93, 94, 92, 90, 82, 74, 65, 52, 39, 22, 100, 15, 95, 93, 94, 92, 90, 82, 74, 65, 52, 39, 22},
          {91, 86, 11, 77, 79, 76, 73, 70, 61, 51, 42, 30, 14, 99, 95, 11, 88, 88, 86, 84, 79, 71, 61, 50, 37, 20, 99, 95, 11, 88, 88, 86, 84, 79, 71, 61, 50, 37, 20, 99, 95, 11, 88, 88, 86, 84, 79, 71, 61, 50, 37, 20},
          {89, 84, 77, 10, 72, 70, 67, 65, 60, 50, 40, 29, 13, 96, 93, 88, 10, 83, 80, 78, 73, 68, 59, 47, 34, 18, 96, 93, 88, 10, 83, 80, 78, 73, 68, 59, 47, 34, 18, 96, 93, 88, 10, 83, 80, 78, 73, 68, 59, 47, 34, 18},
          {90, 84, 79, 72, 8, 66, 61, 59, 55, 49, 38, 27, 14, 98, 94, 88, 83, 8, 75, 72, 67, 62, 56, 45, 33, 19, 98, 94, 88, 83, 8, 75, 72, 67, 62, 56, 45, 33, 19, 98, 94, 88, 83, 8, 75, 72, 67, 62, 56, 45, 33, 19},
          {87, 81, 76, 70, 66, 8, 56, 53, 50, 44, 38, 26, 12, 96, 92, 86, 80, 75, 8, 66, 63, 58, 51, 44, 31, 15, 96, 92, 86, 80, 75, 8, 66, 63, 58, 51, 44, 31, 15, 96, 92, 86, 80, 75, 8, 66, 63, 58, 51, 44, 31, 15},
          {81, 79, 73, 67, 61, 56, 6, 46, 43, 40, 33, 25, 10, 91, 90, 84, 78, 72, 66, 6, 57, 53, 47, 41, 29, 13, 91, 90, 84, 78, 72, 66, 6, 57, 53, 47, 41, 29, 13, 91, 90, 84, 78, 72, 66, 6, 57, 53, 47, 41, 29, 13},
          {76, 73, 70, 65, 59, 53, 46, 5, 38, 34, 28, 23, 9, 85, 82, 79, 73, 67, 63, 57, 5, 48, 42, 35, 26, 11, 85, 82, 79, 73, 67, 63, 57, 5, 48, 42, 35, 26, 11, 85, 82, 79, 73, 67, 63, 57, 5, 48, 42, 35, 26, 11},
          {66, 64, 61, 60, 55, 50, 43, 38, 4, 26, 25, 15, 6, 77, 74, 71, 68, 62, 58, 53, 48, 4, 36, 30, 24, 8, 77, 74, 71, 68, 62, 58, 53, 48, 4, 36, 30, 24, 8, 77, 74, 71, 68, 62, 58, 53, 48, 4, 36, 30, 24, 8},
          {56, 54, 51, 50, 49, 44, 40, 34, 26, 3, 21, 12, 6, 69, 65, 61, 59, 56, 51, 47, 42, 36, 3, 27, 20, 7, 69, 65, 61, 59, 56, 51, 47, 42, 36, 3, 27, 20, 7, 69, 65, 61, 59, 56, 51, 47, 42, 36, 3, 27, 20, 7},
          {45, 43, 42, 40, 38, 38, 33, 28, 25, 21, 2, 10, 4, 55, 52, 50, 47, 45, 44, 41, 35, 30, 27, 2, 16, 4, 55, 52, 50, 47, 45, 44, 41, 35, 30, 27, 2, 16, 4, 55, 52, 50, 47, 45, 44, 41, 35, 30, 27, 2, 16, 4},
          {32, 32, 30, 29, 27, 26, 25, 23, 15, 12, 10, 1, 1, 40, 39, 37, 34, 33, 31, 29, 26, 24, 20, 16, 1, 2, 40, 39, 37, 34, 33, 31, 29, 26, 24, 20, 16, 1, 2, 40, 39, 37, 34, 33, 31, 29, 26, 24, 20, 16, 1, 2},
          {17, 16, 14, 13, 14, 12, 10, 9, 6, 6, 4, 1, 1, 23, 22, 20, 18, 19, 15, 13, 11, 8, 7, 4, 2, 1, 23, 22, 20, 18, 19, 15, 13, 11, 8, 7, 4, 2, 1, 23, 22, 20, 18, 19, 15, 13, 11, 8, 7, 4, 2, 1},
          {23, 100, 99, 96, 98, 96, 91, 85, 77, 69, 55, 40, 23, 23, 95, 91, 89, 90, 87, 81, 76, 66, 56, 45, 32, 17, 23, 100, 99, 96, 98, 96, 91, 85, 77, 69, 55, 40, 23, 23, 100, 99, 96, 98, 96, 91, 85, 77, 69, 55, 40, 23},
          {100, 15, 95, 93, 94, 92, 90, 82, 74, 65, 52, 39, 22, 95, 15, 86, 84, 84, 81, 79, 73, 64, 54, 43, 32, 16, 100, 15, 95, 93, 94, 92, 90, 82, 74, 65, 52, 39, 22, 100, 15, 95, 93, 94, 92, 90, 82, 74, 65, 52, 39, 22},
          {99, 95, 11, 88, 88, 86, 84, 79, 71, 61, 50, 37, 20, 91, 86, 11, 77, 79, 76, 73, 70, 61, 51, 42, 30, 14, 99, 95, 11, 88, 88, 86, 84, 79, 71, 61, 50, 37, 20, 99, 95, 11, 88, 88, 86, 84, 79, 71, 61, 50, 37, 20},
          {96, 93, 88, 10, 83, 80, 78, 73, 68, 59, 47, 34, 18, 89, 84, 77, 10, 72, 70, 67, 65, 60, 50, 40, 29, 13, 96, 93, 88, 10, 83, 80, 78, 73, 68, 59, 47, 34, 18, 96, 93, 88, 10, 83, 80, 78, 73, 68, 59, 47, 34, 18},
          {98, 94, 88, 83, 8, 75, 72, 67, 62, 56, 45, 33, 19, 90, 84, 79, 72, 8, 66, 61, 59, 55, 49, 38, 27, 14, 98, 94, 88, 83, 8, 75, 72, 67, 62, 56, 45, 33, 19, 98, 94, 88, 83, 8, 75, 72, 67, 62, 56, 45, 33, 19},
          {96, 92, 86, 80, 75, 8, 66, 63, 58, 51, 44, 31, 15, 87, 81, 76, 70, 66, 8, 56, 53, 50, 44, 38, 26, 12, 96, 92, 86, 80, 75, 8, 66, 63, 58, 51, 44, 31, 15, 96, 92, 86, 80, 75, 8, 66, 63, 58, 51, 44, 31, 15},
          {91, 90, 84, 78, 72, 66, 6, 57, 53, 47, 41, 29, 13, 81, 79, 73, 67, 61, 56, 6, 46, 43, 40, 33, 25, 10, 91, 90, 84, 78, 72, 66, 6, 57, 53, 47, 41, 29, 13, 91, 90, 84, 78, 72, 66, 6, 57, 53, 47, 41, 29, 13},
          {85, 82, 79, 73, 67, 63, 57, 5, 48, 42, 35, 26, 11, 76, 73, 70, 65, 59, 53, 46, 5, 38, 34, 28, 23, 9, 85, 82, 79, 73, 67, 63, 57, 5, 48, 42, 35, 26, 11, 85, 82, 79, 73, 67, 63, 57, 5, 48, 42, 35, 26, 11},
          {77, 74, 71, 68, 62, 58, 53, 48, 4, 36, 30, 24, 8, 66, 64, 61, 60, 55, 50, 43, 38, 4, 26, 25, 15, 6, 77, 74, 71, 68, 62, 58, 53, 48, 4, 36, 30, 24, 8, 77, 74, 71, 68, 62, 58, 53, 48, 4, 36, 30, 24, 8},
          {69, 65, 61, 59, 56, 51, 47, 42, 36, 3, 27, 20, 7, 56, 54, 51, 50, 49, 44, 40, 34, 26, 3, 21, 12, 6, 69, 65, 61, 59, 56, 51, 47, 42, 36, 3, 27, 20, 7, 69, 65, 61, 59, 56, 51, 47, 42, 36, 3, 27, 20, 7},
          {55, 52, 50, 47, 45, 44, 41, 35, 30, 27, 2, 16, 4, 45, 43, 42, 40, 38, 38, 33, 28, 25, 21, 2, 10, 4, 55, 52, 50, 47, 45, 44, 41, 35, 30, 27, 2, 16, 4, 55, 52, 50, 47, 45, 44, 41, 35, 30, 27, 2, 16, 4},
          {40, 39, 37, 34, 33, 31, 29, 26, 24, 20, 16, 1, 2, 32, 32, 30, 29, 27, 26, 25, 23, 15, 12, 10, 1, 1, 40, 39, 37, 34, 33, 31, 29, 26, 24, 20, 16, 1, 2, 40, 39, 37, 34, 33, 31, 29, 26, 24, 20, 16, 1, 2},
          {23, 22, 20, 18, 19, 15, 13, 11, 8, 7, 4, 2, 1, 17, 16, 14, 13, 14, 12, 10, 9, 6, 6, 4, 1, 1, 23, 22, 20, 18, 19, 15, 13, 11, 8, 7, 4, 2, 1, 23, 22, 20, 18, 19, 15, 13, 11, 8, 7, 4, 2, 1},
          {23, 100, 99, 96, 98, 96, 91, 85, 77, 69, 55, 40, 23, 23, 100, 99, 96, 98, 96, 91, 85, 77, 69, 55, 40, 23, 23, 95, 91, 89, 90, 87, 81, 76, 66, 56, 45, 32, 17, 23, 100, 99, 96, 98, 96, 91, 85, 77, 69, 55, 40, 23},
          {100, 15, 95, 93, 94, 92, 90, 82, 74, 65, 52, 39, 22, 100, 15, 95, 93, 94, 92, 90, 82, 74, 65, 52, 39, 22, 95, 15, 86, 84, 84, 81, 79, 73, 64, 54, 43, 32, 16, 100, 15, 95, 93, 94, 92, 90, 82, 74, 65, 52, 39, 22},
          {99, 95, 11, 88, 88, 86, 84, 79, 71, 61, 50, 37, 20, 99, 95, 11, 88, 88, 86, 84, 79, 71, 61, 50, 37, 20, 91, 86, 11, 77, 79, 76, 73, 70, 61, 51, 42, 30, 14, 99, 95, 11, 88, 88, 86, 84, 79, 71, 61, 50, 37, 20},
          {96, 93, 88, 10, 83, 80, 78, 73, 68, 59, 47, 34, 18, 96, 93, 88, 10, 83, 80, 78, 73, 68, 59, 47, 34, 18, 89, 84, 77, 10, 72, 70, 67, 65, 60, 50, 40, 29, 13, 96, 93, 88, 10, 83, 80, 78, 73, 68, 59, 47, 34, 18},
          {98, 94, 88, 83, 8, 75, 72, 67, 62, 56, 45, 33, 19, 98, 94, 88, 83, 8, 75, 72, 67, 62, 56, 45, 33, 19, 90, 84, 79, 72, 8, 66, 61, 59, 55, 49, 38, 27, 14, 98, 94, 88, 83, 8, 75, 72, 67, 62, 56, 45, 33, 19},
          {96, 92, 86, 80, 75, 8, 66, 63, 58, 51, 44, 31, 15, 96, 92, 86, 80, 75, 8, 66, 63, 58, 51, 44, 31, 15, 87, 81, 76, 70, 66, 8, 56, 53, 50, 44, 38, 26, 12, 96, 92, 86, 80, 75, 8, 66, 63, 58, 51, 44, 31, 15},
          {91, 90, 84, 78, 72, 66, 6, 57, 53, 47, 41, 29, 13, 91, 90, 84, 78, 72, 66, 6, 57, 53, 47, 41, 29, 13, 81, 79, 73, 67, 61, 56, 6, 46, 43, 40, 33, 25, 10, 91, 90, 84, 78, 72, 66, 6, 57, 53, 47, 41, 29, 13},
          {85, 82, 79, 73, 67, 63, 57, 5, 48, 42, 35, 26, 11, 85, 82, 79, 73, 67, 63, 57, 5, 48, 42, 35, 26, 11, 76, 73, 70, 65, 59, 53, 46, 5, 38, 34, 28, 23, 9, 85, 82, 79, 73, 67, 63, 57, 5, 48, 42, 35, 26, 11},
          {77, 74, 71, 68, 62, 58, 53, 48, 4, 36, 30, 24, 8, 77, 74, 71, 68, 62, 58, 53, 48, 4, 36, 30, 24, 8, 66, 64, 61, 60, 55, 50, 43, 38, 4, 26, 25, 15, 6, 77, 74, 71, 68, 62, 58, 53, 48, 4, 36, 30, 24, 8},
          {69, 65, 61, 59, 56, 51, 47, 42, 36, 3, 27, 20, 7, 69, 65, 61, 59, 56, 51, 47, 42, 36, 3, 27, 20, 7, 56, 54, 51, 50, 49, 44, 40, 34, 26, 3, 21, 12, 6, 69, 65, 61, 59, 56, 51, 47, 42, 36, 3, 27, 20, 7},
          {55, 52, 50, 47, 45, 44, 41, 35, 30, 27, 2, 16, 4, 55, 52, 50, 47, 45, 44, 41, 35, 30, 27, 2, 16, 4, 45, 43, 42, 40, 38, 38, 33, 28, 25, 21, 2, 10, 4, 55, 52, 50, 47, 45, 44, 41, 35, 30, 27, 2, 16, 4},
          {40, 39, 37, 34, 33, 31, 29, 26, 24, 20, 16, 1, 2, 40, 39, 37, 34, 33, 31, 29, 26, 24, 20, 16, 1, 2, 32, 32, 30, 29, 27, 26, 25, 23, 15, 12, 10, 1, 1, 40, 39, 37, 34, 33, 31, 29, 26, 24, 20, 16, 1, 2},
          {23, 22, 20, 18, 19, 15, 13, 11, 8, 7, 4, 2, 1, 23, 22, 20, 18, 19, 15, 13, 11, 8, 7, 4, 2, 1, 17, 16, 14, 13, 14, 12, 10, 9, 6, 6, 4, 1, 1, 23, 22, 20, 18, 19, 15, 13, 11, 8, 7, 4, 2, 1},
          {23, 100, 99, 96, 98, 96, 91, 85, 77, 69, 55, 40, 23, 23, 100, 99, 96, 98, 96, 91, 85, 77, 69, 55, 40, 23, 23, 100, 99, 96, 98, 96, 91, 85, 77, 69, 55, 40, 23, 23, 95, 91, 89, 90, 87, 81, 76, 66, 56, 45, 32, 17},
          {100, 15, 95, 93, 94, 92, 90, 82, 74, 65, 52, 39, 22, 100, 15, 95, 93, 94, 92, 90, 82, 74, 65, 52, 39, 22, 100, 15, 95, 93, 94, 92, 90, 82, 74, 65, 52, 39, 22, 95, 15, 86, 84, 84, 81, 79, 73, 64, 54, 43, 32, 16},
          {99, 95, 11, 88, 88, 86, 84, 79, 71, 61, 50, 37, 20, 99, 95, 11, 88, 88, 86, 84, 79, 71, 61, 50, 37, 20, 99, 95, 11, 88, 88, 86, 84, 79, 71, 61, 50, 37, 20, 91, 86, 11, 77, 79, 76, 73, 70, 61, 51, 42, 30, 14},
          {96, 93, 88, 10, 83, 80, 78, 73, 68, 59, 47, 34, 18, 96, 93, 88, 10, 83, 80, 78, 73, 68, 59, 47, 34, 18, 96, 93, 88, 10, 83, 80, 78, 73, 68, 59, 47, 34, 18, 89, 84, 77, 10, 72, 70, 67, 65, 60, 50, 40, 29, 13},
          {98, 94, 88, 83, 8, 75, 72, 67, 62, 56, 45, 33, 19, 98, 94, 88, 83, 8, 75, 72, 67, 62, 56, 45, 33, 19, 98, 94, 88, 83, 8, 75, 72, 67, 62, 56, 45, 33, 19, 90, 84, 79, 72, 8, 66, 61, 59, 55, 49, 38, 27, 14},
          {96, 92, 86, 80, 75, 8, 66, 63, 58, 51, 44, 31, 15, 96, 92, 86, 80, 75, 8, 66, 63, 58, 51, 44, 31, 15, 96, 92, 86, 80, 75, 8, 66, 63, 58, 51, 44, 31, 15, 87, 81, 76, 70, 66, 8, 56, 53, 50, 44, 38, 26, 12},
          {91, 90, 84, 78, 72, 66, 6, 57, 53, 47, 41, 29, 13, 91, 90, 84, 78, 72, 66, 6, 57, 53, 47, 41, 29, 13, 91, 90, 84, 78, 72, 66, 6, 57, 53, 47, 41, 29, 13, 81, 79, 73, 67, 61, 56, 6, 46, 43, 40, 33, 25, 10},
          {85, 82, 79, 73, 67, 63, 57, 5, 48, 42, 35, 26, 11, 85, 82, 79, 73, 67, 63, 57, 5, 48, 42, 35, 26, 11, 85, 82, 79, 73, 67, 63, 57, 5, 48, 42, 35, 26, 11, 76, 73, 70, 65, 59, 53, 46, 5, 38, 34, 28, 23, 9},
          {77, 74, 71, 68, 62, 58, 53, 48, 4, 36, 30, 24, 8, 77, 74, 71, 68, 62, 58, 53, 48, 4, 36, 30, 24, 8, 77, 74, 71, 68, 62, 58, 53, 48, 4, 36, 30, 24, 8, 66, 64, 61, 60, 55, 50, 43, 38, 4, 26, 25, 15, 6},
          {69, 65, 61, 59, 56, 51, 47, 42, 36, 3, 27, 20, 7, 69, 65, 61, 59, 56, 51, 47, 42, 36, 3, 27, 20, 7, 69, 65, 61, 59, 56, 51, 47, 42, 36, 3, 27, 20, 7, 56, 54, 51, 50, 49, 44, 40, 34, 26, 3, 21, 12, 6},
          {55, 52, 50, 47, 45, 44, 41, 35, 30, 27, 2, 16, 4, 55, 52, 50, 47, 45, 44, 41, 35, 30, 27, 2, 16, 4, 55, 52, 50, 47, 45, 44, 41, 35, 30, 27, 2, 16, 4, 45, 43, 42, 40, 38, 38, 33, 28, 25, 21, 2, 10, 4},
          {40, 39, 37, 34, 33, 31, 29, 26, 24, 20, 16, 1, 2, 40, 39, 37, 34, 33, 31, 29, 26, 24, 20, 16, 1, 2, 40, 39, 37, 34, 33, 31, 29, 26, 24, 20, 16, 1, 2, 32, 32, 30, 29, 27, 26, 25, 23, 15, 12, 10, 1, 1},
          {23, 22, 20, 18, 19, 15, 13, 11, 8, 7, 4, 2, 1, 23, 22, 20, 18, 19, 15, 13, 11, 8, 7, 4, 2, 1, 23, 22, 20, 18, 19, 15, 13, 11, 8, 7, 4, 2, 1, 17, 16, 14, 13, 14, 12, 10, 9, 6, 6, 4, 1, 1}};


  //   For example, . valueToHand[20,0] the first dealt top 20% of EV's hand
  // if (0.0) such as the hand does not exist. Done printEVtoHandArray method
  public int[][] valueToHand = {{0, 0}, {11, 12}, {10, 23}, {9, 22}, {8, 21}, {7, 20}, {6, 19}, {9, 25}, {4, 17}, {7, 12}, {3, 16}, {2, 15}, {5, 12}, {3, 12}, {2, 12}, {1, 14}, {1, 12}, {0, 12}, {3, 25}, {4, 25}, {2, 25}, {9, 10}, {1, 25}, {0, 13}, {8, 24}, {6, 11}, {5, 11}, {4, 11}, {7, 10}, {3, 11}, {2, 11}, {5, 24}, {0, 11}, {4, 24}, {3, 24}, {7, 23}, {8, 22}, {2, 24}, {4, 10}, {1, 24}, {0, 24}, {6, 23}, {2, 10}, {1, 10}, {5, 9}, {0, 10}, {6, 7}, {3, 23}, {7, 21}, {4, 9}, {2, 23}, {2, 9}, {1, 23}, {5, 7}, {1, 9}, {0, 23}, {0, 9}, {6, 20}, {5, 21}, {3, 22}, {3, 8}, {2, 8}, {4, 21}, {5, 20}, {1, 8}, {1, 22}, {0, 8}, {3, 6}, {3, 21}, {0, 22}, {2, 7}, {2, 21}, {3, 4}, {1, 7}, {1, 21}, {4, 18}, {0, 7}, {0, 21}, {3, 19}, {1, 6}, {3, 18}, {0, 6}, {1, 20}, {3, 17}, {1, 3}, {0, 20}, {1, 2}, {0, 5}, {2, 16}, {0, 3}, {0, 4}, {0, 2}, {1, 18}, {1, 16}, {1, 17}, {0, 1}, {0, 16}, {0, 17}, {0, 17}, {0, 15}, {0, 14}};


  // to restore the hand above the table in
  public int[] handIndexToHand(int handIndex) {
    int[] hand = new int[2];
    while (valueToHand[handIndex][0]==0 && valueToHand[handIndex][ 1]==0)
    handIndex++;
    hand[0] = valueToHand[handIndex][ 0];
    hand[1] = valueToHand[handIndex][ 1];
    return hand;
  }

  public static double[][] evs = null;

  public static void initData() {
    evs = new double[169][169];
    try {
      RandomAccessFile f = new RandomAccessFile(data, "r");
      for (int i = 0; i< 169; i++) {
        String s = f.readLine();
        StringTokenizer st = new StringTokenizer(s);
        for (int j = 0; j < 169; j++) evs[i][j] = Double.parseDouble((String)st.nextElement());
      }
      f.close();
    } catch (Exception e) {
      System.out.println("Problem while reading EV data from file " + data);
      e.printStackTrace();
      evs = null;
    }
  }
}

