package org.example;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import java.util.HashMap;

import java.io.IOException;
import java.util.Map;

public class ReduceSideJoin1 extends Configured implements Tool {

    public static void main(String[] args) throws Exception {
        // ReduceSideJoin1 실행
        int exitCode = ToolRunner.run(new ReduceSideJoin1(), args);
        System.exit(exitCode);
    }

    @Override
    public int run(String[] args) throws Exception {
        Job job = Job.getInstance(getConf(), "ReduceSideJoin1");

        job.setJarByClass(ReduceSideJoin1.class);
        job.setReducerClass(ReduceSideJoin1Reducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        MultipleInputs.addInputPath(job, new Path(args[0]), TextInputFormat.class, EmployeeMapper.class);
        MultipleInputs.addInputPath(job, new Path(args[1]), TextInputFormat.class, DepartmentMapper.class);

        FileOutputFormat.setOutputPath(job, new Path(args[2]));
        return job.waitForCompletion(true) ? 0 : 1;
    }

    public static class EmployeeMapper extends Mapper<LongWritable, Text, Text, Text> {
        private Text outKey = new Text();
        private Text outValue = new Text();

        @Override
        protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String[] columns = value.toString().split(",");
            outKey.set(columns[6]); // dept_no
            outValue.set("E" + "\t" + columns[0] + "\t" +  columns[2]+ "\t" + columns[4]); // emp_no, first_name, gender
            context.write(outKey, outValue);
        }
    }

    public static class DepartmentMapper extends Mapper<LongWritable, Text, Text, Text> {
        private Text outKey = new Text();
        private Text outValue = new Text();

        @Override
        protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String[] columns = value.toString().split(",");
            outKey.set(columns[0]); // dept_no
            outValue.set("D"+ "\t" + columns[1]); // dept_name
            context.write(outKey, outValue);
        }
    }

    public static class ReduceSideJoin1Reducer extends org.apache.hadoop.mapreduce.Reducer<Text, Text, Text, Text> {
        private Text outKey = new Text();
        private Text outValue = new Text();

        @Override
        protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            HashMap<String, String> employeeMap = new HashMap<String, String>();
            String deptName = "";

            for (Text value : values) {
                String[] columns = value.toString().split("\t");
                if (columns[0].equals("D")) {
                    deptName = columns[1];
                } else {
                    // emp_no 가 키
                    employeeMap.put(columns[1], columns[2] + "\t" + columns[3]);
                }
            }

            for (Map.Entry<String, String> e : employeeMap.entrySet()) {
                outKey.set(e.getKey());
                outValue.set(e.getValue() + "\t" + deptName);
                context.write(outKey, outValue);
            }

        }
    }

}
