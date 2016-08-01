# data-cleaning

Source for aligning and cleaning the data

Arguments for data alignment:

java -jar DataPreproc.jar align inputPath outputPath readmePath startingDate numOfDays missingRatio suppressWrite

align: Selects the alignment operation.

inputPath: The path of the input csv.

outputPath: The path for the output csv.

readmePath: Path for the generated readme.

startingDate: The date of the start of the measurements in format "yyyy-MM-dd hh:mm".

numOfDays: The duration of the measurement period in days.

missingRatio: If more than missingRatio*totalExpectedLabels are missing the time series is discarded. Takes values in [0, 1].

suppressWrite: If true the output file is not written and only the readme is generated. If false both the output and the readme are written.

Example:

java -Xmx7g -jar DataPreproc.jar align /home/pant/Desktop/data_cleaning/data_in.csv /home/pant/Desktop/data_cleaning/data_out.csv /home/pant/Desktop/data_cleaning/align_readme "2013-07-01 00:00" 365 0.05 false

Arguments for data cleaning:

java -jar DataPreproc.jar clean inputPath outputPath readmePath type lowThresh highThresh timeThresh consThresh suppressWrite

clean: Selects the cleaning operation.

inputPath: The path of the input csv.

outputPath: The path for the output csv.

readmePath: Path for the generated readme.

type: Controls the type of cleaning operation. Takes values in {1,2,3,4}. For type==1 the program replaces an erroneous measurement at index i with the measurment at index i-1. For type==2 it replaces broken measurment i with i-24. For type==3 it replaces [i-10, i+10] with [i-24-10, i-24+10]. For type==4 it discards the time series.

lowThesh,highThresh: A measurement x is considered erroneous if x<lowThresh || x>highThresh. Both parameters take integer values.

timeThresh,consThresh: A time series is considered country-house/abnormal if it has more than consThresh of the total consumption in less than timeThresh of the total months (for example more than 0.8*totalConsumption in less than 0.5*totalMonths). Both parameters take values in [0, 1].

suppressWrite: If true the output file is not written and only the readme is generated. If false both the output and the readme are written.

Example:

java -Xmx7g -jar DataPreproc.jar clean /home/pant/Desktop/data_cleaning/data_out.csv /home/pant/Desktop/data_cleaning/data_clean.csv /home/pant/Desktop/data_cleaning/clean_readme 2 -5 1000 0.5 0.8 false

Note: For the 1000 time series both operations require >6 gb of ram. 
