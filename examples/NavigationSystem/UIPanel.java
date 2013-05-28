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

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Timer;

class UIPanel extends JPanel {
	private Position currentPosition = new Position(0);
	private Position currentDestination = new Position(0);
	private Route currentRoute;
	private boolean inProgress;
	private JPanel mapDisplay;
	private JButton confirmDestinationButton;
	private JLabel nextTurn;
	private JTextField routeText;
	private String routeString;
	private Timer timer;

	private RouteCalc rc;

	public UIPanel(RouteCalc adapter) {
		this.rc = adapter;
		inProgress = false;
		mapDisplay = new MapDisplay();
		mapDisplay.setPreferredSize(new Dimension(500, 200));
		confirmDestinationButton = new JButton("Set Destination");
		nextTurn = new JLabel();
		routeText = new JTextField(10);
		confirmDestinationButton
				.addActionListener(new DestinationButtonListener());
		timer = new Timer(200, new TimerCallback());
		routeString = "No route selected";
		// lay out the pieces vertically
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		this.add(mapDisplay);
		this.add(nextTurn);
		JPanel bottomPanel = new JPanel();
		bottomPanel.add(routeText);
		bottomPanel.add(confirmDestinationButton);
		this.add(bottomPanel);
	}

	public void doSetPosition(Position p) {
		currentPosition = p;
		repaint();
	}

	public void doSetNewRoute(final Route route) {
		timer.stop();
		inProgress = false;
		currentRoute = route;
		routeString = "Route: " + route.toString();
		confirmDestinationButton.setEnabled(true);
		repaint();
	}

	public void doAnnounceNextTurn(final Instruction inst) {
		if (!inProgress) 
			nextTurn.setText(inst.decode());
		repaint();
	}

	// listener attached to confirmDestinationButton...
	class DestinationButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			try {
				inProgress = true;
				String routeString = routeText.getText();
				int dest = Integer.parseInt(routeString);
				nextTurn.setText("Calculating route");
				currentDestination = new Position(dest);
				confirmDestinationButton.setEnabled(false);
				System.out.println("button pressed");
				rc.calculateRoute(currentDestination);
				timer.start();
				repaint();
			} catch (NumberFormatException nfe) {
				// do nothing
				inProgress = false;
			}
		}
	}

	/**
	 * Pretends to draw a map showing current position and route...
	 */
	class MapDisplay extends JPanel {
		public void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g;
			g2.setBackground(Color.WHITE);
			g2.clearRect(0, 0, getWidth(), getHeight());
			g2.drawRect(0, 0, getWidth(), getHeight());
			g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 24));
			String pos = "Current position: " + currentPosition.getCoordinate();
			String dest = "Current destination: " + currentDestination.getCoordinate();
			g2.drawString(pos, 30, 30);
			g2.drawString(dest, 30, 60);
			g2.drawString(routeString, 30, 90);
		}
	}

	class TimerCallback implements ActionListener {
		private int animationState = 0;
		private final int MAX = 6;
		public void actionPerformed(ActionEvent e) {
			String text = "Calculating route";
			for (int i = 0; i < animationState; ++i)
				text += ".";
			animationState = (animationState + 1) % 6;
			routeString = text;
			repaint();
		}
	}
} // UIPanel
