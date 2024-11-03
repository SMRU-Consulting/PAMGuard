package simulatedAcquisition.sounds;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class RandomPolynomials extends SimSignal{
	
	private double[] lengthR = {0.5, .9};
	private double[] meanR = {4000, 12000};
	private double[] zeroA = {-2, .43};
	private double[] zeroB = {3.3, 4.5};
	private double[] zeroC = {1.5, 2};
	private boolean addHarmonics;

	private Random r = new Random();
	private double[] harmonicDampingR = {1,25};
	
	private double length, a, b, c, d, damp;
		
	public RandomPolynomials(boolean addHarmonics) {
		super();
		this.addHarmonics = addHarmonics;
	}

	@Override
	public String getName() {
		return "Harmonic Whistles";
	}

	@Override
	public void prepareSignal() {
		length = getRandom(lengthR);
		a = getRandom(zeroA);
		b = getRandom(zeroB);
		c = getRandom(zeroC);
		d = getRandom(meanR);

		damp = getRandom(harmonicDampingR);
		if (b > 0) { // if it starts by going up, make it curve the other way
//			c = -c;
		}
	}
	

	@Override
	public double[] getSignal(int channel, float sampleRate, double sampleOffset) {

		// check the total range of the signal. 
		double df = Math.abs(b*length + c*length*length);
		// generate start freq so it has to stay in range
		double[] startRange = new double[2];
//		startRange[0] = df + sampleRate / 20;
//		startRange[1] = sampleRate/2-startRange[0];
//		a = getRandom(startRange);
		
		int nSamp = (int) (length * sampleRate);
		double[] sound = new double[nSamp];
		double t, phase, sweep, f;
		for (int i = 0; i < nSamp; i++) {
			t = (i+sampleOffset)/sampleRate;
			f = (t-2.5)*(t-2.5)*(t-2.5)*(t-2.5)*(t-2.5)*(t-b)*(t-b)*(t-b)*t*t*c*c+d*t;
			phase = f*2*Math.PI;
			if(addHarmonics) {
				sound[i] = Math.sin(phase)+.15*Math.sin(2*phase)/damp+.015*Math.sin(3*phase)/damp+.015*Math.sin(4*phase)/damp;
			}else {
				sound[i] = Math.sin(phase);
			}
		}
		taperEnds(sound, 10);
		return sound;
	}
	
	private double getRandom(double[] range) {
		return range[0] + (range[1]-range[0]) * r.nextDouble();
	}

	public double[] getLengthR() {
		return lengthR;
	}

	public void setLengthR(double[] lengthR) {
		this.lengthR = lengthR;
	}

	public double[] getMeanR() {
		return meanR;
	}

	public void setMeanR(double[] meanR) {
		this.meanR = meanR;
	}

	
	public double[] getHarmonicDampingR() {
		return harmonicDampingR;
	}

	
}
