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

//A simplified version of the classic arcade game Asteroids.
capsule Controller(Ship ship){
	void run(){
		while(ship.alive()) 
			switch(System.in.read()) {
				case 106: ship.moveLeft(); break;
				case 108: ship.moveRight(); break;
			 case 105: ship.fire(); break;
			}
	}
}

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

capsule Space(Ship s) {
	void run() {
		int lastFired = -1, points = 0, asteroidPos = -1;
		while(s.alive()) {
			yield(1000);
			int shipPos = s.getPos();
			boolean isFiring = s.isFiring(); 
			if(asteroidPos == lastFired) points++;
			else if (asteroidPos == shipPos) s.die();
			if(isFiring) lastFired = shipPos;
			else lastFired = -1;
			Screen.repaint(shipPos, isFiring, asteroidPos, lastFired, points, asteroidPositions);
			asteroidPos = nextAsteroid();
		}
		Screen.endGame(); 
	}
	int[] asteroidPositions = new int[] {-1,-1,-1,-1,-1,-1,-1,-1,-1,-1};
	private int nextAsteroid() {
		for(int i=9; i>0; i--)
			asteroidPositions[i] = asteroidPositions[i-1];
		asteroidPositions[0] = prng.nextInt(10);
		return asteroidPositions[9];
	}
	Random prng = new Random ();
}

system Asteroids {
	Controller c; Ship s; Space p;
	c(s); p(s);
}

class Screen {
	static void repaint(int shipPos, boolean isFiring, int asteroidPos, int lastFired, int points, int[] asteroidPositions) {
		paintHorizBorder();
		for(int i = 0; i<WIDTH; i++) {
			for(int j = 0; j<HEIGHT-1; j++) {
				if(j == asteroidPositions[i]) 
					System.out.print('@');
				else 	System.out.print(' ');
			}
			System.out.print('\n');
		}
		for(int i = 0; i<=10; i++) {
			if(i == asteroidPos) {
				if (i == lastFired) {
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
	static void endGame() {
		System.out.println("Game ended. Press any key to exit.");
	}
	static void paintHorizBorder() {
		for(int i = 0; i<=WIDTH; i++)
			System.out.print('-');
		System.out.println("");
	}
	static final int HEIGHT = 10;
	static final int WIDTH = 10;
}