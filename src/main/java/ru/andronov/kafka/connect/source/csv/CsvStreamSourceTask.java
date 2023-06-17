package ru.andronov.kafka.connect.source.csv;

import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.apache.kafka.connect.source.SourceTaskContext;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class CsvStreamSourceTask extends SourceTask {

    public static final String FILENAME_FIELD = "filename";
    public static final String POSITION_FIELD = "position";
    private List<String> files;
    private Map<String, BufferedReader> fileReaderMap;
    private String topic;
    private Map<String, Long> fileRecordedOffsetMap;

    public String version() {
        return "1";
    }

    @Override
    public void initialize(SourceTaskContext context) {
        super.initialize(context);
        Map<String, String> configs = context.configs();
        this.fileRecordedOffsetMap = Arrays.stream(configs.get(CsvFileSourceConnector.FILES_CONFIG).split(";"))
                .filter(f -> !f.isEmpty())
                .map(f -> {
                    String[] fTokens = f.split("/");
                    return fTokens[fTokens.length-1];
                })
                .map(f -> {
                    Map<String, Object> defVals = new HashMap<>();
                    defVals.put(FILENAME_FIELD, f);
                    defVals.put(POSITION_FIELD, 0L);
                    Map<String, Object> offset = context.offsetStorageReader().offset(Collections.singletonMap(FILENAME_FIELD, f));
                    if (offset != null) {
                        Long lastRecordedOffset = (Long) offset.get(POSITION_FIELD);
                        if (lastRecordedOffset != null) {
                            HashMap<String, Object> vals = new HashMap<>();
                            vals.put(FILENAME_FIELD, f);
                            vals.put(POSITION_FIELD, lastRecordedOffset);
                            return vals;
                        } else {
                            return defVals;
                        }
                    } else {
                        return defVals;
                    }
                })
                .collect(Collectors.toMap(
                        m -> (String) m.get(FILENAME_FIELD),
                        m -> (Long) m.get(POSITION_FIELD)
                ));



    }

    @Override
    public void start(Map<String, String> props) {
        this.files = Arrays.stream(props.get(CsvFileSourceConnector.FILES_CONFIG).split(";"))
                .filter(f -> !f.isEmpty())
                .collect(Collectors.toList());
        System.out.println("Files to process in the task " + String.join(";", this.files));
        this.topic = props.get(CsvFileSourceConnector.TOPIC_CONFIG);
        this.fileReaderMap = this.files.stream()
                .collect(Collectors.toMap(
                        f -> {
                            String[] fTokens = f.split("/");
                            return fTokens[fTokens.length-1];
                        },
                        f -> {
                            try {
                                return new BufferedReader(new FileReader(f));
                            } catch (FileNotFoundException e) {
                                throw new RuntimeException(e);
                            }
                        }));
    }

    @Override
    public List<SourceRecord> poll() throws InterruptedException {
        try {
            List<SourceRecord> sourceRecords = new ArrayList<>();
            for (Map.Entry<String, BufferedReader> keyReader : fileReaderMap.entrySet()) {
                try {
                    String file = keyReader.getKey();
                    Map<String, String> srcPartition = Collections.singletonMap(FILENAME_FIELD, file);
                    Long recordedOffset = fileRecordedOffsetMap.get(file);
                    BufferedReader reader = keyReader.getValue();
                    String line = reader.readLine();
                    long i = 1;
                    while (line != null) {
                        if (i != 1 && i > recordedOffset) {
                            String[] lineTokens = line.split(";");
                            String key = lineTokens[0];
                            Map<String, Long> srcOffset = Collections.singletonMap(POSITION_FIELD, i);
                            SourceRecord sourceRecord = new SourceRecord(srcPartition,
                                    srcOffset, topic, Schema.STRING_SCHEMA, key, Schema.STRING_SCHEMA, line);
                            sourceRecords.add(sourceRecord);
                        }
                        i++;
                        line = reader.readLine();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return sourceRecords;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public synchronized void stop() {
        fileReaderMap.forEach((f, r) -> {
            try {
                r.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
