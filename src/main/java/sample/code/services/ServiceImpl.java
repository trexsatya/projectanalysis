package sample.code.services;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import sample.code.Config;
import sample.code.dtos.DomainObj;
import sample.code.entities.State;
import sample.code.entities.TestEntity;
import sample.code.transformers.DomainToJpaTransformer;

@RequiredArgsConstructor
public class ServiceImpl implements Service {
    Config config;
    ServiceTwo serviceTwo;

    private final DomainToJpaTransformer toJpaTransformer;

    @Override
    public String serviceMethod(DomainObj domainObj) {
        String name = toJpaTransformer.toJpa(domainObj).getName();
        return name;
    }

    public void serviceMethodTwo(DomainObj domainObj) {
        TestEntity jpa = toJpaTransformer.toJpa(domainObj);
        jpa.setState(State.CLOSED);
    }
}
