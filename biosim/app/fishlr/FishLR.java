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
	
	/*
	public static final double[] FB_BETA = {-0.07561636, 0.2372422};
	public static final double[] LR_BETA = {15,30309163, -1.93068044};
	*/
	public static final double[] COMBINED_BETA_X =  {	0.0238146803487, 	//xsep
														-0.00872664696674, 	//ysep
														0.0447434782331, 	//xalign
														0.00298139349923, 	//yalign
														-0.206953112024, 	//xcohes
														0.0608517994282, 	//ycohes
														-0.00232599514171, 	//xobst
														0.000129982389957, 	//yobst
														0.0391581524951};	//bias

	public static final double[] COMBINED_BETA_Y = {	0.000750508637173, 	//xsep
														-0.00354279959463, 	//ysep
														-3.45686904782e-06,	//xalign
														-0.000907278811423,	//yalign
														-0.000267705370536,	//xcohes
														0.00139186498825,	//ycohes
														4.3853175437e-06,	//xobst
														9.79859587638e-07,	//yobst
														-1.40972310414e-05};//bias

	public static final double[] INDEPENDENT_BETA_X = {	0.0231357101335,	//sep
														0.0447573654891, 	//align
														-0.207024332986, 	//cohes
														-0.00236147388765, 	//obst
														0.0392014263121};	//bias

	public static final double[] INDEPENDENT_BETA_Y = {	-0.00351918718575,	//sep
														-0.000901584863935,	//align
														0.00138962257189,	//cohes
														1.14346877271e-06,	//obst
														-1.48782713764e-05};//bias
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
		double[] sensors = new double[COMBINED_BETA_X.length];
		sensors[0] = avgFish.x;
		sensors[1] = avgFish.y;
		//sensors[2] = wall.x;
		//sensors[3] = wall.y;
		double fbSpeed = 0.0, turnSpeed = 0.0;
		//double fbAccel=0.0, lrAccel=0.0;
		for(int i=0;i<sensors.length;i++){
			//fbAccel+= FB_BETA[i]*sensors[i];
			//lrAccel+= -LR_BETA[i]*sensors[i];
			// fbSpeed += FB_BETA[i]*sensors[i];
			// turnSpeed += FB_BETA[i]*sensors[i];
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
