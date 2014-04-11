# Rashmi Avancha
# Sriram Balasubramaniam
# Brian Stebar

# CS 6675 - Spring 2014
# Term Project - Image Denoising with MapReduce

JC = javac
JARPATH = `hadoop classpath`:./hipi-dev/release/*:./src
JFLAGS = -cp $(JARPATH)

JARC = jar
JARFLAGS = cvf
JARFILE = NLMeans.jar 

DRIVERJAVAFILE = NLMeans.java
DEPSJAVAFILES =  NLMeansMapper.java NLMeansReducer.java

JAVAFILES = $(DRIVERJAVAFILE) $(DEPSJAVAFILES)
JAVAFILEPATHS := $(addprefix src/, $(JAVAFILES))
CLASSFILES = $(JAVAFILES:.java=.class)

.SUFFIXES: .java .class

default: classes

.java.class:
	$(JC) $(JFLAGS) $*.java

classes: $(JAVAFILEPATHS:.java=.class)

jar: classes
	$(JARC) $(JARFLAGS) $(JARFILE) $(addprefix -C src , $(CLASSFILES))

clean:
	$(RM) src/*.class *.jar
