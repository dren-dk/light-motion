package dk.dren.lightmotion.onvif;

import lombok.Getter;
import lombok.extern.java.Log;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static javax.xml.parsers.DocumentBuilderFactory.newInstance;

/**
 * This class contains the ONVIF information that we care about having for a camera and the gnarly interaction with
 * the onvif API needed to extract the information from the camera
 */
@Log
@Getter
public class ONVIFCamera {
    private final DocumentBuilder PARSER;

    private final String snapshotUri;
    private final String streamUri;
    private final String cameraAddressAndPort;
    private final String user;
    private final String password;
    private final int profileNumber;
    private final ONVIFProfile profile;
    private String time = getUTCTimeStamp();

    private String getUTCTimeStamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-d'T'HH:mm:ss'Z'");
        sdf.setTimeZone(new SimpleTimeZone(SimpleTimeZone.UTC_TIME, "UTC"));
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        return sdf.format(cal.getTime());
    }

    public ONVIFCamera(String cameraAddressAndPort, String user, String password, int profileNumber) throws SOAPException, IOException, SAXException {
        try {
            PARSER = newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("Failed to get the DOM parser", e);
        }

        this.cameraAddressAndPort = cameraAddressAndPort;
        this.user = user;
        this.password = password;
        this.profileNumber = profileNumber;

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            List<ONVIFProfile> profiles = callGetProfiles(client);
            if (profileNumber < 0 || profileNumber >= profiles.size()) {
                int i = 0;
                for (ONVIFProfile p : profiles) {
                    log.info("Profile #" + i++ + ": " + p);
                }
                throw new IllegalArgumentException("The profile number parameter for " + cameraAddressAndPort + " is out of range: 0 <= " + profileNumber + " < " + profiles.size());
            }

            profile = profiles.get(profileNumber);

            snapshotUri = callGetSnapshotUri(client, profile.getToken());
            streamUri = callGetStreamUri(client, profile.getToken());
        }
    }

    private String callGetStreamUri(CloseableHttpClient client, String profileToken) throws IOException, SAXException {
        List<Element> uris = call(client, "GetStreamUri", profileToken);
        if (uris.isEmpty()) {
            return null;
        }

        return xmlText(uris.get(0), "Uri");
    }

    private String callGetSnapshotUri(CloseableHttpClient client, String profileToken) throws IOException, SAXException {
        List<Element> uris = call(client, "GetSnapshotUri", profileToken);
        if (uris.isEmpty()) {
            return null;
        }

        return xmlText(uris.get(0), "Uri");
    }

    private List<ONVIFProfile> callGetProfiles(CloseableHttpClient client) throws IOException, SAXException {
        return call(client, "GetProfiles").stream().map(ONVIFProfile::new).collect(Collectors.toList());
    }

    List<Element> call(CloseableHttpClient client, String method, String... args) throws IOException, SAXException {
        String uri = "http://"+cameraAddressAndPort+"/onvif/media";

        HttpPost request = new HttpPost(uri);
        request.setHeader("Content-Type", "Content-Type: application/soap+xml; charset=utf-8;action=\"http://www.onvif.org/ver10/media/wsdl/"+method);
        request.setHeader("SOAPAction", "urn:"+method);
        request.setHeader("Accept", "application/soap+xml, multipart/related");
        String requestXmlTemplate = getTemplate(method);

        Map<String, String> argsWithCredentials = new TreeMap();

        String nonce = Long.toHexString(System.currentTimeMillis());
        String authHeader = createAuthHeader(user, password, time, nonce);
        argsWithCredentials.put("header", authHeader);

        for (int i=0;i<args.length;i++) {
            argsWithCredentials.put(Integer.toString(i), args[i]);
        }

        String requestXml = interpolate(requestXmlTemplate, argsWithCredentials);

        request.setEntity(new StringEntity(requestXml));

        try (CloseableHttpResponse reply = client.execute(request)) {
            if (reply.getStatusLine().getStatusCode() != 200) {
                throw new IOException("Failed to call "+method+" on "+uri+" with http error code " + reply.getStatusLine().getStatusCode()+" Request:\n"+requestXml);
            }

            Document doc = PARSER.parse(reply.getEntity().getContent());

            List<Element> result = xmlChildren(doc.getDocumentElement(), "Body", method + "Response");
            if (result == null) {
                throw new IOException("Failed to understand the response to "+method+" call: "+doc.toString());
            }
            return result;
        }
    }

    private static String getTemplate(String name) throws IOException {
        try (InputStream requestXmlStream = ONVIFCamera.class.getResourceAsStream("/onvif/" + name + ".xml")) {
            if (requestXmlStream == null) {
                throw new IllegalArgumentException("No request found for " + name);
            }
            return IOUtils.toString(requestXmlStream, "UTF-8");
        }
    }

    static String createAuthHeader(String user, String password, String time, String nonce) throws IOException {

        Map<String, String> vars = new TreeMap<>();
        vars.put("user", user);
        vars.put("time", time);
        vars.put("nonce", Base64.encodeBase64String(nonce.getBytes("UTF-8")));
        vars.put("hash", hashPassword(user, password, time, nonce));

        return interpolate(getTemplate("Header"), vars);
    }

    static String hashPassword(String user, String password, String time, String nonce) {
        return Base64.encodeBase64String(DigestUtils.sha1(nonce + time + password));
    }

    private static final Pattern TEMPLATE_TAGS = Pattern.compile("\\$\\{([^\\}]+)\\}");

    /**
     * Simple templating engine that replaces ${n} tags with args[n]
     *
     * @param template The template
     * @param args The arguments
     * @return The resulting string, with interpolated strings
     */
    static String interpolate(String template, Map<String, String> args) {
        StringBuffer sb = new StringBuffer();
        Matcher replacer = TEMPLATE_TAGS.matcher(template);
        while (replacer.find()) {
            String var = replacer.group(1);
            String replacement = args.get(var);
            if (replacement == null) {
                throw new IllegalArgumentException("Could not find replacement for ${"+var+"} in "+template);
            }
            replacer.appendReplacement(sb, replacement);
        }
        replacer.appendTail(sb);

        return sb.toString();
    }

    static Element xmlChild(Element element, String... tagNames) {
        String path = "";
        tag: for (String tag : tagNames) {
            if (!path.isEmpty()) {
                path += "/";
            }
            path += element.getTagName();

            for (Element ch : xmlElements(element.getChildNodes())) {
                if (ch.getTagName().equals(tag) || ch.getTagName().endsWith(":"+tag)) {
                    element = ch;
                    continue tag;
                }
            }

            log.warning("Failed to find xml child "+path);
            return null;
        }
        return element;
    }

    static List<Element> xmlElements(NodeList nodes) {
        List<Element> result = new ArrayList<>();
        for (int i=0;i<nodes.getLength();i++) {
            Node item = nodes.item(i);
            if (item instanceof Element) {
                result.add((Element) item);
            }
        }
        return result;
    }

    static List<Element> xmlChildren(Element element, String... tagNames) {
        return xmlElements(xmlChild(element, tagNames).getChildNodes());
    }

    static String xmlText(Element element, String... tagNames) {
        return xmlChild(element, tagNames).getTextContent();
    }

    static Integer xmlInt(Element element, String... tagNames) {
        Element ch = ONVIFCamera.xmlChild(element, tagNames);
        if (ch != null) {
            return Integer.valueOf(ch.getTextContent());
        } else {
            return null;
        }
    }


}
