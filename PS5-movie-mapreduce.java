package movie;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class movie {

    public static class MovieMapper extends Mapper<LongWritable, Text, Text, FloatWritable> {
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            if (key.get() == 0 && value.toString().contains("userId")) return;
            String[] fields = value.toString().split(",");
            if (fields.length >= 3) {
                context.write(new Text(fields[1]), new FloatWritable(Float.parseFloat(fields[2])));
            }
        }
    }

    public static class MovieReducer extends Reducer<Text, FloatWritable, Text, FloatWritable> {
        Map<Text, FloatWritable> avgMap = new HashMap<>();

        public void reduce(Text key, Iterable<FloatWritable> values, Context context) {
            float sum = 0; int count = 0;
            for (FloatWritable v : values) { sum += v.get(); count++; }
            float avg = sum / count;
            avg = Float.parseFloat(String.format("%.2f", avg)); // format to 2 decimal places
            avgMap.put(new Text(key), new FloatWritable(avg));
        }

        protected void cleanup(Context context) throws IOException, InterruptedException {
            avgMap.entrySet().stream()
                .sorted(Map.Entry.<Text, FloatWritable>comparingByValue(
                    Comparator.comparing(FloatWritable::get)).reversed())
                .forEach(entry -> {
                    try {
                        context.write(entry.getKey(), entry.getValue());
                    } catch (IOException | InterruptedException e) { e.printStackTrace(); }
                });
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "Movie Rating Avg Desc");
        job.setJarByClass(movie.class);
        job.setMapperClass(MovieMapper.class);
        job.setReducerClass(MovieReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(FloatWritable.class);
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}

/*
CSV FORMAT:
userId,movieId,rating,timestamp
1,31,2.5,1260759144
1,1029,3.0,1260759179
1,1061,3.0,1260759182
1,1129,2.0,1260759185
1,1172,4.0,1260759205
1,1263,2.0,1260759151
1,1287,2.0,1260759187
1,1293,2.0,1260759148
1,1339,3.5,1260759125
1,1343,2.0,1260759131
1,1371,2.5,1260759135
1,1405,1.0,1260759203
*/
