package biosim.app.fishlr;

import biosim.core.agent.Agent;
import biosim.core.body.AbstractFish;
import biosim.core.body.NotemigonusCrysoleucas;
import biosim.core.gui.GUISimulation;
import biosim.core.sim.Environment;
import biosim.core.sim.Simulation;
import biosim.core.sim.RectObstacle;

import sim.util.MutableDouble2D;
import sim.util.Double3D;

import java.io.File;
import java.io.IOException;


public class FishLR implements Agent{
	AbstractFish fishBody;
	/*
	Speedingforce and turning force:
	Front-back speed betas: [ 0.00145171  0.00013191]
	Turn speed betas: [ -4.55837185e-05   5.36143576e-04]

	dvel x and dvel theta:
	Front-back speed betas: [ 0.07561636 -0.2372422 ]
	Turn speed betas: [-15.30309163   1.93068044]

	dvel, [avgX, avgY, wallX, wallY]:
	Front-back speed betas: [-0.06393678  0.23871273  0.01978693 -0.30392783]
	Turn speed betas: [ -1.73975683  -2.89316081 -16.0708904    6.23293313]

	*/
	
	public static final double[] FB_BETA = {-0.07561636, 0.2372422};
	public static final double[] LR_BETA = {15,30309163, -1.93068044};
	
	/*	
	public static final double[] FB_BETA = {0.07561636, -0.2372422};
	public static final double[] LR_BETA = {15.30309163, -1.93068044};
	/**/
	/*
	public static final double[] FB_BETA = {-0.06393678, 0.23871273,
											0.01978693, -0.30392783};
	public static final double[] LR_BETA = {-1.73975683, -2.89316081,
											-16.0708904,  6.23293313};
	/**/
	/*
	public static final double[] FB_BETA = {0.00831176, -0.0259669, 
											-0.00091077, 0.00584585};
	public static final double[] LR_BETA = {-0.06239644, 0.10435975, 
											-0.10492971,  0.10604713};
	*/
	public double oldTime = 0.0;
	public FishLR(AbstractFish b){
		fishBody = b;
	}
	public void init(){
	}
	public void finish(){
	}
	public void act(double time){
		double[] rv = new double[3];
		double[] oldVel = new double[3];
		MutableDouble2D avgFish = new MutableDouble2D();
		MutableDouble2D nnFish = new MutableDouble2D();
		MutableDouble2D wall = new MutableDouble2D();
		boolean sawFish = fishBody.getAverageSameTypeVec(avgFish);
		fishBody.getAverageSameTypeVec(nnFish);
		fishBody.getNearestObstacleVec(wall);
		fishBody.getSelfVelXYT(oldVel);
		double[] sensors = new double[FB_BETA.length];
		sensors[0] = avgFish.x;
		sensors[1] = avgFish.y;
		//sensors[2] = wall.x;
		//sensors[3] = wall.y;
		double fbSpeed = 0.0, turnSpeed = 0.0;
		//double fbAccel=0.0, lrAccel=0.0;
		for(int i=0;i<sensors.length;i++){
			//fbAccel+= FB_BETA[i]*sensors[i];
			//lrAccel+= -LR_BETA[i]*sensors[i];
			fbSpeed += FB_BETA[i]*sensors[i];
			turnSpeed += FB_BETA[i]*sensors[i];
		}
		//fishBody.setDesiredVelocity(oldVel[0]+(fbAccel*(time-oldTime)),
		//							oldVel[1],
		//							oldVel[2]+(lrAccel*(time-oldTime)));
		fishBody.setDesiredVelocity(fbSpeed, 0.0, turnSpeed);
		oldTime = time;
	}
	
	public static final double WIDTH=2.5;//2.5;
	public static final double HEIGHT=1.5;//2.5;
	
	public static void main(String[] args){
		//set up the environment
		int numFish = 30;
		int numLeaderFish = 5;
		Environment env = new Environment(WIDTH,HEIGHT,1.0/30.0);
		/*
		env.addObstacle(new RectObstacle(0.01,HEIGHT), WIDTH-0.01,  0.0);//east wall
		env.addObstacle(new RectObstacle(0.01,HEIGHT),  0.0,  0.0);//west
		env.addObstacle(new RectObstacle(WIDTH,0.01),  0.0,  0.0);//north
		env.addObstacle(new RectObstacle(WIDTH,0.01),  0.0, HEIGHT-0.01);//south
		/**/
		env.setToroidal(true);
		//add agents
		NotemigonusCrysoleucas[] bodies = new NotemigonusCrysoleucas[numFish];
		for(int i=0;i<bodies.length;i++){
			bodies[i] = new NotemigonusCrysoleucas();
			env.addBody(bodies[i]);
		}
	
		Agent[] agents = new Agent[numFish];
		for(int i=0; i< numLeaderFish;i++){
			agents[i] = new LeaderFish(bodies[i]);
			bodies[i].setAgent(agents[i]);
		}
		for(int i=numLeaderFish;i<agents.length;i++){
			agents[i] = new FishLR(bodies[i]);
			bodies[i].setAgent(agents[i]);
		}
		
		
		//env.runSimulation(args);
		Simulation sim = env.newSimulation();
		GUISimulation gui = new GUISimulation(sim);
		gui.setPortrayalClass(NotemigonusCrysoleucas.class, biosim.app.fishknn.FishPortrayal.class);
		gui.setDisplaySize((int)(WIDTH*500),(int)(HEIGHT*500));
		gui.createController();
	}
}
