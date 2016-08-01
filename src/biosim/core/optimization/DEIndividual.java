package biosim.core.optimization;
public class DEIndividual{
	public double[] phenotype;
	public Double fitness;
	public DEIndividual(){
		phenotype = null;
		fitness = null;
	}
	public DEIndividual add(DEIndividual other){
		DEIndividual rv = new DEIndividual();
		rv.phenotype = new double[phenotype.length];
		for(int i=0;i<phenotype.length;i++){
			rv.phenotype[i] = phenotype[i]+other.phenotype[i];
		}
		return rv;
	}
	public DEIndividual sub(DEIndividual other){
		DEIndividual rv = new DEIndividual();
		rv.phenotype = new double[phenotype.length];
		for(int i=0;i<phenotype.length;i++){
			rv.phenotype[i] = phenotype[i]-other.phenotype[i];
		}
		return rv;
	}
	public DEIndividual scale(double alpha){
		DEIndividual rv = new DEIndividual();
		rv.phenotype = new double[phenotype.length];
		for(int i=0;i<rv.phenotype.length;i++){
			rv.phenotype[i] = alpha*phenotype[i];
		}
		return rv;
	}
	public String toString(){
		if(phenotype == null) return "[]";
		String rv = "[";
		for(int i=0;i<phenotype.length;i++){
			rv += String.format(" %f",phenotype[i]);
		}
		rv += String.format(" ]:%f",fitness);
		return rv;
	}
}