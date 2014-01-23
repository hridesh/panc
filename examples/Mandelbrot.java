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
 * Adam Campbell
 */

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

 
/**
 * FractalProcessor signature, defines an interface for lengthy calculation needing to be completed
 * @author Adam Campbell
 */
signature FractalProcessor {
	int[] calculateResult(double realMin, double realMax, double imgMin, double imgMax, int realRes, int imgRes, int maxIterations);
}

/**
 * FractalProcessor implementation, performs the calculation of the Mandelbrot fractal
 * @author Adam Campbell
 */
capsule FractalProcessorImpl() implements FractalProcessor {
	int[] calculateResult(double realMin, double realMax, double imgMin, double imgMax, int realRes, int imgRes, int maxIterations) {
		if (realMax < realMin || imgMax < imgMin) {
			throw new IllegalArgumentException("Error: max must be >= min");
		}
		
		int[] iterations = new int[realRes * imgRes];
		
		// Iterate through every pixel and fill the iterations array
		for (int i=0; i < realRes; i++) {
			for (int j=0; j < imgRes; j++) {
				double re = realMin + ((double)i/realRes)*(realMax - realMin);
				double im = imgMin + ((double)j/imgRes)*(imgMax - imgMin);
				int iter = getIterations(re, im, maxIterations);
				iterations[j*realRes + i] = iter;
			}
		}
		
		return iterations;
	}
	
	/**
	 * Get the number of iterations for convergence of the specified complex number 
	 * 
	 * Code adapted from Bob Boothby's code :
	 * http://bbboblog.blogspot.com/2009/10/gpgpu-mandelbrot-with-opencl-and-java.html
	 */
	int getIterations(double re, double im, int maxIterations) {
		double resquared = re * re;
		double imsquared = im * im;
		double valx = re;
		double valy = im;

		int iter = 0;
		while ( (iter < maxIterations) && ((resquared + imsquared) < 4) ) {
			valy = (2 * (valx * valy));
			valx = resquared - imsquared;
			valx += re;
			valy += im;
			resquared = valx * valx;
			imsquared = valy * valy;

			iter++;
		}
		
		return iter;
	}
}

/**
 * FractalApp class, application to compute & display the Mandelbrot fractal
 * @author Adam Campbell
 */
capsule FractalApp(UI ui, FractalProcessor processor) {
	void go() {
		int imgWidth = 700;
		int imgHeight = 700;
		int maxIterations = 64;
		
		double realMin = -1.75;
		double realMax = .75;
		double imgMin = -1.25;
		double imgMax = 1.25;
		int realRes = imgWidth;
		int imgRes = imgHeight;
	
		// Compute results
		long startTime = System.currentTimeMillis();
		int[] iterations = processor.calculateResult(realMin, realMax, imgMin, imgMax, realRes, imgRes, maxIterations);

		// Display results
		ui.display(iterations, maxIterations, imgWidth, imgHeight);
	}
}

/**
 * UI for displaying the fractal image in a JFrame
 * @author Adam Campbell
 */
capsule UI() {
	void display(int[] iterations, int maxIterations, int imgWidth, int imgHeight) {
		//Go through each pixel and convert its iterations into a color
		int[] imageData = new int[iterations.length];
		for (int i=0; i < imageData.length; i++) {
			if (iterations[i] >= maxIterations) {
				// Idea to use color scheme, but very slow?
				
				//imageData[i] = colorScheme.getInMandelbrotSetColor();
				imageData[i] = 0x00aaaa;
			} else {
				//imageData[i] = colorScheme.getColorForIterations(iterations[i], maxIterations);
				imageData[i] = (int) (((double) iterations[i] / maxIterations) * 200);
			}
		}
			
		//Create an image from the image data
		BufferedImage image = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);
		image.setRGB(0, 0, imgWidth, imgHeight, imageData, 0, imgWidth);

		update(image);
	}
	
	/// Center the JFrame
	void center() {
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Dimension scrSize = Toolkit.getDefaultToolkit().getScreenSize();
		f.setLocation((scrSize.width - f.getWidth())/2, (scrSize.height - f.getHeight())/2);
		f.setVisible(true);
	}
	
	/// Update the frame & display the image
	void update(BufferedImage image) {
		f.getContentPane().removeAll();
		f.getContentPane().add(new JScrollPane(new JLabel(new ImageIcon(image))));
		f.pack();
		
		center();
	}
	
	/// JFrame for the UI
	JFrame f = new JFrame("Mandelbrot");;
}

/**
 * Main application, run this
 * @author Adam Campbell
 */
capsule Mandelbrot() {
	design {
		FractalApp fractalApp;
		UI ui;
		FractalProcessorImpl processor;
		
		fractalApp(ui, processor);
	}

	void run() {
		fractalApp.go();
	}
}