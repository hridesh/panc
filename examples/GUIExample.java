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
 * Contributor(s): Steven M. Kautz, Hridesh Rajan
 */

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JFrame;

/**
 * Simple GUI example in Panini. Each time the button is clicked, the 
 * label updates to show the total number of clicks. 
 */
capsule GUIExample {

	class LabelPanel extends JPanel {
		private JButton button;   //Swing components
		private JLabel label;
		private int count; //The number of times the button has been clicked.

		public LabelPanel() { //Create all components contained in the panel
			label = new JLabel("Push this button! "); //create a label with some initial text
			this.add(label); //add label to the panel
			button = new JButton("Push me"); //create a button
			ActionListener myListener = new MyButtonListener(); 
			button.addActionListener(myListener); //add an ActionListener to button's list of listeners
			this.add(button); //add the button to the panel
		}

		public void setCounter(int newCount) {
			count = newCount;
		}

		private class MyButtonListener implements ActionListener {
			@Override
			public void actionPerformed(ActionEvent event) {       
				System.out.println("Button pushed, incrementing count");
				++count;
				System.out.println("Count incremented");
				label.setText("Pushed " + count + " times: ");
			}
		}
	} //End of class definition for LabelPanel

	LabelPanel panel = null;
	JFrame frame = null;

	void run() {
		Runnable r = new Runnable() {
			public void run() {
				frame = new JFrame("GUI Example in Panini"); //create the frame
				panel = new LabelPanel(); //create an instance of our JPanel subclass, and
				frame.getContentPane().add(panel); //add it to the frame.
				frame.setSize(300, 100); //give the frame nonzero size

				// we want to shut down the application if the 
				// "close" button is pressed on the frame
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

				// make the frame visible and start the UI machinery
				frame.setVisible(true);
			}
		};
		java.awt.EventQueue.invokeLater(r);
	}

	void setCounter(final int arg) {
		Runnable r = new Runnable() {
			public void run() {
				panel.setCounter(arg);
			}
		};
		java.awt.EventQueue.invokeLater(r);
	}
}
