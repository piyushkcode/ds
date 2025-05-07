package music2;

import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class music2 {

    public static class MusicMapper extends Mapper<LongWritable, Text, Text, Text> {
        private Text trackId = new Text();
        private Text outValue = new Text();

        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            // Skip header
            if (key.get() == 0 && value.toString().contains("UserId")) return;

            String[] fields = value.toString().split(",");
            if (fields.length < 5) return;

            String track = fields[1];
            String radio = fields[3];
            String skip = fields[4];

            trackId.set(track);

            if (radio.equals("1")) {
                outValue.set("RADIO");
                context.write(trackId, outValue);
            }
            if (skip.equals("1")) {
                outValue.set("SKIP");
                context.write(trackId, outValue);
            }
        }
    }

    public static class MusicReducer extends Reducer<Text, Text, Text, Text> {
        private Text result = new Text();

        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            int radioCount = 0;
            int skipCount = 0;

            for (Text val : values) {
                if (val.toString().equals("RADIO")) radioCount++;
                else if (val.toString().equals("SKIP")) skipCount++;
            }

            result.set("Radio Plays: " + radioCount + ", Skipped: " + skipCount);
            context.write(key, result);
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "Music Radio & Skip Count");

        job.setJarByClass(music2.class);
        job.setMapperClass(MusicMapper.class);
        job.setReducerClass(MusicReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));   // input: /user/hadoop/music2.csv
        FileOutputFormat.setOutputPath(job, new Path(args[1])); // output: /user/hadoop/output_music2

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}

/**
 CSV Format expected:
 UserId,TrackId,Shared,Radio,Skip

 Example:
 111115,222,0,1,0
 111117,223,0,1,1
 */