package no.difi.vefa.peppol.lookup.locator;

import no.difi.vefa.peppol.common.model.ParticipantIdentifier;
import no.difi.vefa.peppol.lookup.api.LookupException;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.xbill.DNS.*;

import java.net.URI;
import java.security.MessageDigest;
import java.security.Security;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BdxlLocator extends AbstractLocator {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
            Security.addProvider(new BouncyCastleProvider());
    }

    private String hostname;

    public BdxlLocator(String hostname) {
        this.hostname = hostname;
    }

    @Override
    public URI lookup(ParticipantIdentifier participantIdentifier) throws LookupException {

        String receiverHash;

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-224", BouncyCastleProvider.PROVIDER_NAME);
            byte[] digest = md.digest(participantIdentifier.getIdentifier().getBytes());
            receiverHash = Hex.encodeHexString(digest);
        } catch (Exception e) {
            throw new LookupException(e.getMessage(), e);
        }

        String hostname = String.format("b-%s.%s.%s", receiverHash, participantIdentifier.getScheme().getValue(), this.hostname);

        try {
            Record[] records = new Lookup(hostname, Type.NAPTR).run();
            if (records == null)
                return null;

            for (Record record : records) {
                NAPTRRecord naptrRecord = (NAPTRRecord) record;

                if ("Meta:SMP".equals(naptrRecord.getService()) && "U".equalsIgnoreCase(naptrRecord.getFlags())) {
                    String result = handleRegex(naptrRecord.getRegexp(), hostname);
                    if (result != null)
                        return URI.create(result);
                }
            }
        } catch (TextParseException e) {
            throw new LookupException("Error when handling DNS lookup for BDXL.", e);
        }

        return null;
    }

    public static String handleRegex(String naptrRegex, String hostname) {
        String[] regexp = naptrRegex.split("!");

        // DIGIT bug
        if (regexp[1].equals("^.$"))
            regexp[1] = "^.*$";

        // Simple stupid
        if ("^.*$".equals(regexp[1]))
            return regexp[2];

        // Using regex
        Pattern pattern = Pattern.compile(regexp[1]);
        Matcher matcher = pattern.matcher(hostname);
        if (matcher.matches())
            return matcher.replaceAll(regexp[2].replaceAll("\\\\{2}", "\\$"));

        // No match
        return null;
    }
}