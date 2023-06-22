package ru.andronov.kafka.connect.source.csv.schema;

import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;

public class UserSchema {

    public static Schema getSchema() {
        return SchemaBuilder.struct().name("user")
                .field("name", Schema.STRING_SCHEMA)
                .field("age", Schema.INT16_SCHEMA)
                .build();
    }

    public static Struct getStruct(String line) {
        String[] tokens = line.split(";");
        return new Struct(getSchema())
                .put("name", tokens[1])
                .put("age", Integer.parseInt(tokens[2]));
    }
}
