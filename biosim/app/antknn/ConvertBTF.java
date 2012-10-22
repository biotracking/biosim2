
import biosim.core.util.BTFData;

import java.io.File;
import java.io.FileWriter;

public class ConvertBTF{
	public static void main(String[] args){
		try{
			if(args.length == 3){
				double ppm = Double.parseDouble(args[1]);
				double tps = Double.parseDouble(args[2]);
				File cwd = new File(System.getProperties().getProperty("user.dir"));
				BTFData btf = new BTFData();
				btf.loadDir(new File(args[0]));
				String[] xCol, yCol, timeCol;
				xCol = btf.loadColumn("ximage");
				yCol = btf.loadColumn("yimage");
				timeCol = btf.loadColumn("timestamp");
				for(int i=0;i<xCol.length;i++){
					xCol[i] = ""+Double.parseDouble(xCol[i])/ppm;
					yCol[i] = ""+Double.parseDouble(yCol[i])/ppm;
					timeCol[i] = ""+Double.parseDouble(timeCol[i])/tps;
				}
				FileWriter xOut, yOut, timeOut;
				xOut = new FileWriter(new File(cwd,"xpos.btf"));
				yOut = new FileWriter(new File(cwd,"ypos.btf"));
				timeOut = new FileWriter(new File(cwd,"clocktime.btf"));
				for(int i=0;i<xCol.length;i++){
					xOut.write(xCol[i]+"\n");
					yOut.write(yCol[i]+"\n");
					timeOut.write(timeCol[i]+"\n");
				}
				xOut.close();
				yOut.close();
				timeOut.close();
			} else { 
				System.out.println("usage: java ConvertBTF <btfDirectory> <pixels-per-meter> <timestamp-per-second>");
			}
		} catch(Exception e){
			throw new RuntimeException(e);
		}
	}
}
