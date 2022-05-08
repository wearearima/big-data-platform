package eu.arima.spring.batch.demo;


import java.util.Date;

public class WeatherObservation {
    private String id;
    private Date date;
    private String element;
    private int value;
    private String measurementFlag;
    private String qualityFlag;
    private String sourceFlag;
    // 4 chars with local time "1930", "0700"
    private String observationTime;

    public WeatherObservation() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getElement() {
        return element;
    }

    public void setElement(String element) {
        this.element = element;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getMeasurementFlag() {
        return measurementFlag;
    }

    public void setMeasurementFlag(String measurementFlag) {
        this.measurementFlag = measurementFlag;
    }

    public String getQualityFlag() {
        return qualityFlag;
    }

    public void setQualityFlag(String qualityFlag) {
        this.qualityFlag = qualityFlag;
    }

    public String getSourceFlag() {
        return sourceFlag;
    }

    public void setSourceFlag(String sourceFlag) {
        this.sourceFlag = sourceFlag;
    }

    public String getObservationTime() {
        return observationTime;
    }

    public void setObservationTime(String observationTime) {
        this.observationTime = observationTime;
    }

    @Override
    public String toString() {
        return "WeatherObservation{" +
                "id=" + id +
                ", date=" + date +
                ", element='" + element + '\'' +
                ", value=" + value +
                '}';
    }
}
