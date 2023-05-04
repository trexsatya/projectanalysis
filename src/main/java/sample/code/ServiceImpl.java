package sample.code;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class ServiceImpl implements Service {
    Config config;

    @Override
    public List<String> serviceMethod(String argumentOne, Instant argumentTwo) {
        return someList().stream().map(this::transform).collect(Collectors.toList());
    }

    private String transform(DomainObj it) {
        return it.toString();
    }

    public List<DomainObj> someList() {
        DomainObj one = new DomainObjImpl();
        if(config.getParam(one.getClass())) {
            one = new DomainObjImpl();
        }
        return List.of(one);
    }
}
