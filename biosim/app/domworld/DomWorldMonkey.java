package biosim.app.domworld;

import biosim.core.agent.Agent;
import biosim.core.body.Body;
import biosim.core.body.AbstractMonkey;
import sim.util.MutableDouble2D;

public class DomWorldMonkey implements Agent{
	AbstractMonkey body;
	public double dominanceRank;
	public DomWorldMonkey(AbstractMonkey b, double domRank){
		body = b;
		dominanceRank = domRank;
	}
	public void init(){

	}
	public void finish(){

	}
	public void act(double time){

	}
}