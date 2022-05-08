package eu.arima.spring.batch.demo;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.MultiResourceItemReader;
import org.springframework.batch.item.file.builder.MultiResourceItemReaderBuilder;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.net.MalformedURLException;
import java.sql.SQLException;

@Configuration
@EnableBatchProcessing
public class SpringBatchConfig {
    private JobBuilderFactory jobBuilderFactory;
    private StepBuilderFactory stepBuilderFactory;
    @Value("${trino.url}")
    private String trinoUrl;
    @Value("${observations.src.path}")
    private Resource[] weatherObservationsSource;

    public SpringBatchConfig(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
    }

    @Bean
    public Job readWeatherDataJob() throws MalformedURLException, SQLException {
        return jobBuilderFactory.get("read-and-write-csv-files")
                .start(step1())
                .build();
    }

    @Bean
    public Step step1() throws MalformedURLException, SQLException {
        return stepBuilderFactory.get("read-and-write-weather-observations")
                .<WeatherObservation, WeatherObservation>chunk(100)
                .reader(multiResourceReader())
                .writer(trinoWeatherWriter())
                .build();
    }

    @Bean
    public MultiResourceItemReader multiResourceReader() throws MalformedURLException {
        return new MultiResourceItemReaderBuilder<WeatherObservation>()
                .name("multiResourceReader")
                .delegate(csvReader())
                .resources(weatherObservationsSource)
                .build();
    }

    @SuppressWarnings({"unchecked"})
    @Bean
    public FlatFileItemReader<WeatherObservation> csvReader() {
        FlatFileItemReader<WeatherObservation> reader = new FlatFileItemReader<WeatherObservation>();
        reader.setName("weather-csv-reader");
        reader.setLineMapper(lineMapper());
        return reader;
    }

    private LineMapper<WeatherObservation> lineMapper() {
        DefaultLineMapper<WeatherObservation> lineMapper = new DefaultLineMapper<WeatherObservation>();
        lineMapper.setLineTokenizer(new DelimitedLineTokenizer());
        lineMapper.setFieldSetMapper(new WeatherObservationFieldSetMapper());
        return lineMapper;
    }

    protected static class WeatherObservationFieldSetMapper implements FieldSetMapper<WeatherObservation> {
        public WeatherObservation mapFieldSet(FieldSet fieldSet) {
            WeatherObservation weatherObs =new WeatherObservation();
            weatherObs.setId(fieldSet.readString(0));
            weatherObs.setDate(fieldSet.readDate(1, "yyyyMMdd"));
            weatherObs.setElement(fieldSet.readString(2));
            weatherObs.setValue(fieldSet.readInt(3));
            weatherObs.setMeasurementFlag(fieldSet.readString(4));
            weatherObs.setQualityFlag(fieldSet.readString(5));
            weatherObs.setSourceFlag(fieldSet.readString(6));
            weatherObs.setObservationTime(fieldSet.readString(7));
            return weatherObs;
        }
    }

    @Bean
    public ConsoleWriter<WeatherObservation> consoleWriter() {
        return new ConsoleWriter<WeatherObservation>();
    }

    @Bean
    public TrinoWeatherObservationWriter trinoWeatherWriter() throws SQLException {
        return new TrinoWeatherObservationWriter(trinoUrl);
    }
}

