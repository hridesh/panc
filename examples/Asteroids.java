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
 * Contributor(s): Hridesh Rajan
 */

import java.util.*;
import java.io.*;

capsule Ship {
	short state = 0;
	void die() { state = 2; }
	void fire() { state = 1; }
	boolean alive() { return state != 2; }
	boolean isFiring() { 
		if(state == 1) { state = 0; return true; }
		return false;
	}
	int x = 5; 
	int getPos() { return x; }
	void moveLeft() { if (x>0) x--; }
	void moveRight() { if (x<10) x++; }
}

capsule Asteroids {
	design {
		Ship s; UI ui; Game g; Input i;
		ui(g); i(s);
	}
	void run() {
		int points = 0;
		while(s.alive()) {
			int shipPos = s.getPos();
			boolean isFiring = s.isFiring(); 
			int result = g.step(shipPos, isFiring);
			if(result>0) points += result;
			else if (result<0) s.die();
			ui.repaint(shipPos, isFiring, points);
			yield(1000);
		}
		ui.endGame(); 
	}
}

capsule Input (Ship ship) {
	void run(){
		try {
			while(ship.alive())
				switch(System.in.read()) {
					case 106: ship.moveLeft(); break;
					case 108: ship.moveRight(); break;
					case 105: ship.fire(); break;
				}
		} catch (IOException ioe) {}
	}
}

capsule Game () {
	int step(int shipPos, boolean isFiring){
		int result = 0;
		if(asteroidPos == lastFired) result = 1;
		else if (asteroidPos == shipPos) result = -1;
		if(isFiring) lastFired = shipPos;
		else lastFired = -1;
		asteroidPos = nextAsteroid();
		return result;
	}
	short[] asteroidPositions = new short[] {-1,-1,-1,-1,-1,-1,-1,-1,-1,-1};
	private int nextAsteroid() {
		for(int i=9; i>0; i--)
			asteroidPositions[i] = asteroidPositions[i-1];
		asteroidPositions[0] = (short) prng.nextInt(10);
		return asteroidPositions[9];
	}
	short getAsteroidPosition(int index) { return asteroidPositions[index]; }
	int asteroidPos = -1;
	int lastFired = -1;
	int getLastFired() { return lastFired; }
	Random prng = new Random ();
}

capsule UI (Game g){
	void repaint(int shipPos, boolean isFiring, int points) {
		paintHorizBorder();
		for(int i = 0; i<Constants.WIDTH; i++) {
			for(int j = 0; j<Constants.HEIGHT-1; j++) {
				if(j == g.getAsteroidPosition(i)) 
					System.out.print('@');
				else 	System.out.print(' ');
			}
			System.out.print('\n');
		}
		for(int i = 0; i<=10; i++) {
			if(i == g.getAsteroidPosition(Constants.HEIGHT-1)) {
				if (i == g.getLastFired()) {
					System.out.print('#');
				} else if (i == shipPos) {
					System.out.print('X');
				} else 	System.out.print('@');
			} else 	if (i == shipPos) {
				if(isFiring) {
					System.out.print('*');
				} else {
					System.out.print('^');
				}
			} else		System.out.print(' ');
		}
		System.out.print('\n');
		paintHorizBorder();
	}
	
	void endGame() {
		System.out.println("Game ended. Press any key to exit.");
	}
	
	private void paintHorizBorder() {
		for(int i = 0; i<=Constants.WIDTH; i++)
			System.out.print('-');
		System.out.println("");
	}
}

class Constants {
	static final int HEIGHT = 10;
	static final int WIDTH = 10;
}
