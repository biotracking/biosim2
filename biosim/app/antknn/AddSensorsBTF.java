
import biosim.core.util.BTFData;

import sim.util.MutableDouble2D;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;

public class AddSensorsBTF{
	
	public static void main(String[] args){
		try{
			if(args.length == 7){
				File cwd = new File(System.getProperties().getProperty("user.dir"));
				BTFData btf = new BTFData();
				btf.loadDir(new File(args[0]));
				HashMap<String,String[]> data = btf.loadAllColumns();
				int blockStart = 0, blockEnd = -1, nextBlockEnd = -1;
				double sensorRange = Double.parseDouble(args[1]);
				double sensorFOVRad = Math.toRadians(Double.parseDouble(args[2])/2.0);
				double left = Double.parseDouble(args[3]);
				double right = Double.parseDouble(args[4]);
				double top = Double.parseDouble(args[5]);
				double bottom = Double.parseDouble(args[6]);
				FileWriter wallVecOut = new FileWriter(new File(cwd,"wallvec.btf"));
				FileWriter wallBoolOut = new FileWriter(new File(cwd,"wallbool.btf"));
				FileWriter antVecOut = new FileWriter(new File(cwd,"antvec.btf"));
				FileWriter antBoolOut = new FileWriter(new File(cwd,"antbool.btf"));
				FileWriter desiredVelOut = new FileWriter(new File(cwd,"dvel.btf"));
				FileWriter desiredBoolOut = new FileWriter(new File(cwd,"dbool.btf"));
				for(int i=0;i<data.get("id").length;i++){
					if(i%(data.get("id").length/10)==0){
						System.out.println("On track "+i+" of "+data.get("id").length);
					}
					//find the end of the block
					if(blockEnd == -1){
						for(int j=0;(blockStart+j)<data.get("clocktime").length;j++){
							if(Double.parseDouble(data.get("clocktime")[blockStart+j]) != Double.parseDouble(data.get("clocktime")[blockStart])){
								blockEnd = blockStart+j;
								break;
							}
						}
					}

					//find the end of the next block
					if(nextBlockEnd == -1){
						for(int j=0;(j+blockEnd)<data.get("clocktime").length;j++){
							if(Double.parseDouble(data.get("clocktime")[blockEnd+j]) != Double.parseDouble(data.get("clocktime")[blockEnd])){
								nextBlockEnd = blockEnd+j;
								break;
							}
						}
					}

					//get the current id,x,y,t,time
					String id = data.get("id")[i];
					double x = Double.parseDouble(data.get("xpos")[i]);
					double y = Double.parseDouble(data.get("ypos")[i]);
					MutableDouble2D loc = new MutableDouble2D(x,y);
					double theta = Double.parseDouble(data.get("timage")[i]);
					MutableDouble2D dir = (new MutableDouble2D(1,0)).rotate(theta);
					double curTime = Double.parseDouble(data.get("clocktime")[i]);

					//find the closest ant within sensorRange
					double tmpX=0.0,tmpY=0.0,tmpDist,tmpT;
					boolean detectedAnt = false;
					MutableDouble2D minVec = null;
					MutableDouble2D tmpVec = null;
					double minDist = -1;
					for(int j=blockStart;j<blockEnd;j++){
						if(data.get("id")[j].equals(id)) continue;
						tmpX = Double.parseDouble(data.get("xpos")[j]);
						tmpY = Double.parseDouble(data.get("ypos")[j]);
						tmpVec = (new MutableDouble2D(tmpX-x,tmpY-y)).rotate(-theta);
						tmpDist = tmpVec.lengthSq();
						if(Math.abs(tmpVec.angle()) > Math.abs(sensorFOVRad)) continue;
						if(minVec==null || tmpDist < minDist){
							minVec = tmpVec;
							minDist = tmpDist;
						}
					}
					tmpX = tmpY = 0.0;
					if(minVec != null && minDist <= Math.pow(sensorRange,2)){
						tmpX = minVec.x;
						tmpY = minVec.y;
						detectedAnt = true;
					}
					String antLine = tmpX+" "+tmpY;
					String antBoolLine = ""+detectedAnt;

					//find the closest wall within sensorRange
					tmpX=0.0; tmpY = 0.0;
					boolean detectedWall = false;
					minVec = null; tmpVec = null;
					minDist = -1;
					//left
					tmpVec = (new MutableDouble2D(left-loc.x,0).rotate(-theta));
					if(Math.abs(tmpVec.angle()) <= Math.abs(sensorFOVRad)){
						minVec = tmpVec;
					}
					//right
					tmpVec = (new MutableDouble2D(right-loc.x,0).rotate(-theta));
					if(Math.abs(tmpVec.angle()) <= Math.abs(sensorFOVRad)){
						if(minVec == null || tmpVec.lengthSq() < minVec.lengthSq()){
							minVec = tmpVec;
						}
					}
					//top
					tmpVec = (new MutableDouble2D(0,top-loc.y).rotate(-theta));
					if(Math.abs(tmpVec.angle()) <= Math.abs(sensorFOVRad)){
						if(minVec == null || tmpVec.lengthSq() < minVec.lengthSq()){
							minVec = tmpVec;
						}
					}
					//bottom
					tmpVec = (new MutableDouble2D(0,bottom-loc.y).rotate(-theta));
					if(Math.abs(tmpVec.angle()) <= Math.abs(sensorFOVRad)){
						if(minVec == null || tmpVec.lengthSq() < minVec.lengthSq()){
							minVec = tmpVec;
						}
					}
					tmpX = tmpY = 0.0;
					if(minVec != null && minVec.lengthSq() <= Math.pow(sensorRange,2)){
						tmpX = minVec.x;
						tmpY = minVec.y;
						detectedWall = true;
					}
					String wallLine = tmpX+" "+tmpY;
					String wallBoolLine = ""+detectedWall;
					
					//find the ant position/orientation in the next block
					boolean desiredBool = false;
					double[] desiredVel = {0.0, 0.0, 0.0};
					for(int j = blockEnd; j< nextBlockEnd; j++){
						if(data.get("id")[j].equals(id)){
							tmpX = Double.parseDouble(data.get("xpos")[j]);
							tmpY = Double.parseDouble(data.get("ypos")[j]);
							tmpT = Double.parseDouble(data.get("timage")[j]);
							double tmpC = Double.parseDouble(data.get("clocktime")[j]);
							tmpVec = (new MutableDouble2D(tmpX-x,tmpY-y).rotate(-theta));
							desiredVel[0] = tmpVec.x/(curTime-tmpC);
							desiredVel[1] = tmpVec.y/(curTime-tmpC);
							desiredVel[2] = (tmpT-theta)/(curTime-tmpC);
							desiredBool = true;
							break;
						}
					}
					String dVelLine = desiredVel[0]+" "+desiredVel[1]+" "+desiredVel[2];
					String dVelBoolLine = ""+desiredBool;
					//write out lines
					wallVecOut.write(wallLine+"\n");
					wallBoolOut.write(wallBoolLine+"\n");
					antVecOut.write(antLine+"\n");
					antBoolOut.write(antBoolLine+"\n");
					desiredVelOut.write(dVelLine+"\n");
					desiredBoolOut.write(dVelBoolLine+"\n");
					//if current timestamp > timestamp start of block = end of block
					//end of block = end of next block
					//find end of next block
					if(i >= (blockEnd-1)){
						blockStart = blockEnd;
						blockEnd = nextBlockEnd;
						for(int j=0;(j+blockEnd)<data.get("clocktime").length;j++){
							if(Double.parseDouble(data.get("clocktime")[blockEnd+j]) != Double.parseDouble(data.get("clocktime")[blockEnd]) 
								|| (blockEnd+j+1)== data.get("clocktime").length){
								nextBlockEnd = blockEnd+j;
								break;
							}
						}
					}
				}
				wallVecOut.close();
				wallBoolOut.close();
				antVecOut.close();
				antBoolOut.close();
				desiredVelOut.close();
				desiredBoolOut.close();
			} else {
				System.out.println("usage: java AddSensorsBTF <btfDir> <sensorRange> <sensorFOVDegrees> <leftWall> <rightWall> <topWall> <bottomWall>");
			}
		} catch(Exception e){
			throw new RuntimeException(e);
		}
	}
}
