package biosim.core.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.ArrayList;

public class BTFData{
	public HashMap<String,File> columns;
	public static long timeout=10000; //10 seconds
	public BTFData(){
		columns = new HashMap<String,File>();
	}
	public void loadBTF(File btfFile){
		String columnName = btfFile.getName().split("\\.")[0];
		columns.put(columnName,btfFile);
	}
	public void loadDir(File parentDirectory){
		if(parentDirectory.isDirectory()){
			File[] btfFiles = parentDirectory.listFiles(new FileFilter(){
					public boolean accept(File pathname){
						return pathname.getName().endsWith(".btf");
					}
			});
			String columnName;
			for(int i=0;i<btfFiles.length;i++){
				columnName = btfFiles[i].getName().split("\\.")[0];
				//System.out.println(columnName);
				columns.put(columnName,btfFiles[i]);
			}
		}
	}

	public void writeBTF(File parentDirectory,String cName) throws IOException{
		writeBTF(parentDirectory,cName,0,-1);
	}
	public void writeBTF(File parentDirectory, String cName, int start, int end) throws IOException{
		//System.out.println("Writting "+cName+" to "+parentDirectory);
		String[] colData = loadColumn(cName);
		if(end <0 || end > colData.length){
			end = colData.length;
		}
		FileWriter writer = new FileWriter(new File(parentDirectory,cName+".btf"));
		for(int i=start;i<end;i++){
			writer.write(colData[i]+"\n");
		}
		writer.close();
	}

	public void writeDir(File parentDirectory) throws IOException{
		writeDir(parentDirectory,0,-1);
	}
	public void writeDir(File parentDirectory, int start, int end) throws IOException{
		for(String cname:columns.keySet()){
			writeBTF(parentDirectory,cname, start, end);
		}
	}

	public String[] loadColumn(String columnName) throws IOException{
		String[] rv = new String[0];
		ArrayList<String> data = new ArrayList<String>();
		File col = columns.get(columnName);
		if(col != null){
			BufferedReader buf = new BufferedReader(new FileReader(col));
			String line = buf.readLine();
			while(line != null){// && buf.ready()){
				data.add(line);
				if(buf.ready())
					line = buf.readLine();
				else line = null;
			}
		}
		return data.toArray(rv);
	}

	public double[][] columnAsDoubles(String columnName) throws IOException{
		String[] colStrings = loadColumn(columnName);
		String[] splt;
		int width = colStrings[0].split(" ").length;
		double[][] rv = new double[colStrings.length][width];
		for(int row=0;row<colStrings.length;row++){
			splt = colStrings[row].split(" ");
			for(int col=0;col<width;col++){
				rv[row][col] = Double.parseDouble(splt[col]);
			}
		}
		return rv;
	}

	public int numUniqueFrames(){
		int rv = 1;
		try{
			String[] timestamps = loadColumn("timestamp");
			for(int i=1;i<timestamps.length;i++){
				if(!(timestamps[i].equals(timestamps[i-1]))){
					rv++;
				}
			}
		} catch(IOException ioe){
			throw new RuntimeException("[BTFData] error parsing for unique frames: "+ioe);
		}
		return rv;
	}

	public ArrayList<Integer> getUniqueIDs(){
		return getUniqueIDs(0,-1);
	}
	public ArrayList<Integer> getUniqueIDs(int start, int end){
		try{
			return getUniqueIDs(start,end,loadColumn("id"));
		} catch(IOException ioe){
			throw new RuntimeException("[BTFData] parsing for unique IDs: "+ioe);
		}
	}
	public ArrayList<Integer> getUniqueIDs(int start, int end, String[] idCol){
		ArrayList<Integer> rv = new ArrayList<Integer>();
		if(end<0||end>idCol.length) end = idCol.length;
		for(int i=start;i<end;i++){
			int foo = Integer.parseInt(idCol[i]);
			if(rv.contains(foo)){
				continue;
			} else {
				rv.add(foo);
			}
		}
		return rv;
	}

	public ArrayList<Integer> rowIndexForID(int id){
		ArrayList<Integer> rv = new ArrayList<Integer>();

		String[] ids;
		try{
			ids = loadColumn("id");
		} catch(IOException ioe){
			throw new RuntimeException("[BTFData] error loading id column");
		}
		for(int i=0;i<ids.length;i++){
			if(ids[i].equals(""+id)){
				rv.add(i);
			}
		}
		return rv;
	}

	public HashMap<String,String[]> loadAllColumns() throws IOException{
		HashMap<String,String[]> rv = new HashMap<String,String[]>();
		for(String name : columns.keySet()){
			rv.put(name,loadColumn(name));
		}
		return rv;
	}

	public class  BTFDataFrame{
		public int start, len;
		public BTFData parentObj;
		public BTFDataFrame(int start, int len, BTFData parentObj){
			this.start = start;
			this.len = len;
			this.parentObj = parentObj;
		}
	}

	public ArrayList<BTFDataFrame> splitIntoFrames() throws IOException{
		return splitIntoFrames("timestamp");
	}
	public ArrayList<BTFDataFrame> splitIntoFrames(String frameColName) throws IOException{
		String[] frames = loadColumn(frameColName);
		ArrayList<BTFDataFrame> rv = new ArrayList<BTFDataFrame>();
		int currentBlockStart=0;
		long lastTime = System.currentTimeMillis();
		while(currentBlockStart<frames.length){
			int blockLen;
			for(blockLen=1;blockLen+currentBlockStart<frames.length;blockLen++){
				long curTime = System.currentTimeMillis();
				if(curTime-lastTime > timeout){
					System.out.println("Line: "+(blockLen+currentBlockStart)+" ("+ 100*((blockLen+currentBlockStart)/(double)frames.length)+"%)");
					lastTime = curTime;
				}
				if(!(frames[currentBlockStart].equals(frames[currentBlockStart+blockLen]))){
					break;
				}
			}
			rv.add(new BTFDataFrame(currentBlockStart,blockLen,this));
			currentBlockStart = currentBlockStart+blockLen;
		}
		return rv;
	}

	public static void main(String[] args){
		if(args.length == 1){
			BTFData btf = new BTFData();
			btf.loadBTF(new File(args[0]));
			System.out.println("Loaded");
			for(String name : btf.columns.keySet()){
				System.out.println(name+" -> "+btf.columns.get(name));
			}
			try{
				HashMap<String,String[]> data = btf.loadAllColumns();
				System.out.println(data);
			} catch(IOException ioe){
				System.out.println(ioe);
				throw new RuntimeException(ioe);
			}
		} else {
			System.out.println("java biosim.core.util.BTFData <btfDirectory>");
		}
	}
}
