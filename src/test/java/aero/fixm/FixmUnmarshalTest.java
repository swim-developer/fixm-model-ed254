package aero.fixm;

import aero.fixm.ed254.ArrivalSequence;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.UnmarshalException;
import jakarta.xml.bind.Unmarshaller;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("FIXM ED-254 JAXB Unmarshal")
class FixmUnmarshalTest {

    private static JAXBContext jaxbContext;
    private static Schema schema;

    @BeforeAll
    static void setup() throws Exception {
        jaxbContext = JAXBContext.newInstance("aero.fixm.ed254:aero.fixm.base:aero.fixm.flight");

        URL schemaUrl = FixmUnmarshalTest.class.getClassLoader().getResource("schemas/ed254.xsd");
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schema = sf.newSchema(Paths.get(schemaUrl.toURI()).toFile());
    }

    private ArrivalSequence unmarshalArrivalSequence(String resourcePath) throws JAXBException {
        Unmarshaller u = jaxbContext.createUnmarshaller();
        u.setSchema(schema);
        try (InputStream is = FixmUnmarshalTest.class.getResourceAsStream(resourcePath)) {
            return (ArrivalSequence) u.unmarshal(is);
        } catch (Exception e) {
            if (e instanceof JAXBException je) throw je;
            throw new JAXBException(e.getMessage(), e);
        }
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("Valid XML — should unmarshal successfully")
    @ValueSource(strings = {
        "SEQ_EGLL_001.xml",
        "SEQ_EDDF_004.xml",
        "SEQ_EHAM_010.xml",
        "SEQ_LEMD_033.xml",
        "SEQ_LPPT_016.xml",
        "SEQ_LFPG_017.xml",
        "SEQ_EGKK_005.xml",
        "SEQ_EHAM_outage_recovery_004.xml",
        "SEQ_ESSA_normal_001.xml",
        "SEQ_LFPG_advisories_003.xml"
    })
    void validXml_shouldUnmarshalSuccessfully(String filename) throws Exception {
        ArrivalSequence result = unmarshalArrivalSequence("/ed254-valid/" + filename);

        assertThat(result).isNotNull();
        assertThat(result.getAerodromeDesignator())
            .as("aerodromeDesignator must be present")
            .isNotNull()
            .isNotEmpty();
        assertThat(result.getCreationTime())
            .as("creationTime must be present")
            .isNotNull();
        assertThat(result.getSequenceEntries())
            .as("sequenceEntries must be present")
            .isNotNull();
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("Invalid XML — should fail with validation error")
    @ValueSource(strings = {
        "INVALID_SEQ_EDDF_015.xml",
        "INVALID_SEQ_EHAM_099.xml",
        "INVALID_SEQ_EIDW_024.xml"
    })
    void invalidXml_shouldFailWithValidationError(String filename) {
        UnmarshalException ex = assertThrows(
            UnmarshalException.class,
            () -> unmarshalArrivalSequence("/ed254-invalid/" + filename)
        );

        Throwable cause = ex.getCause() != null ? ex.getCause() : ex.getLinkedException();
        assertThat(cause)
            .isNotNull()
            .isInstanceOfAny(SAXParseException.class, Exception.class);

        String errorDetail = buildErrorDetail(ex, filename);
        System.out.println(errorDetail);

        assertThat(cause.getMessage())
            .as("validation error message must describe the problem")
            .isNotNull()
            .isNotEmpty();
    }

    private String buildErrorDetail(UnmarshalException ex, String filename) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n  [VALIDATION FAILURE] ").append(filename);
        Throwable linked = ex.getLinkedException() != null ? ex.getLinkedException() : ex.getCause();
        if (linked instanceof SAXParseException spe) {
            sb.append("\n    Line   : ").append(spe.getLineNumber());
            sb.append("\n    Column : ").append(spe.getColumnNumber());
            sb.append("\n    Error  : ").append(spe.getMessage());
        } else if (linked != null) {
            sb.append("\n    Error  : ").append(linked.getMessage());
        } else {
            sb.append("\n    Error  : ").append(ex.getMessage());
        }
        return sb.toString();
    }
}
