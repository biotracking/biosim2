// WriteVelStats.java
package biosim.core.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

public class WriteVelStats{
	public static void main(String[] args){
		try{
			Properties cmdLineArgs = ArgsToProps.parse(args);
			String btfPath = cmdLineArgs.getProperty("--btf");
			BufferedWriter outf = new BufferedWriter(new FileWriter(new File(new File(btfPath),"xvmean.btf")));
			BTFData btf = new BTFData();
			System.out.print("Loading ["+btfPath+"], ");
			System.out.flush();
			btf.loadDir(new File(btfPath));
			System.out.print("xvmean, ");
			System.out.flush();
			String[] velstats = btf.computeXVMean();
			for(String line : velstats){
				outf.write(line+"\n");
			}
			outf.close();
			System.out.print("xvstd, ");
			System.out.flush();
			outf = new BufferedWriter(new FileWriter(new File(new File(btfPath),"xvstd.btf")));
			btf.loadDir(new File(btfPath));
			String[] xvstd = btf.computeXVStd();
			for(String line : xvstd){
				outf.write(line+"\n");
			}
			outf.close();
			System.out.print("xvmax, ");
			System.out.flush();
			outf = new BufferedWriter(new FileWriter(new File(new File(btfPath),"xvmax.btf")));
			btf.loadDir(new File(btfPath));
			String[] xvmax = btf.computeXVMaxMag();
			for(String line : xvmax){
				outf.write(line+"\n");
			}
			outf.close();
			System.out.println("done!");
		} catch(IOException ioe){
			throw new RuntimeException("[WriteVelStats] IO error: "+ioe);
		}
	}
}