package sample.code;

import java.time.Instant;
import java.util.List;

public interface Service {
    List<String> serviceMethod(String argumentOne, Instant argumentTwo);
}
