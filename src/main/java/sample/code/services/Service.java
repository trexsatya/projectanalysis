package sample.code.services;

import java.util.List;
import sample.code.dtos.DomainObj;

public interface Service {
    String serviceMethod(DomainObj domainObj);
    void serviceMethodTwo(DomainObj domainObj);
}
