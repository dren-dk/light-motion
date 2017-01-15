package dk.dren.lightmotion.db;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dren.lightmotion.api.Phone;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * This is the worst kind of database, it doesn't store anything
 */
public class PhoneDB {

    private final List<Phone> phones;

    public PhoneDB() throws IOException {
        // This is slightly silly, here we read in the json from the classpath only to have Dropwizard turn it
        // right back into json for AngularJS, but this demonstrates how easy it is to read json using Jackson
        // and it allows this example project to work without a real database,
        // btw if you need a database, look no further than dropwizard-jdbi and PostgreSQL
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = PhoneDB.class.getResourceAsStream("/static/angular/phones/phones.json")) {
            phones = mapper.readValue(is, new TypeReference<List<Phone>>(){});
        }
    }

    public List<Phone> findAllPhones() {
        return phones;
    }
}
