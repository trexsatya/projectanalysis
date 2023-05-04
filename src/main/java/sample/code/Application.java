package sample.code;

import java.time.Instant;

public class Application {
    private Service service;
    private GetDate getDate;

    public void apiCall(String param) {
        Instant date = getDate(param);
        service.serviceMethod(transform(param), date);
    }

    private String transform(String param) {
        return param;
    }

    private Instant getDate(String param) {
        return getDate.getDate(param);
    }

    public static void main(String[] args) {

    }
}
