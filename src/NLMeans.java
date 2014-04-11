// Rashmi Avancha
// Sriram Balasubramaniam
// Brian Stebar
// 
// CS 6675 - Spring 2014
// Term Project - Image Denoising with MapReduce

import hipi.image.FloatImage;
import hipi.imagebundle.mapreduce.HipiJob;

import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.util.*;

public class NLMeans extends Configured implements Tool {

  public static void main(String[] args) throws Exception {
    int status = ToolRunner.run(new Configuration(), new NLMeans(), args);
    System.exit(status);
  }

  //
  // run
  //
  // Driver for the NL-means algorithm MapReduce job
  //
  public int run(String[] args) throws Exception {
    HipiJob job = new HipiJob(getConf(), "NLMeans");
    job.setJarByClass(NLMeans.class);

    // Define the types of output I expect from the reducer
    job.setOutputKeyClass(IntWritable.class);
    job.setOutputValueClass(FloatImage.class);

    // Tell Hadoop what mappers and reducers to use
    job.setMapperClass(NLMeansMapper.class);
    job.setReducerClass(NLMeansReducer.class);

    //
    // INPUT AND OUTPUT DEFINITIONS HERE
    //

    // Let HIPI do its optimizations and stuff
    job.setCompressMapOutput(true);
    job.setMapSpeculativeExecution(true);
    job.setReduceSpeculativeExecution(true);

    boolean success = job.waitForCompletion(true);
    return success ? 0 : 1;
  }
}
