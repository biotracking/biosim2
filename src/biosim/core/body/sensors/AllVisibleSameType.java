package biosim.core.body.sensors;

import java.util.List;
import sim.util.MutableDouble2D;

import biosim.core.agent.Agent;

/**
 * This sensor returns a list of all the visible agents of the same kind within
 * a specified field-of-view and range.
 *
 * @author Brian Hrolenok
 * @version 2.0
 */
public interface AllVisibleSameType {
	/**
	 * Queries the sensor for visible agents. <b>NOTE</b>: the return
	 * value represents a successful query, but doesn't specify that
	 * any agents were visible. That is, this method can return <code>true</code>
	 * even if <code>rv</code> is empty.
	 * @param	rv	A list in which to store egocentric vectors to visible agents
	 * 				of the same type.
	 * @return 		<code>true</code> if the call succeeded, <code>false</code>
	 *				otherwise. May return true even if <code>rv</code> is empty.
	 */
	public boolean getAllVisibleSameType(List<Agent> rv);
	/**
	 * Returns the range of this sensor.
	 * @return 		The range in meters of this sensor.
	 */
	public double getAllVisibleSameTypeSensorRange();
	/**
	 * Returns the field-of-view of this sensor.
	 * @return 		The field-of-view in radians of this sensor.
	 */
	public double getAllVisibleSameTypeSensorFoV();
}
