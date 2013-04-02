package biosim.core.body;

import biosim.core.body.sensors.*;
import biosim.core.body.actuators.*;

import ec.util.MersenneTwisterFast;
import sim.engine.Steppable;
import sim.engine.SimState;

public abstract class AbstractFly extends Body
	implements 	NearestSameAgentVec,
				NearestObstacleVec,
				NearestPredatorVec,
				HolonomicDrive,
				SelfVelXYT {
	public AbstractAnt grabbedBy;
	public boolean yummy = true;
	public void init(){
		yummy = true;
	}
}
