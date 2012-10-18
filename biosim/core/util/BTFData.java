package biosim.core.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.ArrayList;

public class BTFData{
	public HashMap<String,File> columns;
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
	public String[] loadColumn(String columnName) throws IOException{
		String[] rv = new String[0];
		ArrayList<String> data = new ArrayList<String>();
		File col = columns.get(columnName);
		if(col != null){
			BufferedReader buf = new BufferedReader(new FileReader(col));
			String line = buf.readLine();
			while(line != null && buf.ready()){
				data.add(line);
				line = buf.readLine();
			}
		}
		return data.toArray(rv);
	}
	public HashMap<String,String[]> loadAllColumns() throws IOException{
		HashMap<String,String[]> rv = new HashMap<String,String[]>();
		for(String name : columns.keySet()){
			rv.put(name,loadColumn(name));
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
