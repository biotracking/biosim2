package biosim.core.body;

import biosim.core.body.sensors.*;

public abstract class AbstractAnt extends Body
	implements	NearestSameAgentVec,
				Homing,
				DirectionToPOI,
				NearPOI,
				NearestObstacleVec,
				SelfVelXYT {
	
}
