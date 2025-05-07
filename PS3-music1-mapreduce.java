package music1;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class music1 {

    public static class MusicMapper extends Mapper<LongWritable, Text, Text, Text> {
        private Text trackId = new Text();
        private Text outputValue = new Text();

        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String[] fields = value.toString().split(",");

            // Extract Track id and shared values
            String userId = fields[0];
            String track = fields[1];
            String shared = fields[2];

            // Set the trackId for the key
            trackId.set(track);

            // Emit (trackId, "USER:<userId>") for unique listeners (one for each user)
            outputValue.set("USER:" + userId);
            context.write(trackId, outputValue);

            // If the event is Shared (i.e 1) emit the (TrackId, "SHARE:1") for shared count
            if (shared.equals("1")) {
                outputValue.set("SHARE:1");  // We set the outputValue to "SHARE:1" for shared event
                context.write(trackId, outputValue);
            }
        }
    }

    public static class MusicReducer extends Reducer<Text, Text, Text, Text> {
        private Text result = new Text();

        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            Set<String> uniqueListeners = new HashSet<>();
            int sharedCount = 0;

            // Iterate through all the values for the given trackID
            for (Text val : values) {
                String v = val.toString();

                if (v.startsWith("USER:")) {
                    String userId = v.substring(5);
                    uniqueListeners.add(userId);
                } else if (v.startsWith("SHARE:")) {
                    sharedCount++;
                }
            }

            // Set the result to trackID and the count of unique listeners and Shared
            result.set("Unique Listeners: " + uniqueListeners.size() + ", Shared: " + sharedCount);
            context.write(key, result);
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "Music Analysis");

        job.setJarByClass(music1.class);
        job.setMapperClass(MusicMapper.class);
        job.setReducerClass(MusicReducer.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}

/*
CSV Input Format (comma-separated):

UserId,TrackId,Shared,Radio,Skip

Example lines:
111115,222,0,1,0
111113,225,1,0,0
111117,223,0,1,1
111115,225,1,0,0
111116,225,0,1,0
111117,225,1,1,0
111118,225,0,1,0
111119,225,0,1,0
111119,225,1,0,1
111120,225,0,1,0
111117,222,1,1,0
111121,225,0,1,0
111122,226,0,1,1
111117,223,0,1,1
111118,226,0,0,1
111119,223,1,1,0
111120,226,1,0,0
111121,223,1,1,0
111122,224,1,0,0
111123,228,0,1,0
111124,229,0,1,0
*/
