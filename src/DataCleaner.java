import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.TimeZone;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;


public class DataCleaner {

	int[] counts;
	FileWriter fw=null;
	CSVPrinter csv=null;
	DataReader data=null;
	
	
	public void writeOver(ArrayList<DataPair> ts, int index, int type){
		
		if(type==1 && index>0)
			ts.set(index, new DataPair(ts.get(index).cal,ts.get(index-1).consumption));
		
		if(type==2)
			if(index>=24)
				ts.set(index, new DataPair(ts.get(index).cal,ts.get(index-24).consumption));
			else
				ts.set(index, new DataPair(ts.get(index).cal,ts.get(index-1).consumption));
		
		if(type==3 && index>=34 && index < ts.size()-10 )
			for(int i=-10;i<11;i++)
				ts.set(index, new DataPair(ts.get(index+i).cal,ts.get(index-24+i).consumption));
	
	}
	
	public void updateExtremeCounts(boolean isDirtyHigh,boolean isDirtyLow,int type){
		if(isDirtyHigh)
			counts[2]++;
		if(isDirtyLow)
			counts[3]++;
		if(type!=4 && (isDirtyLow || isDirtyHigh))
			counts[7]++;
	}
	
	public boolean determineExtremeReturn(int type,boolean isDirtyHigh,boolean isDirtyLow){
		if(type==4 && (isDirtyHigh || isDirtyLow))
			return true;
		else
			return false;
	}
	
	public boolean handleExtreme(ArrayList<DataPair> ts, int type, int lowThresh,int highThresh){
		boolean isDirtyHigh = false;
		boolean isDirtyLow = false;
		
		for(int i=0;i<ts.size();i++){
			
			if(ts.get(i).consumption>highThresh){
				counts[0]++;
				isDirtyHigh=true;
				writeOver(ts,i,type);
			}
			
			if(ts.get(i).consumption<lowThresh){
				counts[1]++;
				isDirtyLow=true;
				writeOver(ts,i,type);
			 }
		
		}
		updateExtremeCounts(isDirtyHigh,isDirtyLow,type);
		return determineExtremeReturn(type,isDirtyHigh,isDirtyLow);

	}
	
	public ArrayList<Double> getMonths(ArrayList<DataPair> series){
		ArrayList<Double> months = new ArrayList<Double>();
		
		for(int i=0;i<series.size()-720;i+=720)
			months.add(sum(series,i,i+720));
	
		Collections.sort(months);
		return months;	
	
	}
	
	public int findStart(ArrayList<Double> months, double timeThresh){
		return (int)Math.round(months.size()*(1-timeThresh));
	}
	
	public boolean checkCountryCondition(double partialCons,double consThresh,double totalCons){
		if(partialCons>consThresh*totalCons){
			counts[4]++;
			return true;
		}
		else
			return false;
	}
	
	public double getPartialCons(ArrayList<Double> months,int start,int end){
		double cons=0;
		for(int i=start;i<end;i++)
			cons+=months.get(i);
		return cons;
	}
	
	public boolean handleCountry(ArrayList<DataPair> year,double timeThresh,double consThresh){
		ArrayList<Double> months = getMonths(year);
		Double totalCons = sum(year,0,year.size());
		
		Integer start=findStart(months,timeThresh),end=months.size();
		double partialCons = getPartialCons(months,start,end);
	
		return checkCountryCondition(partialCons,consThresh,totalCons);		
	}
	
	public double sum(ArrayList<DataPair> ts, int start, int end){
		double sum= 0;
		for (int i=start; i<end;i++)
			sum+=ts.get(i).consumption;
		return sum;
	}
	
	public void initClean(String inputPath,String outputPath,boolean suppressWrite) throws IOException{
		counts = new int[9];
		System.out.println("reading csv");
		data = new DataReader(inputPath);
		if(!suppressWrite){
			fw = new FileWriter(outputPath);
			csv = new CSVPrinter(fw,CSVFormat.DEFAULT.withDelimiter(';'));
			String[] header = {"household_id","date-time","consumption"};
			csv.printRecord(header);		
		}
	}
	
	public void updateTotalCounts(ArrayList<DataPair> ts,boolean extreme,boolean country){
		counts[5]++;
		counts[6]+=ts.size();
		if (extreme || country)
			counts[8]++;
	}
	
	public void handleWrites(ArrayList<DataPair> ts,String id,boolean extreme,boolean country,boolean suppressWrite) throws NumberFormatException, ParseException, IOException{
		if(!(extreme || country || suppressWrite))
			writeTS(csv,ts,id);
	}
	
	public void cleanUp(boolean suppressWrite) throws IOException{
		if(!suppressWrite){
			csv.close();
			fw.close();
		}
	}
	
	private void writeTS(CSVPrinter csv,ArrayList<DataPair> ts, String id) throws NumberFormatException, ParseException, IOException{

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
	
	public void cleanTimeSeries(String inputPath,String outputPath,String readmePath,int type,int lowThresh,int highThresh,double timeThresh,double consThresh,boolean suppressWrite) throws NumberFormatException, ParseException, IOException{
		
		ArrayList<DataPair> ts;
		
		initClean(inputPath,outputPath,suppressWrite);
		System.out.println("processing time series");
		
		boolean extreme,country;
		while(data.hasMore){
			ts = data.getNextSeries();
			extreme = handleExtreme(ts,type,lowThresh,highThresh);
			country = handleCountry(ts,timeThresh,consThresh);
			updateTotalCounts(ts,extreme,country);
			handleWrites(ts,data.name,extreme,country,suppressWrite);
		}
		
		cleanUp(suppressWrite);
		generateCleaningReadme(readmePath);
	}
	
	private void generateCleaningReadme(String readmePath) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(readmePath);
		pw.println("Total data : "+counts[6]+" records, in "+counts[5]+" time series.");
		pw.println("Below low threshold : "+counts[1]+" records in "+counts[3]+" time series.");
		pw.println("Above high threshold : "+counts[0]+" records in "+counts[2]+" time series.");
		pw.println("Identified as country houses: " + counts[4] + " time series.");
		pw.println(counts[7]+" time series were modified.");
		pw.println(counts[8]+" time series were removed.");
		pw.close();	
	}
	
}
