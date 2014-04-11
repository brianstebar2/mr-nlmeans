import hipi.image.FloatImage;
import hipi.image.ImageHeader;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.*;

//
// NLMeansMap
//
// Mapper class for the NL-means algorithm
//
public class NLMeansMapper extends
    Mapper<ImageHeader, FloatImage, IntWritable, FloatImage> {

  public void map(ImageHeader key, FloatImage value, Context context) {

  }
}