package logtime;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class logtime {

    public static class LogMapper extends Mapper<LongWritable, Text, Text, IntWritable> {

        private final static SimpleDateFormat format = new SimpleDateFormat("M/d/yyyy H:mm");
        private Text user = new Text();

        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            try {
                String[] parts = value.toString().split(",");

                if (parts.length < 8) return;

                String ip = parts[1].trim();
                String loginStr = parts[5].trim();
                String logoutStr = parts[7].trim();

                Date login = format.parse(loginStr);
                Date logout = format.parse(logoutStr);

                long duration = (logout.getTime() - login.getTime()) / (1000 * 60); // minutes

                user.set(ip);
                context.write(user, new IntWritable((int) duration));
            } catch (Exception e) {
                System.err.println("Skipping line: " + value.toString());
            }
        }
    }

    public static class LogReducer extends Reducer<Text, IntWritable, Text, IntWritable> {

        public void reduce(Text key, Iterable<IntWritable> values, Context context)
                throws IOException, InterruptedException {

            int totalTime = 0;
            for (IntWritable val : values) {
                totalTime += val.get();
            }

            context.write(key, new IntWritable(totalTime));
        }
    }

    public static void main(String[] args) throws Exception {

        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "Log Duration Calculator");

        job.setJarByClass(logtime.class);
        job.setMapperClass(LogMapper.class);
        job.setReducerClass(LogReducer.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}

/*
Expected CSV input format (comma-separated):

MAC Address,IP Address,X,Y,Z,Login Time,MAC Address,Logout Time

Example lines:
00-01-6C-D0-9F-25,10.10.10.221,1,8,1,1/6/2018 7:13,00-01-6C-D0-9F-25,1/6/2018 7:40
00-01-6C-D0-9F-25,10.10.13.167,1,13,1,1/6/2018 11:22,00-01-6C-D0-9F-25,1/6/2018 11:22
00-01-6C-D0-9F-25,10.10.10.14,1,6,1,1/6/2018 17:02,00-01-6C-D0-9F-25,1/6/2018 17:12
00-01-6C-D0-9F-25,10.10.10.220,1,13,1,1/6/2018 0:00,00-01-6C-D0-9F-25,1/6/2018 3:14

Note:
- Time format must be M/d/yyyy H:mm (e.g., 1/6/2018 7:13)
- Fields must be comma-separated, no tabs
*/
