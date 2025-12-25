package com.jag.aires.configuration;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class YearMonthCodec implements Codec<YearMonth> {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MM/yyyy");

    @Override
    public YearMonth decode(BsonReader reader, DecoderContext decoderContext) {
        String dateString = reader.readString();
        return YearMonth.parse(dateString, FORMATTER);
    }

    @Override
    public void encode(BsonWriter writer, YearMonth value, EncoderContext encoderContext) {
        writer.writeString(value.format(FORMATTER));
    }

    @Override
    public Class<YearMonth> getEncoderClass() {
        return YearMonth.class;
    }
}