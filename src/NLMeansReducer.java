import hipi.image.FloatImage;
import hipi.image.ImageHeader;
import hipi.image.io.*;

import java.io.FileOutputStream;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.*;

//
// NLMeansMap
//
// Reducer class for the NL-means algorithm
//
public class NLMeansReducer extends
    Reducer<IntWritable, FloatImage, IntWritable, IntWritable> {

  public void reduce(IntWritable key, Iterable<FloatImage> values, 
    Context context) {
    Configuration conf = context.getConfiguration();

    int counter = 1;
    for (FloatImage fi: values) {
      try {
        Path outputPath = new Path(conf.get("output_path"), counter + ".png");
        FileOutputStream pos = new FileOutputStream(outputPath.toString());
        PNGImageUtil.getInstance().encodeImage(fi, null, pos);
        
        context.write(new IntWritable(counter++), new IntWritable(1));
      }
      catch (Exception e) {
        e.printStackTrace();  
      }
    }
  }
}