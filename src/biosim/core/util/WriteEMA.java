// WriteEMA.java
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

public class WriteEMA{
	public static void main(String[] args){
		try{
			Properties cmdLineArgs = ArgsToProps.parse(args);
			String btfPath = cmdLineArgs.getProperty("--btf");
			String outputColname = cmdLineArgs.getProperty("--outColName");
			String inputColname = cmdLineArgs.getProperty("--inColName");
			double alpha = Double.parseDouble(cmdLineArgs.getProperty("--alpha"));
			BufferedWriter outf = new BufferedWriter(new FileWriter(new File(new File(btfPath),outputColname+".btf")));
			BTFData btf = new BTFData();
			System.out.print("Loading ["+btfPath+"], ");
			System.out.flush();
			btf.loadDir(new File(btfPath));
			System.out.print(outputColname+", ");
			System.out.flush();
			String[] emaCol = btf.computeEMA(inputColname,alpha);
			for(String line : emaCol){
				outf.write(line+"\n");
			}
			outf.close();
			System.out.println("done!");
		} catch(IOException ioe){
			throw new RuntimeException("[WriteEMA] IO error: "+ioe);
		}
	}
}