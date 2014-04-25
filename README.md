MapReduce: Non-local Means Image Denoising 
==========================================

SYSTEM REQUIREMENTS
-------------------
This job was developed and tested using the following system configuration:
 - Ubuntu 12.04 LTS 64-bit
 - Oracle JDK 1.8.0
 - Apache Hadoop 1.2.1 (in standalone configuration)



TO COMPILE
----------
From the root folder of the project, use the 'make jar' command.  



TO RUN
------
NLMeans references two JAR dependencies. Since theose JARs are not bundled into
the NLMeans.jar file, they must be added to the HADOOP_CLASSPATH environment 
variable:

$ export HADOOP_CLASSPATH=/<PROJECT_ROOT>/hipi-dev/releases/hipi-0.0.1.jar: \
  /<PROJECT_ROOT>/hipi-dev/3rdparty/metadata-extractor-2.3.1.jar:$HADOOP_CLASSPATH

Extra care must be taken in distributed Hadoop environments to ensure that these
JARs are included on the classpath on each node.

The job can be executed using the following command:

$ hadoop jar NLMeans.jar NLMeans <INPUT_FOLDER_PATH> <HIB_FILE_PATH> <OUTPUT_FOLDER_PATH>

  INPUT_FOLDER_PATH  - path to folder containing the images to be processed
  HIB_FILE_PATH      - path to HIB file (essentially a temp file)
  OUTPUT_FOLDER_PATH - path to folder where output should be stored

To estimate runtimes, expect throughput to be on the order of a few megapixels
per hour per mapper.
