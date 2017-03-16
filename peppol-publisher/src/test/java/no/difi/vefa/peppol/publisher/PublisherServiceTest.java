package no.difi.vefa.peppol.publisher;

import no.difi.vefa.peppol.common.model.DocumentTypeIdentifier;
import no.difi.vefa.peppol.common.model.ParticipantIdentifier;
import no.difi.vefa.peppol.common.model.ServiceMetadata;
import no.difi.vefa.peppol.lookup.api.FetcherResponse;
import no.difi.vefa.peppol.lookup.api.MetadataReader;
import no.difi.vefa.peppol.lookup.reader.MultiReader;
import no.difi.vefa.peppol.publisher.api.ServiceGroupProvider;
import no.difi.vefa.peppol.publisher.api.ServiceMetadataProvider;
import no.difi.vefa.peppol.publisher.builder.ServiceGroupBuilder;
import no.difi.vefa.peppol.publisher.builder.ServiceMetadataBuilder;
import no.difi.vefa.peppol.publisher.model.ServiceGroup;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.List;

/**
 * @author erlend
 */
public class PublisherServiceTest {

    private static final DocumentTypeIdentifier DTI_INVOICE = DocumentTypeIdentifier.of(
            "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice" +
                    "##urn:www.cenbii.eu:transaction:biitrns010:ver2.0" +
                    ":extended:urn:www.peppol.eu:bis:peppol4a:ver2.0::2.1");


    private ServiceGroupProvider serviceGroupProvider = Mockito.mock(ServiceGroupProvider.class);

    private ServiceMetadataProvider serviceMetadataProvider = Mockito.mock(ServiceMetadataProvider.class);

    private PublisherService publisherService =
            new PublisherService(serviceGroupProvider, serviceMetadataProvider, null, "bdxr");

    private MetadataReader metadataReader = new MultiReader();

    @BeforeTest
    public void before() {
        Mockito.reset(serviceGroupProvider, serviceMetadataProvider);
    }

    @Test
    public void simpleServiceGroup() throws Exception {
        ServiceGroup serviceGroup = ServiceGroupBuilder.newInstance(ParticipantIdentifier.of("9908:999999999"))
                .add(DTI_INVOICE)
                .build();

        Mockito.when(serviceGroupProvider.get(Mockito.any(ParticipantIdentifier.class)))
                .thenReturn(serviceGroup);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        publisherService.serviceGroup(byteArrayOutputStream, null, URI.create("http://localhost:8080/"),
                ParticipantIdentifier.of("9908:999999999"));

        List<DocumentTypeIdentifier> result = metadataReader.parseDocumentIdentifiers(
                new FetcherResponse(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()), null));

        Assert.assertEquals(result.size(), 1);
        Assert.assertEquals(result.get(0).toString(), DTI_INVOICE.toString());
    }

    @Test
    public void simpleServiceMetadata() throws Exception {
        ServiceMetadata serviceMetadata = ServiceMetadataBuilder.newInstance()
                .participant(ParticipantIdentifier.of("9908:999888777"))
                .documentTypeIdentifier(DTI_INVOICE)
                .build();

        Mockito.when(serviceMetadataProvider.get(
                Mockito.any(ParticipantIdentifier.class), Mockito.any(DocumentTypeIdentifier.class)))
                .thenReturn(serviceMetadata);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        publisherService.metadataProvider(byteArrayOutputStream, null,
                ParticipantIdentifier.of("9908:999888777"), DTI_INVOICE);
    }
}