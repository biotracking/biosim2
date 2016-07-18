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
		loadDir(parentDirectory,true);
	}

	public void loadDir(File parentDirectory,boolean cached){
		if(parentDirectory.isDirectory()){
			File[] btfDirs = parentDirectory.listFiles();
			String seqName;
			BTFData seq;
			for(int i=0;i<btfDirs.length;i++){
				seqName = btfDirs[i].getName();
				seq = new BTFData();
				seq.loadDir(btfDirs[i]);
				if(cached){
					BufferedBTFData buffered = new BufferedBTFData(seq);
					try{
						buffered.loadBuffer();
					} catch(IOException ioe){
						System.err.println("[BTFSequences] Warning! failed to load cache: "+ioe);
					}
					seq = buffered;
				}
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
		System.out.println("Splitting into frames");
		ArrayList<BTFData.BTFDataFrame> frames = originalBTF.splitIntoFrames();
		BTFSequences rv = new BTFSequences();
		String seqPrefix = "seq_";
		int seqCtr = 0;
		int curStartFrame = 0;
		long lastTime = System.currentTimeMillis();
		long startTime = lastTime;
		double lastLine = frames.get(frames.size()-1).start + frames.get(frames.size()-1).len;
		while(curStartFrame<frames.size()){
			//System.out.println("Outer loop");
			int frameEnd=curStartFrame+1;
			BTFData.BTFDataFrame startFrame = frames.get(curStartFrame);
			ArrayList<Integer> startIDs = startFrame.parentObj.getUniqueIDs(startFrame.start,startFrame.start+startFrame.len);
			while(frameEnd<frames.size() && (frameEnd-curStartFrame)<framesPerSeq){
				//System.out.println("Inner loop");
				BTFData.BTFDataFrame tmp = frames.get(frameEnd);
				ArrayList<Integer> frameEndIDs = tmp.parentObj.getUniqueIDs(tmp.start,tmp.start+tmp.len);
				long curTime = System.currentTimeMillis();
				if( (curTime - lastTime) > timeout){
					String stats = String.format(" (%.1f%%, %.1f fps)",(100*tmp.start/lastLine), (1000.0*frameEnd)/(double)(curTime-startTime));
					System.out.println("Sequence "+seqCtr+" Frame: "+frameEnd+stats);
					lastTime = curTime;
				}
				if(frameEndIDs.size() != startIDs.size()){
					break;
				}
				if( !startIDs.containsAll(frameEndIDs)){
					break;
				}
				frameEnd++;
			}
			if((frameEnd-curStartFrame)==framesPerSeq){
				File seqDir = new File(parentDirectory,seqPrefix+seqCtr);
				//System.out.println("Writting sequence "+seqCtr);
				if(!seqDir.exists()){
					seqDir.mkdir();
				}
				BTFData.BTFDataFrame endFrame = frames.get(curStartFrame+framesPerSeq);
				originalBTF.writeDir(seqDir,startFrame.start,endFrame.start);
				seqCtr++;
			}
			curStartFrame = frameEnd;
		}
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
				BufferedBTFData bbtf = new BufferedBTFData(btf);
				System.out.println("Loading btf to memory");
				bbtf.loadBuffer();
				System.out.println("Done");
				splitIntoSequences(saveDir,bbtf, framesPerSeq);
			} catch(IOException ioe){
				throw new RuntimeException("[BTFSequences] Error splitting into sequences: "+ioe);
			}
		}
	}
}
