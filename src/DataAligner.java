import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;


public class DataAligner {

	FileWriter fw=null;
	CSVPrinter csv=null;
	DataReader data=null;
	int[] counts;

	public int firstIndex(Calendar cal,long zeroHour){
		return (int) ((cal.getTimeInMillis()-zeroHour)/60000);
	}

	public int lastIndex(Calendar cal,long zeroHour){
		return (int) ((cal.getTimeInMillis()-zeroHour)/60000);
	}

	public double[] makeMinutes(ArrayList<DataPair> ts, long zeroHour,int days){
		double[] minutes = new double[days*24*60];
		int start,end;
		double cons=0.0;
		Calendar prev,cur;
		for(int i=1;i<ts.size();i++){
			cur  = ts.get(i).cal;
			prev = ts.get(i-1).cal;
			start = firstIndex(prev,zeroHour);
			end = lastIndex(cur,zeroHour);
			cons = ts.get(i).consumption/(end-start);
			for(int j=start;j<end;j++)
				minutes[j]+=cons;
		}
		return minutes;
	}

	public ArrayList<DataPair> delta(ArrayList<DataPair> data) throws ParseException{

		ArrayList<DataPair> tData = new ArrayList<DataPair>(data.size()-1);
		for(int i=1;i<data.size();i++)
			tData.add(new DataPair(data.get(i).date, data.get(i).cal, data.get(i).consumption-data.get(i-1).consumption));
		return tData;

	}

	public ArrayList<DataPair> fixTS(ArrayList<DataPair> ts, String startTime, int days) throws ParseException{

		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		df.setTimeZone(tz);
		long zeroHour = df.parse(startTime).getTime();

		double[] minutes = makeMinutes(ts,zeroHour,days);
		ArrayList<DataPair> fixed = makeTS(minutes,zeroHour);

		return fixed;

	}

	public ArrayList<DataPair> makeTS(double[] minutes,long zeroHour) throws ParseException{

		ArrayList<DataPair> fixed = new ArrayList<DataPair>();

		TimeZone tz = TimeZone.getTimeZone("UTC");
		Calendar cal = new GregorianCalendar(tz);
		cal.setTimeInMillis(zeroHour);

		double sum;
		for(int i=0;i<minutes.length;i+=60){
			sum=0;
			for(int j=0;j<60;j++)
				sum+=minutes[i+j];
			cal.add(Calendar.HOUR_OF_DAY, 1);
			fixed.add(new DataPair((Calendar)cal.clone(),sum));
		}

		return fixed;

	}

	public void writeTS(CSVPrinter csv,ArrayList<DataPair> ts, String id) throws NumberFormatException, ParseException, IOException{

		ArrayList<String> record;
		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		df.setTimeZone(tz);
		for(DataPair dp:ts){
			record=new ArrayList<String>();
			record.add(id);
			record.add(df.format(dp.cal.getTime()));
			record.add(Double.toString(dp.consumption));		
			csv.printRecord(record);
		}
	}

	public boolean checkShort(ArrayList<DataPair> ts, int days, double shortThresh){
		if((1-shortThresh)*days*24>ts.size())
			return true;
		else
			return false;		
	}

	public void initAlign(String inputPath,String outputPath,boolean suppressWrite) throws IOException{
		counts = new int[2];
		System.out.println("reading csv");
		data = new DataReader(inputPath);
		if(!suppressWrite){
			fw = new FileWriter(outputPath);
			csv = new CSVPrinter(fw,CSVFormat.DEFAULT.withDelimiter(';'));
			String[] header = {"household_id","date-time","consumption"};
			csv.printRecord(header);		
		}
	}

	public void addTsToCountsAlign(boolean isShort){
		counts[0]++;
		if(isShort)
			counts[1]++;
	}

	private void generateAlignmentReadme(String readmePath) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(readmePath);
		pw.println("Total time series : "+counts[0]);
		pw.print("Time series with length below threshold : "+counts[1]);
		pw.close();
	}

	public void correctAlignement(String inputPath,String outputPath,String readmePath,String startTime, int days,double shortThresh,boolean suppressWrite) throws IOException, NumberFormatException, ParseException{

		ArrayList<DataPair> ts;
		boolean isShort=false;

		initAlign(inputPath,outputPath,suppressWrite);
		System.out.println("processing time series");

		while(data.hasMore){
			ts = data.getNextSeries();
			isShort = checkShort(ts,days,shortThresh);
			addTsToCountsAlign(isShort);
			handleWriteAlign(ts,data.name,startTime,days,isShort,suppressWrite);
		}
		
		cleanUp(suppressWrite);
		generateAlignmentReadme(readmePath);
	}

	public void handleWriteAlign(ArrayList<DataPair> ts,String id,String startTime, int days, boolean isShort,boolean suppressWrite) throws NumberFormatException, ParseException, IOException{
		if(!(isShort || suppressWrite)){
			ts = fixTS(delta(ts),startTime,days);
			writeTS(csv,ts,id);
		}
	}
	
	public void cleanUp(boolean suppressWrite) throws IOException{
		if(!suppressWrite){
			csv.close();
			fw.close();
		}
	}

}
