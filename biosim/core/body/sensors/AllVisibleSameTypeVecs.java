package biosim.core.body.sensors;
import java.util.List;
import sim.util.MutableDouble2D;

/**
 * This sensor returns a list of vectors to all the visible agents of the same kind within
 * a specified field-of-view and range.
 *
 * @author Brian Hrolenok
 * @version 2.0
 */
public interface AllVisibleSameTypeVecs {
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
	public boolean getAllVisibleSameTypeVecs(List<MutableDouble2D> rv);
	/**
	 * Returns the range of this sensor.
	 * @return 		The range in meters of this sensor.
	 */
	public double getAllVisibleSameTypeVecsSensorRange();
	/**
	 * Returns the field-of-view of this sensor.
	 * @return 		The field-of-view in radians of this sensor.
	 */
	public double getAllVisibleSameTypeVecsSensorFoV();
}
