// WriteXAccel.java
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

public class WriteXAccel{
	public static void main(String[] args){
		try{
			Properties cmdLineArgs = ArgsToProps.parse(args);
			String btfPath = cmdLineArgs.getProperty("--btf");
			BufferedWriter outf = new BufferedWriter(new FileWriter(new File(new File(btfPath),"xaccel.btf")));
			BTFData btf = new BTFData();
			System.out.print("Loading ["+btfPath+"], ");
			System.out.flush();
			btf.loadDir(new File(btfPath));
			System.out.print("xaccel, ");
			System.out.flush();
			String[] xaccel = btf.computeXAccel(Double.parseDouble(cmdLineArgs.getProperty("--fps")));
			for(String line : xaccel){
				outf.write(line+"\n");
			}
			outf.close();
			System.out.println("done!");
		} catch(IOException ioe){
			throw new RuntimeException("[WriteXAccel] IO error: "+ioe);
		}
	}
}