package biosim.app.learnedants;

import biosim.core.agent.Agent;
import biosim.core.body.AbstractAnt;
import biosim.core.body.AphaenogasterCockerelli;
import biosim.core.gui.GUISimulation;
import biosim.core.sim.Environment;
import biosim.core.sim.Simulation;
import biosim.core.sim.RectObstacle;
import biosim.core.util.BTFData;
import biosim.core.util.FastKNN;

import sim.util.MutableDouble2D;

import java.io.File;
import java.io.IOException;

public class LearnedAnts implements Agent{
	public static final int FEATURE_DIM = 7;
	public static final int NUM_SWITCHES = 2;
	public static final int NUM_NEIGHBORS = 5;
	double[][][] transitionMatrix;
	double[] prior;
	FastKNN[] outputFunction;
	AbstractAnt antBody;
	double[] prevVel = {0.0, 0.0, 0.0};
	int currentState = -1;
	public LearnedAnts(AbstractAnt b, double[] prior, double[][][] transitionMatrix, FastKNN[] knns){
		antBody = b;
		this.prior = prior;
		this.transitionMatrix = transitionMatrix;
		outputFunction = knns;
	}
		
	public double[] act(double time){
		//get the sensors
		double[] rv = new double[3];
		MutableDouble2D ant = new MutableDouble2D();
		boolean sawAnt = antBody.getNearestSameAgentVec(ant);
		MutableDouble2D wall = new MutableDouble2D();
		boolean sawWall = antBody.getNearestObstacleVec(wall);
		MutableDouble2D home = new MutableDouble2D();
		boolean sawHome = antBody.getHomeDir(home);
		double[] sensorVec = new double[FEATURE_DIM];
		double[][] nearestK = new double[NUM_NEIGHBORS][3];
		boolean[] switches = new boolean[NUM_SWITCHES];
		sensorVec[0] = ant.x;
		sensorVec[1] = ant.y;
		sensorVec[2] = wall.x;
		sensorVec[3] = wall.y;
		sensorVec[4] = prevVel[0];
		sensorVec[5] = prevVel[1];
		sensorVec[6] = prevVel[2];
		switches[0] = sawWall;
		switches[1] = sawAnt;
		//figure out the initial state
		if(currentState == -1){
			double sum = 0.0;
			double randNum = antBody.getRandom().nextDouble();
			for(int i=0;i<prior.length;i++){
				if(randNum > prior[i]+sum){
					sum += prior[i];
				} else {
					currentState = i;
					break;
				}
			}
			//just in case prior doesn't sum to 1
			if(currentState == -1) currentState = prior.length-1;
		}
		//figure out the output for the current state
		outputFunction[currentState].query(sensorVec,nearestK);
		for(int i=0;i<rv.length;i++) rv[i] = 0.0;
		for(int i=0;i<nearestK.length;i++){
			for(int j=0;j<nearestK[i].length;j++){
				rv[j] += nearestK[i][j];
			}
		}
		for(int i=0;i<rv.length;i++) rv[i] = rv[i]/(double)nearestK.length;
		prevVel[0] = rv[0];
		prevVel[1] = rv[1];
		prevVel[2] = rv[2];
		//figure out the next state to transition to
		int newState = -1;
		int switchingVariable = 0;
		for(int i=0;i<switches.length;i++){
			switchingVariable = switchingVariable << 1;
			if(switches[i]){
				switchingVariable += 1;
			}
		}
		double sum = 0.0;
		double randNum = antBody.getRandom().nextDouble();
		for(int i=0;i<transitionMatrix[currentState].length;i++){
			if(randNum > transitionMatrix[currentState][i][switchingVariable]+sum){
				sum += transitionMatrix[currentState][i][switchingVariable];
			} else {
				newState = i;
				break;
			}
		}
		if(newState == -1) newState = transitionMatrix[currentState].length-1;
		currentState = newState;
		
		return rv;
	}
	
	public static void buildParameters(File parameterFile, FastKNN[] knns, double[] prior, double[][][] transitionFunction){
		
	}
	
}