import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.ArrayList;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;


public class DataReader {

	BufferedReader br;
	ArrayList<DataPair> ts;
	ArrayList<CSVRecord> csv;
	String name;
	int index;
	boolean hasMore;
	
	public DataReader(String inputPath) throws IOException{
		readCSV(inputPath);
	}
	
	public void readCSV(String path) throws IOException{
		 Reader in = new FileReader(path);
		 CSVParser parser = new CSVParser(in, CSVFormat.DEFAULT.withDelimiter(';'));
		 csv = (ArrayList<CSVRecord>) parser.getRecords();
		 parser.close();
		 in.close();
		 
		 index = 1;
		 hasMore = true;
	}
	
	public void reset(){
		index=1;
		hasMore=true;
	}
	
	public ArrayList<DataPair> getNextSeries() throws NumberFormatException, ParseException{
		ts = new ArrayList<DataPair>();
		int i;
		name = csv.get(index).get(0);
		
		ts.add(new DataPair(csv.get(index).get(1),(double)Double.parseDouble(csv.get(index).get(2)),0));
		
		for(i=index+1;i<csv.size();i++){
			if( index==csv.size()-1 || csv.get(i).get(0).equals(csv.get(i-1).get(0)))
				ts.add(new DataPair(csv.get(i).get(1),(double)Double.parseDouble(csv.get(i).get(2)),0 ));
			else
				break;
		}
		
		index=i;
		if(index==csv.size())
			hasMore=false;
		
		return ts;
		
	}

	
}
