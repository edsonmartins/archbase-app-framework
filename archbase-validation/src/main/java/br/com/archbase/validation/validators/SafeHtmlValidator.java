package br.com.archbase.validation.validators;

import br.com.archbase.validation.constraints.SafeHtml;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Iterator;

/**
 * Valide se a string não contém código malicioso.
 * <p>
 * Ele usa <a href="http://www.jsoup.org"> JSoup </a> como a biblioteca analisadora / sanitizadora subjacente.
 */
public class SafeHtmlValidator implements ConstraintValidator<SafeHtml, CharSequence> {
    private Safelist whitelist;

    @Override
    public void initialize(SafeHtml safeHtmlAnnotation) {
        switch (safeHtmlAnnotation.whitelistType()) {
            case BASIC:
                whitelist = Safelist.basic();
                break;
            case BASIC_WITH_IMAGES:
                whitelist = Safelist.basicWithImages();
                break;
            case NONE:
                whitelist = Safelist.none();
                break;
            case RELAXED:
                whitelist = Safelist.relaxed();
                break;
            case SIMPLE_TEXT:
                whitelist = Safelist.simpleText();
                break;
            default:
                throw new IllegalStateException("Valor inesperado: " + safeHtmlAnnotation.whitelistType());
        }
        whitelist.addTags(safeHtmlAnnotation.additionalTags());

        for (SafeHtml.Tag tag : safeHtmlAnnotation.additionalTagsWithAttributes()) {
            whitelist.addAttributes(tag.name(), tag.attributes());
        }
    }


    /**
     * Returns a document whose {@code <body>} element contains the given HTML fragment.
     */
    private Document getFragmentAsDocument(CharSequence value) {
        // usar o analisador XML garante que todos os elementos na entrada sejam retidos, também se eles realmente não forem permitidos no dado
        // localização; Por exemplo, um elemento <td> não é permitido diretamente no elemento <body>, portanto, seria usado pelo analisador HTML padrão.
        // precisamos mantê-lo para aplicar a lista branca fornecida corretamente; Consulte HV-873
        Document fragment = Jsoup.parse(value.toString(), "", Parser.xmlParser());
        Document document = Document.createShell("");

        // adicione os nós do fragmento ao corpo do documento resultante
        Iterator<Element> nodes = fragment.children().iterator();
        while (nodes.hasNext()) {
            document.body().appendChild(nodes.next());
        }

        return document;
    }

    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        return new Cleaner(whitelist).isValid(getFragmentAsDocument(value));
    }
}
