import biosim.core.util.annwrapper.SimpleANN;
public class Test{
	public static void main(String[] args){
		System.loadLibrary("annwrapper");
		SimpleANN foo = new SimpleANN(3);
		double[] bar = {0.0, 10.0, 0.0};
		double[] bar2 = {0.0, 1.0, 0.0};
		int[] index = {0};
		foo.add(bar);
		foo.add(bar2);		
		foo.query(bar,index,1);
		System.out.println(index[0]);
		foo.query(bar2,index,1);
		System.out.println(index[0]);
	}
}
