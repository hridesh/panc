/*
 * This file is part of the Panini project at Iowa State University.
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 * 
 * For more details and the latest version of this code please see
 * http://paninij.org
 * 
 * Contributor(s):
 */

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;


interface RouteCalc {
  void calculateRoute(Position dst);
}

capsule NavSystem {
 design {
  ManeuverGen mg;
  FakeDataGenerator gen;
  RouteCalculator rc;
  UI ui;
  GPS gps;
  
  mg(ui);
  rc(mg, gen);
  gps(mg, gen);
  ui(rc);
 }
}

capsule ManeuverGen(UI ui) {
  Route currentRoute = null;
  Position currentPosition = null;
  Random rand = new Random();
  
  void setNewRoute(Route r) {
    currentRoute = r;
    if (r != null) {
      ui.setNewRoute(r);
    }
  }
  void updatePosition(Position p) {
    currentPosition = p;
    ui.setPosition(p);
    Instruction inst = nextManeuver();
    if (inst != null) 
      ui.announceNextTurn(inst);
  }
  Position getCurrentPosition() {
    return new Position(currentPosition.getCoordinate());
  }
  private Instruction nextManeuver() {
    if (currentRoute == null) {
      return new Instruction("No route selected");
    } else if (currentRoute.getDest().getCoordinate() == 
    		currentPosition.getCoordinate()) {
      return new Instruction("Arrived!");
    } else {
      // make up some instructions
      String dir = rand.nextInt(2) == 0 ? "left" : "right";
      int dist = 100 * (rand.nextInt(4) + 1);
      return new Instruction("Turn " + dir + " in " + dist + " meters");
    }
  }
}

capsule RouteCalculator(ManeuverGen mg, FakeDataGenerator gen) {
  void calculateRoute(Position dst) {
    mg.setNewRoute(null);
    gen.notMoving();
    Route r = doCalculate(mg.getCurrentPosition(), dst);
    mg.setNewRoute(r);
    gen.setDest(dst.getCoordinate());
  }
  // This operation may take a long time
  private Route doCalculate(Position src, Position dst) {
    lookBusy(5000);
    return new Route(src, dst);
  }
  private static void lookBusy(long millis) {
    long interval = 300;
    long stop = System.currentTimeMillis() + millis;

    try {
      while (System.currentTimeMillis() < stop) {
        System.out.print(".");
        yield(interval);
      }
    } catch (InterruptedException ie) { }
  }
}

capsule GPS(ManeuverGen mg, FakeDataGenerator gen) {
  void run() {
    while (true) {
      try {
        yield(2000);
      } catch (InterruptedException cantHappen){}
      
      Position p = gen.readData();
      mg.updatePosition(p);
    }
  }
}

//since we don't have a real satellite or real car
capsule FakeDataGenerator {
  int pos = 0;
  int dest = 0;
  void notMoving() {
    dest = pos;
  }
  void setDest(int destination) {
    dest = destination;
  }
  public Position readData() {
    // pretend to get closer to "destination"
    if (pos < dest) ++pos;
    else if (pos > dest) --pos;
    return new Position(pos);
  }
}

capsule UI(RouteCalculator rc) {
  UIPanel panel = null;
  JFrame frame = null;  
  void run() {
    Runnable r = new Runnable() {
      public void run() {
        // create the panel and frame
        frame = new JFrame("Nav system");
        panel = new UIPanel(new RouteCalcAdapter());
        frame.getContentPane().add(panel);

        // use the preferred sizes
        frame.pack();

        // we want to shut down the application if the 
        // "close" button is pressed on the frame
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // make the frame visible and start the UI machinery
        frame.setVisible(true);
      }
    };
    SwingUtilities.invokeLater(r);
  }
  void setPosition(final Position p) {
    Runnable r = new Runnable() {
      public void run() {
        panel.doSetPosition(p);
      }
    };
    SwingUtilities.invokeLater(r);
  }
  void setNewRoute(final Route route) {
    Runnable r = new Runnable() {
      public void run() {
        panel.doSetNewRoute(route);
      }
    };
    SwingUtilities.invokeLater(r);
  }  
  public void announceNextTurn(final Instruction inst) {
    Runnable r = new Runnable() {
      public void run() {
        panel.doAnnounceNextTurn(inst);
      }
    };
    SwingUtilities.invokeLater(r);
  }
  class RouteCalcAdapter implements RouteCalc {
    public void calculateRoute(Position dst) {
      rc.calculateRoute(dst);
    }
  }
}

class Position {
  private final int p;
  public Position(int p) {
    this.p = p;
  }
  public int getCoordinate() {
    return p;
  }  
  @Override
  public String toString() {
    return "" + p;
  }
}

class Route {
	private final Position src;
	private final Position dst;
	public Route(Position src, Position dst) {
		this.src = src;
		this.dst = dst;
	}
	@Override
	public String toString() {
		return "From " + src + " to " + dst;
	}
	public Position getSource()	{
		return src;
	}
	public Position getDest() {
		return dst;
	}
}

class Instruction {
	private final String inst;
	public Instruction(String inst)	{
		this.inst = inst;
	}
	String decode()	{
		return inst;
	}
}

