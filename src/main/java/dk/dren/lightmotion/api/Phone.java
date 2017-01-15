package dk.dren.lightmotion.api;

import lombok.Data;

/**
 * The API classes need to be beans (have a default constructor and getters and setters for all fields,
 * Fortunately Lombok saves us from having to write and maintain that mess we we use @Data
 */
@Data
public class Phone {
    private String id;
    private String name;
    private String Snippet;
    private Integer age;
    private String imageUrl;
    private String carrier;
}
