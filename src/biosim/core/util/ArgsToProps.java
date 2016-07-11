// ArgsToProps.java

package biosim.core.util;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

public class ArgsToProps{
	private ArgsToProps(){}
	public static Properties parse(String[] args) throws IOException{
		return parse(args,null);
	}
	public static Properties parse(String[] args, Properties defaults) throws IOException{
		Properties rv = new Properties(defaults);
		String buff = "";
		for(int i=0;i<args.length;i++){
			buff += args[i]+"\n";
		}
		rv.load(new StringReader(buff));
		return rv;
	}
}