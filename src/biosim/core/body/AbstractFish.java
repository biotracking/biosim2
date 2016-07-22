package biosim.core.body;

import biosim.core.body.sensors.*;
import biosim.core.body.actuators.*;

public abstract class AbstractFish extends Body
	implements	NearestSameTypeVec,
				AverageSameTypeVec,
				AverageRBFSameTypeVec,
				AverageRBFOrientationSameTypeVec,
				NearestObstacleVec,
				Proximity,
				ZoneCoMVecs,
				HolonomicDrive,
				SelfVelXYT,
				NeighborhoodStatistics{
	public boolean sameAsMe(AbstractFish other){
		return other == this;
	}
}
