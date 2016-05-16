package biosim.core.util;

import java.util.HashMap;
import java.io.File;

public class BTFSequences{
	public HashMap<String,BTFData> sequences;

	public BTFSequences(){
		sequences = new HashMap<String,BTFData>();
	}

	public void loadDir(File parentDirectory){
		if(parentDirectory.isDirectory()){
			File[] btfDirs = parentDirectory.listFiles();
			String seqName;
			BTFData seq;
			for(int i=0;i<btfDirs.length;i++){
				seqName = btfDirs[i].getName();
				seq = new BTFData();
				seq.loadDir(btfDirs[i]);
				sequences.put(seqName,seq);
			}
		}
	}
}