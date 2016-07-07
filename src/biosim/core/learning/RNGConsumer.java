// RNGConsumer.java

package biosim.core.learning;

import ec.util.MersenneTwisterFast;

public interface RNGConsumer{
	public MersenneTwisterFast getRandom();
	public void setRandom(MersenneTwisterFast r);
}