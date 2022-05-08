package eu.arima.spring.batch.demo;

import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;

import java.sql.*;
import java.util.List;


public class TrinoWeatherObservationWriter implements ItemWriter<WeatherObservation> {
    private String trinoUrl;

    public TrinoWeatherObservationWriter(String trinoUrl) throws SQLException {
        this.trinoUrl = trinoUrl;
        Connection connection = DriverManager.getConnection(trinoUrl);
        Statement statement = connection.createStatement();
        // the bucket 'weather' needs to exists in Minio
        statement.execute("create schema if not exists weather with (location='s3a://weather/')");
        statement.execute("create table if not exists weather.observations(" +
                        "station_id varchar(11), " +
                        "date date, " +
                        "element varchar(4), " +
                        "value integer, " +
                        "measurement_flag varchar(1), " +
                        "quality_flag varchar(1), " +
                        "source_flag varchar(1), " +
                        "obs_time varchar(4))");
        statement.close();
        connection.close();
    }

    @Override
    public void write(List<? extends WeatherObservation> observations) throws Exception {
        Connection connection = DriverManager.getConnection(trinoUrl);
        String compiledQuery = "insert into weather.observations values(?,?,?,?,?,?,?,?)";
        PreparedStatement preparedStatement = connection.prepareStatement(compiledQuery);

        for(WeatherObservation o: observations) {
            preparedStatement.setString(1, o.getId());
            preparedStatement.setDate(2, new Date(o.getDate().toInstant().toEpochMilli()));
            preparedStatement.setString(3, o.getElement());
            preparedStatement.setInt(4, o.getValue());
            preparedStatement.setString(5, o.getMeasurementFlag());
            preparedStatement.setString(6, o.getQualityFlag());
            preparedStatement.setString(7, o.getSourceFlag());
            preparedStatement.setString(8, o.getObservationTime());
            preparedStatement.addBatch();
        }

        preparedStatement.executeBatch();
        preparedStatement.close();
        connection.close();
        System.out.println("Processed one batch");
    }
}
