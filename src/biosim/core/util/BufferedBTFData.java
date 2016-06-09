package biosim.core.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;

public class BufferedBTFData extends BTFData{
	public HashMap<String,ArrayList<String> > data;
	public BufferedBTFData(BTFData backing){
		data = new HashMap<String, ArrayList<String> >();
		if(backing != null) this.columns = backing.columns;
	}
	public String[] loadColumn(String columnName) throws IOException{
		ArrayList<String> rv = data.get(columnName);
		if(rv==null){
			rv = new ArrayList<String>(Arrays.asList(super.loadColumn(columnName)));
			data.put(columnName,rv);
		}
		return rv.toArray(new String[rv.size()]);
	}
	public void loadBuffer() throws IOException{
		for(String cname : columns.keySet()){
			loadColumn(cname);
		}
	}
}
