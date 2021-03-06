package biosim.core.body;

import biosim.core.body.sensors.*;
import biosim.core.body.actuators.*;

public abstract class AbstractAnt extends Body
	implements	NearestSameTypeVec,
				Homing,
				DirectionToPOI,
				NearPOI,
				NearestObstacleVec,
				NearestPreyVec,
				Gripper,
				GripperSensor,
				HolonomicDrive,
				SelfVelXYT {
	public AbstractFly grabbing;
	public void init(){
		super.init();
		if(grabbing != null){
			grabbing.grabbedBy = null;
			grabbing = null;
		}
	}
	
}
