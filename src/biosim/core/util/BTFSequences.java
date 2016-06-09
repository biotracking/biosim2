package biosim.core.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.io.File;
import java.io.IOException;

public class BTFSequences{
	public HashMap<String,BTFData> sequences;
	public static long timeout=10000; //10 seconds
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

	public void writeDir(File parentDirectory) throws IOException{
		for(String seqName : sequences.keySet()){
			File seqDir = new File(parentDirectory,seqName);
			if(!seqDir.exists()){
				seqDir.mkdir();
				BTFData btf = sequences.get(seqName);
				btf.writeDir(seqDir);
			}
		}
	}

	public static void splitIntoSequences(File parentDirectory, BTFData originalBTF, int framesPerSeq) throws IOException{
		ArrayList<BTFData.BTFDataFrame> frames = originalBTF.splitIntoFrames();
		BTFSequences rv = new BTFSequences();
		String seqPrefix = "seq_";
		int seqCtr = 0;
		int curStartFrame = 0;
		long lastTime = System.currentTimeMillis();
		double lastLine = frames.get(frames.size()-1).start + frames.get(frames.size()-1).len;
		while(curStartFrame<frames.size()){
			int frameEnd=curStartFrame+1;
			BTFData.BTFDataFrame startFrame = frames.get(curStartFrame);
			ArrayList<Integer> startIDs = startFrame.parentObj.getUniqueIDs(startFrame.start,startFrame.start+startFrame.len);
			while(frameEnd<frames.size() && (frameEnd-curStartFrame)<framesPerSeq){
				BTFData.BTFDataFrame tmp = frames.get(frameEnd);
				ArrayList<Integer> frameEndIDs = tmp.parentObj.getUniqueIDs(tmp.start,tmp.start+tmp.len);
				if((frameEndIDs.size() != startIDs.size())||!(startIDs.containsAll(frameEndIDs))){
					break;
				}
				frameEnd++;
				long curTime = System.currentTimeMillis();
				if( (curTime - lastTime) > timeout){
					System.out.println("Frame: "+frameEnd+" ("+(100*tmp.start/lastLine)+"%)");
					lastTime = curTime;
				}
			}
			if((frameEnd-curStartFrame)<framesPerSeq){
				continue;
			} else {
				File seqDir = new File(parentDirectory,seqPrefix+seqCtr);
				if(!seqDir.exists()){
					seqDir.mkdir();
				}
				BTFData.BTFDataFrame endFrame = frames.get(curStartFrame+framesPerSeq);
				originalBTF.writeDir(seqDir,startFrame.start,endFrame.start);
				seqCtr++;
			}
			curStartFrame = frameEnd;
		}
		System.out.println("Split "+seqCtr+" frames");
	}

	public static void main(String[] args){
		if(args.length != 3){
			System.out.println("Usage: java BTFSequences <BTFFile> <saveDir> <framesPerSeq>");
			System.out.print("Args:");
			for(int i=0;i<args.length;i++){
				System.out.print(" "+args[i]);
			}
			System.out.println();
		} else{
			BTFData btf = new BTFData();
			btf.loadDir(new File(args[0]));
			File saveDir = new File(args[1]);
			int framesPerSeq = Integer.parseInt(args[2]);
			try{
				splitIntoSequences(saveDir,new BufferedBTFData(btf), framesPerSeq);
			} catch(IOException ioe){
				throw new RuntimeException("[BTFSequences] Error splitting into sequences: "+ioe);
			}
		}
	}
}