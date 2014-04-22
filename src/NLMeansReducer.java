import hipi.image.FloatImage;
import hipi.image.ImageHeader;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.*;

//
// NLMeansMap
//
// Reducer class for the NL-means algorithm
//
public class NLMeansReducer extends
    Reducer<IntWritable, FloatImage, IntWritable, FloatImage> {

  public void reduce(IntWritable key, Iterable<FloatImage> values, 
      Context context) {

      int counter = 1;
      for (FloatImage fi: values) {
          try {
              context.write(new IntWritable(counter++), fi);
          }
          catch (Exception e) {
              e.printStackTrace();
          }
      }
  }
}