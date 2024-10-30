package sample.code.entities;


import lombok.Data;

@Data
public class TestEntity implements JpaEntity {
    private String name;
    private int age;
    private State state;
}
