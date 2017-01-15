package dk.dren.lightmotion.onvif;

import lombok.Getter;
import lombok.extern.java.Log;
import org.apache.commons.io.IOUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.cdmckay.coffeedom.Document;
import org.cdmckay.coffeedom.Element;
import org.cdmckay.coffeedom.input.SAXBuilder;

import javax.xml.soap.SOAPException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class contains the ONVIF information that we care about having for a camera and the gnarly interaction with
 * the onvif API needed to extrac the information from the camera
 */
@Log
@Getter
public class CameraONVIFLight {
    public static final SAXBuilder BUILDER = new SAXBuilder();

    private final List<Element> profiles;
    private final String snapshotUri;
    private final String streamUri;
    private final String cameraAddressAndPort;
    private final String user;
    private final String password;
    private final int profileNumber;
    private final String profileToken;

    public CameraONVIFLight(String cameraAddressAndPort, String user, String password, int profileNumber) throws SOAPException, IOException {
        this.cameraAddressAndPort = cameraAddressAndPort;
        this.user = user;
        this.password = password;
        this.profileNumber = profileNumber;

//        String uri = "http://localhost:8084/onvif/device_service";

        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(user, password);
        CredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(AuthScope.ANY, credentials);

        try (CloseableHttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build()) {
            profiles = callGetProfiles(client);
            if (profileNumber < 0 || profileNumber >= profiles.size()) {
                int i = 0;
                for (Element p : profiles) {
                    log.info("Profile #" + i++ + ": " + p.getAttributeValue("token"));
                }
                throw new IllegalArgumentException("The profile number parameter for " + cameraAddressAndPort + " is out of range: 0 <= " + profileNumber + " < " + profiles.size());
            }

            profileToken = profiles.get(profileNumber).getAttributeValue("token");

            snapshotUri = callGetSnapshotUri(client, profileToken);
            streamUri = callGetStreamUri(client, profileToken);
        }
    }

    private String callGetStreamUri(CloseableHttpClient client, String profileToken) throws IOException {
        List<Element> uris = call(client, "GetStreamUri", profileToken);
        if (uris.isEmpty()) {
            return null;
        }

        return xmlText(uris.get(0), "Uri");
    }

    private String callGetSnapshotUri(CloseableHttpClient client, String profileToken) throws IOException {
        List<Element> uris = call(client, "GetSnapshotUri", profileToken);
        if (uris.isEmpty()) {
            return null;
        }

        return xmlText(uris.get(0), "Uri");
    }

    private List<Element> callGetProfiles(CloseableHttpClient client) throws IOException {
        return call(client, "GetProfiles");
    }

    List<Element> call(CloseableHttpClient client, String method, String... args) throws IOException {
        String uri = "http://"+cameraAddressAndPort+"/onvif/device_service";

        HttpPost postProfiles = new HttpPost(uri);
        postProfiles.setHeader("Content-Type", "text/xml;charset=UTF-8");
        postProfiles.setHeader("SOAPAction", "urn:"+method);
        InputStream requestXmlStream = CameraONVIFLight.class.getResourceAsStream("/onvif/" + method + ".xml");
        if (requestXmlStream == null) {
            throw new IllegalArgumentException("No request found for "+method);
        }
        String requestXmlTemplate = IOUtils.toString(requestXmlStream, "UTF-8");
        String requestXml = interpolate(requestXmlTemplate, args);


        postProfiles.setEntity(new StringEntity(requestXml));

        try (CloseableHttpResponse reply = client.execute(postProfiles)) {
            if (reply.getStatusLine().getStatusCode() != 200) {
                throw new IOException("Failed to call "+method+" on "+uri+" with http error code " + reply.getStatusLine().getStatusCode());
            }

            Document doc = BUILDER.build(reply.getEntity().getContent());
            List<Element> result = xmlChildren(doc.getRootElement(), "Body", method + "Response");
            if (result == null) {
                throw new IOException("Failed to understand the response to "+method+" call: "+doc.toString());
            }
            return result;
        }
    }

    private static final Pattern TEMPLATE_TAGS = Pattern.compile("\\$\\{(\\d+)\\}");

    /**
     * Simple templating engine that replaces ${n} tags with args[n]
     *
     * @param template The template
     * @param args The arguments
     * @return The resulting string, with interpolated strings
     */
    private static String interpolate(String template, String... args) {
        StringBuffer sb = new StringBuffer();
        Matcher replacer = TEMPLATE_TAGS.matcher(template);
        while (replacer.find()) {
            int an = Integer.parseInt(replacer.group(1));
            replacer.appendReplacement(sb, args[an]);
        }
        replacer.appendTail(sb);

        return sb.toString();
    }

    private static Element xmlChild(Element element, String... tagNames) {
        String path = "";
        tag: for (String tag : tagNames) {
            if (!path.isEmpty()) {
                path += "/";
            }
            path += element.getName();

            for (Element ch : element.getChildren()) {
                if (tag.equals("*") || ch.getName().equals(tag) || ch.getName().endsWith(":"+tag)) {
                    element = ch;
                    continue tag;
                }
            }

            log.warning("Failed to find xml child "+path);
            return null;
        }
        return element;
    }

    private static List<Element> xmlChildren(Element element, String... tagNames) {
        return xmlChild(element, tagNames).getChildren();
    }

    private static String xmlText(Element element, String... tagNames) {
        return xmlChild(element, tagNames).getText();
    }

}
