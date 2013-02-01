package biosim.core.util.kdewrapper;

import java.util.ArrayList;
import java.util.Scanner;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;


public class TestSimpleKDE {
	public static void main(String[] args){
		if(args.length != 1){
			System.out.println("Usage: java TestSimpleKDE <SamplesFilename>");
		} else {
			try{
				System.loadLibrary("kdewrapper");
				SimpleKDE kde = new SimpleKDE(1,0.25);
				//System.out.println("#Adding data");
				BufferedReader bRead = new BufferedReader(new FileReader(args[0]));
				String line = bRead.readLine();
				double[] tmp = new double[1];
				while(line != null && bRead.ready()){
					line.trim();
					Scanner scan = new Scanner(line);
					tmp[0] = scan.nextDouble();
					kde.add(tmp);
					line = bRead.readLine();
				}
				//System.out.println("#Number of samples: "+kde.numSamples());
				//System.out.println("#Generating estimated output, range:[-5,15), step size:0.01");
				for(double i = -5.0;i<15.0;i+= 0.01){
					tmp[0] = i;
					double estimate = kde.estimate(tmp,1);
					//System.out.println(i+" "+estimate);
				}
				//System.out.println("#Done!");
			} catch(IOException ioe){
				throw new RuntimeException(ioe);
			}
		}
	}
}
