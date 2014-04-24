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

import hipi.image.ImageHeader.ImageType;
import hipi.imagebundle.AbstractImageBundle;
import hipi.imagebundle.HipiImageBundle;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class NLMeans extends Configured implements Tool {

  public static void main(String[] args) throws Exception {
    createHipiImageBundle(args);
    int status = ToolRunner.run(new Configuration(), new NLMeans(), args);
    System.exit(status);
  }

    private static void createHipiImageBundle (String args[]) throws IOException {
        File folder = new File(args[0]);
        File[] files = folder.listFiles();
        Configuration conf = new Configuration();
        HipiImageBundle hib = new HipiImageBundle(new Path(args[1]), conf);
        hib.open(AbstractImageBundle.FILE_MODE_WRITE, true);
        for (File file : files) {
            FileInputStream fis = new FileInputStream(file);
            String fileName = file.getName().toLowerCase();
            String suffix = fileName.substring(fileName.lastIndexOf('.'));
            if (suffix.compareTo(".jpg") == 0 || suffix.compareTo(".jpeg") == 0) {
                hib.addImage(fis, ImageType.JPEG_IMAGE);
            } else if (suffix.compareTo(".png") == 0) {
                hib.addImage(fis, ImageType.PNG_IMAGE);
            }
        }
        hib.close();
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
        job.getConfiguration().set("radio_sim","3");
        job.getConfiguration().set("radio_search","7");
        job.getConfiguration().set("degree","30");
        job.getConfiguration().set("output_path", args[2]);

        FileOutputFormat.setOutputPath(job, new Path(args[2]));
        FileInputFormat.setInputPaths(job, new Path(args[1]));

        // Let HIPI do its optimizations and stuff
        job.setCompressMapOutput(true);
        job.setMapSpeculativeExecution(true);
        job.setReduceSpeculativeExecution(true);

        boolean success = job.waitForCompletion(true);
        return success ? 0 : 1;
    }
}
