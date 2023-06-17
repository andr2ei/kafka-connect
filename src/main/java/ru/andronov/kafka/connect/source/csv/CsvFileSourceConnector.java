package ru.andronov.kafka.connect.source.csv;


import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.source.SourceConnector;
import org.apache.kafka.connect.util.ConnectorUtils;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class CsvFileSourceConnector extends SourceConnector {

    public static final String DIR_CONFIG = "working.dir";
    public static final String FILES_CONFIG = "files.to.process";
    public static final String TOPIC_CONFIG = "topic";
    private Map<String, String> props;
    private Boolean inputMonitorEnabled;
    private AtomicInteger filesNumber = new AtomicInteger();

    private Thread inputMonitorThread;

    private static final ConfigDef CONFIG_DEF = new ConfigDef()
            .define(DIR_CONFIG, ConfigDef.Type.STRING, null, ConfigDef.Importance.HIGH, "Dir with files")
            .define(TOPIC_CONFIG, ConfigDef.Type.STRING, null, ConfigDef.Importance.HIGH, "Kafka topic");

    @Override
    public void start(Map<String, String> props) {
        this.props = props;
        System.out.println("Start method completed");
    }

    @Override
    public Class<? extends Task> taskClass() {
        return CsvStreamSourceTask.class;
    }

    @Override
    public List<Map<String, String>> taskConfigs(int maxTasks) {
        if (inputMonitorThread != null && inputMonitorEnabled) {
            inputMonitorEnabled = false;
            inputMonitorThread.interrupt();
        }
        File[] files = getFiles();
        filesNumber.set(files.length);
        if (files.length == 0) return new ArrayList<>();
        List<Map<String, String>> taskPropsList = getTaskPropsList(maxTasks, files);

        inputMonitorEnabled = true;
        inputMonitorThread = new Thread(() -> {
            while (inputMonitorEnabled) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    System.out.println("Thread was interrupted");
                }
                File[] curFiles = getFiles();
                if (curFiles.length != filesNumber.get()) {
                    System.out.println("Requesting task reconfiguration");
                    filesNumber.set(curFiles.length);
                    this.context.requestTaskReconfiguration();
                }
            }
        });
        inputMonitorThread.start();

        return taskPropsList;
    }

    private List<Map<String, String>> getTaskPropsList(int maxTasks, File[] files) {
        List<String> filesList = Arrays.stream(files)
                .map(File::getAbsolutePath)
                .collect(Collectors.toList());
        System.out.println("Files from working dir" + String.join(";", filesList));

        List<List<String>> partitionedFiles = ConnectorUtils.groupPartitions(filesList, maxTasks);

        return partitionedFiles.stream()
                .map(l -> String.join(";", l))
                .map(fStr -> {
                    HashMap<String, String> taskProps = new HashMap<>();
                    taskProps.put(FILES_CONFIG, fStr);
                    taskProps.put(TOPIC_CONFIG, props.get(TOPIC_CONFIG));
                    return taskProps;
                }).collect(Collectors.toList());
    }

    private File[] getFiles() {
        String dir = props.get(DIR_CONFIG);
        File dirFile = new File(dir);
        if(!dirFile.isDirectory()) throw new IllegalArgumentException("Directory cannot be file " + dir);
        return dirFile.listFiles();
    }

    @Override
    public void stop() {
        inputMonitorEnabled = false;
        inputMonitorThread.interrupt();
        System.out.println("Stop method completed");
    }

    @Override
    public ConfigDef config() {
        return CONFIG_DEF;
    }

    public String version() {
        return "1";
    }
}
