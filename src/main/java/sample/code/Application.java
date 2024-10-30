package sample.code;

import java.time.Instant;
import sample.code.dtos.DomainObj;
import sample.code.services.Service;

public class Application {
    private Service service;
    private GetDate getDate;

    public void apiCall(String param) {
        var domainObj = new DomainObj();
        domainObj.setName("name");
        service.serviceMethod(domainObj);
    }

    public void apiCallTwo(String param) {
        var domainObj = new DomainObj();
        String name = "name";
        name = sanitize(name);
        domainObj.setName(name);
        service.serviceMethodTwo(domainObj);
    }

    private String sanitize(String name) {
        return name;
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
