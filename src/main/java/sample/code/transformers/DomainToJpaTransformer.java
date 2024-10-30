package sample.code.transformers;

import sample.code.dtos.DomainObj;
import sample.code.entities.State;
import sample.code.entities.TestEntity;

public class DomainToJpaTransformer {
    public TestEntity toJpa(DomainObj domainObj) {
        TestEntity testEntity = new TestEntity();
        testEntity.setName(domainObj.getName());
        testEntity.setState(State.OPEN);
        return testEntity;
    }
}
