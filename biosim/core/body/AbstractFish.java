package biosim.core.body;

import biosim.core.body.sensors.*;
import biosim.core.body.actuators.*;

public abstract class AbstractFish extends Body
	implements	NearestSameTypeVec,
				AverageSameTypeVec,
				NearestObstacleVec,
				Proximity,
				ZoneCoMVecs,
				HolonomicDrive,
				SelfVelXYT{
}
