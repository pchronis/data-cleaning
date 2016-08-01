import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;


public class main {

	public static void main(String[] args) throws NumberFormatException, IOException, ParseException {
		// TODO Auto-generated method stub
		
		if(args[0].equals("align")){
			DataAligner al = new DataAligner();
			al.correctAlignement(args[1], args[2],args[3], args[4], Integer.parseInt(args[5]),Double.parseDouble(args[6]), Boolean.parseBoolean(args[7]));
		}
		if(args[0].equals("clean")){
			DataCleaner cl = new DataCleaner();
			cl.cleanTimeSeries(args[1], args[2],args[3], Integer.parseInt(args[4]), Integer.parseInt(args[5]), Integer.parseInt(args[6]), Double.parseDouble(args[7]), Double.parseDouble(args[8]), Boolean.parseBoolean(args[9]));
		}
	}
}
