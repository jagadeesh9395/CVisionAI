package com.jag.aires.configuration;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.BsonReader;
import org.bson.BsonString;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Override
    protected String getDatabaseName() {
        return "cvision_resumes";
    }

    @Bean
    @Override
    public MongoClient mongoClient() {
        CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
                CodecRegistries.fromCodecs(new YearMonthCodec()),
                MongoClientSettings.getDefaultCodecRegistry(),
                CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build())
        );

        return MongoClients.create(MongoClientSettings.builder()
                .applyConnectionString(new com.mongodb.ConnectionString("mongodb://localhost:27017"))
                .codecRegistry(codecRegistry)
                .build());
    }

    @Bean
    @Override
    public MongoCustomConversions customConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new YearMonthToStringConverter());
        converters.add(new StringToYearMonthConverter());
        return new MongoCustomConversions(converters);
    }

    // Custom Codec for YearMonth
    public static class YearMonthCodec implements Codec<YearMonth> {
        private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yyyy");

        @Override
        public YearMonth decode(BsonReader reader, DecoderContext decoderContext) {
            String dateString = reader.readString();
            return YearMonth.parse(dateString, formatter);
        }

        @Override
        public void encode(BsonWriter writer, YearMonth value, EncoderContext encoderContext) {
            writer.writeString(value.format(formatter));
        }

        @Override
        public Class<YearMonth> getEncoderClass() {
            return YearMonth.class;
        }
    }

    // Converter for reading from String to YearMonth
    public static class StringToYearMonthConverter implements Converter<String, YearMonth> {
        @Override
        public YearMonth convert(String source) {
            if (source == null || source.trim().isEmpty()) {
                return null;
            }
            return YearMonth.parse(source, DateTimeFormatter.ofPattern("MM/yyyy"));
        }
    }

    // Converter for writing from YearMonth to String
    public static class YearMonthToStringConverter implements Converter<YearMonth, String> {
        @Override
        public String convert(YearMonth source) {
            return source != null ? source.format(DateTimeFormatter.ofPattern("MM/yyyy")) : null;
        }
    }
}