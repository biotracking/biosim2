package biosim.core.body;

import biosim.core.body.sensors.*;
import biosim.core.body.actuators.*;

/**
 * This class collects the sensors and actuators for a general body
 * suitible for representing monkeys.
 *
 * @author Brian Hrolenok
 * @version 2.0
 */
public abstract class AbstractMonkey extends Body
	implements	HolonomicDrive,
				AllVisibleSameAgentsVecs {
}