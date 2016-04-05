package biosim.core.gui;

import sim.engine.SimState;
import sim.display.GUIState;
import sim.display.Display2D;
import sim.display.Controller;
import sim.portrayal.Portrayal;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.continuous.ContinuousPortrayal2D;
import sim.portrayal.simple.OvalPortrayal2D;
import sim.portrayal.simple.OrientedPortrayal2D;
import sim.portrayal.simple.LabelledPortrayal2D;
import sim.portrayal.SimplePortrayal2D;

import biosim.core.sim.Simulation;
import biosim.core.sim.RectObstacle;
import biosim.core.body.Body;
import biosim.core.gui.portrayals.BasicBodyPortrayal;
import biosim.core.gui.portrayals.RectObstaclePortrayal;

import javax.swing.JFrame;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.lang.reflect.InvocationTargetException;
import java.io.File;
import java.io.IOException;
public class GUISimulation extends GUIState {
	public static String getName() { return "BioSim"; }
	public static Object getInfo(){
		return
		"<H2>BioSim</H2>"+
		"<p>A simulation engine for biological multiagent systems</p>";
	}
	
	public Display2D display;
	public JFrame displayFrame;
	public int displayWidthPixels = 500;
	public int displayHeightPixels = 500;
	public String screenshotDir = null;
	public long sscounter = 0;
	public void setDisplaySize(int width, int height){ displayWidthPixels = width; displayHeightPixels = height; }
	public ContinuousPortrayal2D field2DPortrayal = new ContinuousPortrayal2D(){
		final SimplePortrayal2D EMPTY_PORTRAYAL = new SimplePortrayal2D();
		public Portrayal getDefaultPortrayal(){ return EMPTY_PORTRAYAL; }
	};
	public HashMap<Class<?>,Class<? extends Portrayal>> displayMap = new HashMap<Class<?>,Class<? extends Portrayal>>();

	public GUISimulation(Simulation sim){ super(sim); }
	
	public boolean validSimState(SimState state){
		return state instanceof Simulation;
	}

	public void setPortrayalClass(Class<?> objClass, Class<? extends Portrayal> portrayalClass){
		displayMap.put(objClass,portrayalClass);
	}

	public void setupPortrayals(){
		if(state != null && state instanceof Simulation){
			Simulation sim = (Simulation)state;
			double fontScaleX = sim.env.width/displayWidthPixels;
			double fontScaleY = sim.env.height/displayHeightPixels;
			//System.out.println("field2d "+sim.field2D.width+" "+sim.field2D.height);
			field2DPortrayal.setField(sim.field2D);

			for(int i=0;i<sim.field2D.allObjects.numObjs;i++){
				Object tmpObj = sim.field2D.allObjects.objs[i];
				Class<? extends Portrayal> portClass = displayMap.get(tmpObj.getClass());
				Portrayal port = null;
				if(portClass != null){
					//user specified a portrayal, try to instantiate it
					try{
						port = portClass.getConstructor(Simulation.class,Object.class).newInstance(sim,tmpObj);
					} catch(NoSuchMethodException e){
						System.out.println("Constructor DNE:"+e);
					} catch(InstantiationException e){
						System.out.println("Tried to build an abstract class:"+e);
					} catch(IllegalAccessException e){
						System.out.println("Inaccessible constructor:"+e);
					} catch(IllegalArgumentException e){
						System.out.println("Argument error:"+e);
					} catch(InvocationTargetException e){
						System.out.println("Constructor threw an exception:"+e);
					}
				}
				if(port == null && tmpObj instanceof Body){
					port = new BasicBodyPortrayal(sim,tmpObj);
				} else if(port == null && tmpObj instanceof String){
					port = new LabelledPortrayal2D(new SimplePortrayal2D(),0,0,fontScaleX,fontScaleY,new Font(Font.SANS_SERIF,Font.PLAIN,10),LabelledPortrayal2D.ALIGN_LEFT,(String)tmpObj,Color.blue,false);
				} else if(port == null && tmpObj instanceof RectObstacle){
					port = new RectObstaclePortrayal((RectObstacle)tmpObj);
				}
				field2DPortrayal.setPortrayalForObject(tmpObj, port);
			}
			//reschedule the displayer
			display.reset();
			display.setBackdrop(Color.white);
			//redraw the display
			display.repaint();			
		} else {
			if(state == null){
				throw new RuntimeException("null SimState object"); 
			} else {
				throw new RuntimeException("!(state instanceof Simulation)");
			}
		}
	}

	public void start(){
		super.start();
		setupPortrayals();
	}
	
	public void load(SimState s){
		super.load(s);
		setupPortrayals();
	}
	
	public void init(Controller c){
		super.init(c);
		if(state instanceof Simulation){
			Simulation sim = (Simulation)state;
			display = new Display2D(displayWidthPixels,displayHeightPixels,this);
			display.setClipping(false);
			displayFrame = display.createFrame();
			displayFrame.setTitle("BioSim");
			c.registerFrame(displayFrame);
			displayFrame.setVisible(true);
			display.attach(field2DPortrayal,"BioSim");
			display.setBackdrop(Color.white);
		} else{
			throw new RuntimeException("!(state instanceof Simulation)");
		}
	}

	public boolean step(){
		boolean rv = super.step();
		if(screenshotDir!=null){
			File outf = new File(screenshotDir,"sshot"+(sscounter++)+".png");
			try{
				display.takeSnapshot(outf,display.TYPE_PNG);
			} catch (IOException ioe){
				System.err.println("Error saving screenshot to "+outf);
				//welp, that didn't work, lets not keep trying
				screenshotDir = null;
			}
		}
		return rv;
	}
}
