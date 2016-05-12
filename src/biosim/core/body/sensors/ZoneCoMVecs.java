package biosim.core.body.sensors;

import sim.util.MutableDouble2D;
public interface ZoneCoMVecs {
	public int getNumZones();
	public boolean getZoneCoMVecs(MutableDouble2D[] zoneVecs);
	public void getZoneRanges(double[] ranges);
}
