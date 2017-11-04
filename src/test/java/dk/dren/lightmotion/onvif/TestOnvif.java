package dk.dren.lightmotion.onvif;

import lombok.extern.java.Log;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.TreeMap;

@Log
public class TestOnvif {

    /*
    Works:

    <env:Header>
    <wsse:Security>
        <wsse:UsernameToken>
            <wsse:Username>admin</wsse:Username>
            <wsse:Password Type="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordDigest">12r4n9X85IdnhP9GfDmc4nwEKIg=</wsse:Password>
            <wsse:Nonce EncodingType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary">LTc4MjEyMzM4Ng==</wsse:Nonce>
            <wsu:Created>2017-01-13T21:03:12Z</wsu:Created>
        </wsse:UsernameToken>
    </wsse:Security>
    </env:Header>


    <soap:Header>
        <wsse:Security>
            <wsse:UsernameToken>
                <wsse:Username>admin</wsse:Username>
                <wsse:Password Type="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordDigest">4EEYTgUfL1XfKAn6xF0EuZL8vuk=</wsse:Password>
                <wsse:Nonce EncodingType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary">MTU5OWM4YTQ3NmQ=</wsse:Nonce>
                <wsu:Created>2017-01-14T10:33:44Z</wsu:Created>
            </wsse:UsernameToken>
        </wsse:Security>
    </soap:Header>

     */

    @Test
    public void testPasswordHash() throws UnsupportedEncodingException {
        String nonceBase64 = "LTc4MjEyMzM4Ng==";
        String nonce = new String(Base64.decodeBase64(nonceBase64), "UTF-8");
        Assert.assertEquals("12r4n9X85IdnhP9GfDmc4nwEKIg=", ONVIFCamera.hashPassword( "test", "2017-01-13T21:03:12Z", nonce));
    }

    /* Failed:
    <soap:Header>
        <wsse:Security xmlns:wsse="http://schemas.xmlsoap.org/ws/2003/06/secext">
            <wsse:UsernameToken wsu:Id="admin" xmlns:wsu="http://schemas.xmlsoap.org/ws/2003/06/utility">
                <wsse:Username>admin</wsse:Username>
                <wsse:Password Type="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordDigest">BvSNMGwzTZOD2F3WA9hHNiWiNkg=</wsse:Password>
                <wsse:Nonce EncodingType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary">MTU5OTlhNDc5ZGY=</wsse:Nonce>
                <wsu:Created>2017-01-13T21:03:29Z</wsu:Created>
            </wsse:UsernameToken>
        </wsse:Security>
    </soap:Header>

     */
    @Test
    public void testPasswordHashBroken() throws UnsupportedEncodingException {
        String nonceBase64 = "MTU5OTlhNDc5ZGY=";
        String nonce = new String(Base64.decodeBase64(nonceBase64), "UTF-8");
        Assert.assertEquals("BvSNMGwzTZOD2F3WA9hHNiWiNkg=", ONVIFCamera.hashPassword("test", "2017-01-13T21:03:29Z", nonce));
    }

    /*




     */

    @Test
    public void testReplay() throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<S:Envelope xmlns:S=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\">\n" +
                "  <env:Header>\n" +
                "    <wsse:Security>\n" +
                "      <wsse:UsernameToken>\n" +
                "        <wsse:Username>admin</wsse:Username>\n" +
                "        <wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordDigest\">meAEL1ArJ1ndKT6386KRK9oyFEY=</wsse:Password>\n" +
                "        <wsse:Nonce EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\">LTMzNzMxODU=</wsse:Nonce>\n" +
                "        <wsu:Created>2017-01-14T14:33:10Z</wsu:Created>\n" +
                "      </wsse:UsernameToken>\n" +
                "    </wsse:Security>\n" +
                "  </env:Header>\n" +
                "  <S:Body>\n" +
                "    <ns5:GetProfiles xmlns:ns10=\"http://www.onvif.org/ver10/device/wsdl\" xmlns:ns11=\"http://www.w3.org/2004/08/xop/include\" xmlns:ns2=\"http://docs.oasis-open.org/wsrf/bf-2\" xmlns:ns3=\"http://www.w3.org/2005/08/addressing\" xmlns:ns4=\"http://docs.oasis-open.org/wsn/b-2\" xmlns:ns5=\"http://www.onvif.org/ver10/media/wsdl\" xmlns:ns6=\"http://www.onvif.org/ver10/schema\" xmlns:ns7=\"http://docs.oasis-open.org/wsn/t-1\" xmlns:ns9=\"http://www.onvif.org/ver10/deviceIO/wsdl\" xmlns:xmime=\"http://www.w3.org/2005/05/xmlmime\"/>\n" +
                "  </S:Body>\n" +
                "</S:Envelope>";

        xml = xml.replaceAll(">\\s+<", "><");
        System.err.println("Length: "+xml.length());

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
           String uri = "http://10.0.2.90:8080/onvif/media";
            HttpPost request = new HttpPost(uri);
            request.setHeader("Accept", "application/soap+xml, multipart/related");
            request.setHeader("Content-Type", "application/soap+xml; charset=utf-8;action=\"http://www.onvif.org/ver10/media/wsdl/GetProfiles\"");

            String auth = "admin:test";
            byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("ISO-8859-1")));
            String authHeader = "Basic " + new String(encodedAuth);
            request.setHeader(HttpHeaders.AUTHORIZATION, authHeader);


            request.setEntity(new StringEntity(xml));

            try (CloseableHttpResponse reply = client.execute(request)) {
                if (reply.getStatusLine().getStatusCode() != 200) {
                    throw new IOException("Failed on " + uri + " with http error code " + reply.getStatusLine().getStatusCode() + " Request:\n" + xml);
                }

            }
        }
    }

    @BeforeClass
    public static void init() {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
    }

    @Test
    public void testRaw() throws IOException, ParserConfigurationException, SAXException, XPathExpressionException, SOAPException {


        for (int i = 0;i<9;i++) {
            /*
            OnvifDevice legacy = new OnvifDevice("10.0.2.9"+i+":8080", "admin", "test");
            legacy.getMedia().getProfiles();
*/
//            CameraONVIF cam = new CameraONVIF("10.0.2.9"+i+":8080", "admin", "admin", 0);
            ONVIFCamera cam = new ONVIFCamera("10.0.2.9"+i+":8080", "admin", "test");
            System.out.println("Camera "+i);
            for (ONVIFProfile profile : cam.getProfiles()) {
                System.out.println("\tstream="+profile.getStreamUrl()+"\tsnap="+profile.getSnapshotUri());
            }
        }


    }


    @Test
    public void testTemplate() {
        Map<String, String> replacements = new TreeMap<>();
        replacements.put("hest", "fest");
        Assert.assertEquals("testfest", ONVIFCamera.interpolate("test${hest}", replacements));
    }
}
