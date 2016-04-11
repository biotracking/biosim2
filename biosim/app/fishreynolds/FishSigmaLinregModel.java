package biosim.app.fishreynolds;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

import sim.util.MutableDouble2D;

import biosim.core.learning.LinregModel;
import biosim.core.body.AbstractFish;

public class FishSigmaLinregModel extends LinregModel{

    public static double SEP_SIGMA=0.1;
    public static double ORI_SIGMA=0.2; //These two features are different from the optimal computed LR sigmas, but look better in simulation
    public static double COH_SIGMA=1.0;
    public static double OBS_SIGMA=0.05; //These two features are different from the optimal computed LR sigmas, but look better in simulation
    public static int NUM_FEATURES=8;//sepX, sepY, oriX, oriY, cohX, cohY, wallX, wallY

	public static void loadFeatureSigma(BufferedReader featureSigmasSource) throws IOException {
		ArrayList<String> sigmaLines = new ArrayList<String>();
		for(String line=null; featureSigmasSource.ready();){
			line = featureSigmasSource.readLine();
			sigmaLines.add(line);
		}
		int readNumSigmas = sigmaLines.size();
		if(NUM_FEATURES/2 != readNumSigmas){
			System.out.println("[FishReynolds] WARNING! Number of parsed sigmas ("+readNumSigmas+") different from NUM_FEATURES/2 ("+NUM_FEATURES/2+")");
		}
		SEP_SIGMA = Double.parseDouble(sigmaLines.get(0));
		ORI_SIGMA = Double.parseDouble(sigmaLines.get(1));
		COH_SIGMA = Double.parseDouble(sigmaLines.get(2));
		OBS_SIGMA = Double.parseDouble(sigmaLines.get(3));
	}

	public FishSigmaLinregModel(){
		this(NUM_FEATURES,3);
	}

	public FishSigmaLinregModel(int i, int j){
		super(i,j);
	}

	public double[] computeFeatures(AbstractFish fishBody){
		// order of sensors: sep, ori, coh, obs, bias
		MutableDouble2D sep = new MutableDouble2D();
		MutableDouble2D ori = new MutableDouble2D();
		MutableDouble2D coh = new MutableDouble2D();
		MutableDouble2D wall = new MutableDouble2D();
		fishBody.getAverageRBFSameTypeVec(sep,SEP_SIGMA);
		fishBody.getAverageRBFOrientationSameTypeVec(ori,ORI_SIGMA);
		fishBody.getAverageRBFSameTypeVec(coh,COH_SIGMA);
		fishBody.getNearestObstacleVec(wall);
		wall.multiplyIn(Math.exp(-wall.lengthSq()/(2.0*Math.pow(OBS_SIGMA,2))));
		double[] sensors = new double[NUM_FEATURES+1]; // +1 for bias
		sensors[0] = sep.x;
		sensors[1] = sep.y;
		sensors[2] = ori.x;
		sensors[3] = ori.y;
		sensors[4] = coh.x;
		sensors[5] = coh.y;
		sensors[6] = wall.x * Math.exp(-(Math.pow(wall.x,2)+Math.pow(wall.y,2))/(2.0*Math.pow(OBS_SIGMA,2)));
		sensors[7] = wall.y * Math.exp(-(Math.pow(wall.x,2)+Math.pow(wall.y,2))/(2.0*Math.pow(OBS_SIGMA,2)));
		sensors[8] = 1.0;
		return sensors;
	}

	public void loadParameters(BufferedReader source) throws IOException {
		ArrayList<String> coeffLines = new ArrayList<String>();
		for(String line=null; source.ready(); ){
			line = source.readLine();
			coeffLines.add(line);
		}
		int readNumCoeffs = coeffLines.size();
		if(readNumCoeffs!=NUM_FEATURES+1){
			throw new RuntimeException("Unexpected number of features read: "+readNumCoeffs+" (read) != "+(NUM_FEATURES+1)+" (expected)");
		}
		// System.out.println(coeffLines.size());
		// System.out.println(coeffLines.get(0));
		// System.out.println(coeffLines.get(0).split(" "));
		int readNumOutputs = coeffLines.get(0).split(" ").length;
		if(readNumOutputs!=3){
			throw new RuntimeException("Unexpected number of outputs: "+readNumOutputs+" (read) != 3 (expected)");
		}
		double[][] newParameters = new double[readNumCoeffs][readNumOutputs];
		for(int i=0;i<readNumCoeffs;i++){
			String[] tmp = coeffLines.get(i).split(" ");
			for(int j=0;j<readNumOutputs;j++){
				newParameters[i][j] = Double.parseDouble(tmp[j]);
			}
		}
		parameters = newParameters;
	}


}