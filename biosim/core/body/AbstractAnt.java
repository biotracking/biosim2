package biosim.core.body;

import biosim.core.body.sensors.*;
import biosim.core.body.actuators.*;

public abstract class AbstractAnt extends Body
	implements	NearestSameAgentVec,
				Homing,
				DirectionToPOI,
				NearPOI,
				NearestObstacleVec,
				NearestPreyVec,
				Gripper,
				GripperSensor,
				SelfVelXYT {
	public AbstractFly grabbing;
	
}
