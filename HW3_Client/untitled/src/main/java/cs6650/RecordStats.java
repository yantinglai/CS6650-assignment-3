package cs6650;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.csv.CSVFormat;
public class RecordStats {
    public static void recordStatistics(String filePath) throws Exception {
        try (Reader in = new FileReader(filePath)) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.parse(in);

            List<Integer> postLatency = new ArrayList<>();
            
            for (CSVRecord record : records) {
                Integer latency = Integer.parseInt(record.get(2));
                postLatency.add(latency);
            }

            double[] postlatencyArray = postLatency.stream().mapToDouble(d -> d).toArray();
            // Compute statistics
            DescriptiveStatistics poststats = new DescriptiveStatistics( postlatencyArray );
           
            System.out.println("Post Stats:");
            System.out.println("Mean: " + poststats .getMean() + " ms");
            System.out.println("Median: " +poststats .getPercentile(50) + " ms");
            System.out.println("99%: " + poststats .getPercentile(99) + " ms");
            System.out.println("Min: " + poststats .getMin() + " ms");
            System.out.println("Max: " + poststats .getMax() + " ms");
        }
    }
}


