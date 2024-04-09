package br.com.archbase.validation.validators;

import br.com.archbase.validation.constraints.Domain;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p> Rotinas de validação de <b>nome de domínio</b>. </p>
 *
 * <p>
 * Este validador fornece métodos para validar nomes de domínio da Internet
 * e domínios de nível superior.
 * </p>
 *
 * <p> Os nomes de domínio são avaliados de acordo
 * para os padrões <a href="http://www.ietf.org/rfc/rfc1034.txt"> RFC1034 </a>,
 * seção 3 e <a href="http://www.ietf.org/rfc/rfc1123.txt"> RFC1123 </a>,
 * seção 2.1. Nenhuma acomodação é fornecida para as necessidades especializadas de
 * outros aplicativos; se o nome de domínio foi codificado por URL, por exemplo,
 * a validação falhará mesmo que a versão equivalente em texto simples do
 * mesmo nome teria passado.
 * </p>
 *
 * <p>
 * A validação também é fornecida para domínios de nível superior (TLDs), conforme definido e
 * mantido pela Internet Assigned Numbers Authority (IANA):
 * </p>
 *
 * <ul>
 * <li> {@link #isValidInfrastructureTld} -valida TLDs de infraestrutura
 * (<code> .arpa </code>, etc.) </li>
 * <li> {@link #isValidGenericTld} -valida TLDs genéricos
 * (<code> .com, .org </code>, etc.) </li>
 * <li> {@link #isValidCountryCodeTld} -valida TLDs de código de país
 * (<code> .us, .uk, .cn </code>, etc.) </li>
 * </ul>
 *
 * <p>
 * (<b>NOTA</b>: Esta classe não fornece pesquisa de endereço IP para nomes de domínio ou
 * métodos para garantir que um determinado nome de domínio corresponda a um IP específico; consulte
 * {@link java.net.InetAddress} para essa funcionalidade.)
 * </p>
 */
public class DomainValidator implements ConstraintValidator<Domain, CharSequence> {

    // Strings de expressão regular para nomes de host (derivado de RFC2396 e RFC 1123)
    private static final Pattern DOMAIN_LABEL = Pattern.compile("\\p{Alnum}(?>[\\p{Alnum}-]*\\p{Alnum})*");
    private static final Pattern DOMAIN_NAME_REGEX =
            Pattern.compile("^(?:" + DOMAIN_LABEL.pattern() + "\\.)+(\\p{Alpha}{2,})$");
    private static final Set<String> INFRASTRUCTURE_TLDS = new HashSet<>(Arrays.asList("arpa", // internet infrastructure
            "root" // marcador de diagnóstico para zona raiz não truncada
    ));
    private static final Set<String> GENERIC_TLDS = new HashSet<>(Arrays.asList("aero", // indústria de transporte aéreo
            "asia", // Pan-Asia/Asia Pacific
            "biz", // businesses
            "cat", // Catalan linguistic/cultural community
            "com", // commercial enterprises
            "coop", // cooperative associations
            "info", // informational sites
            "jobs", // Human Resource managers
            "mobi", // mobile products and services
            "museum", // museums, surprisingly enough
            "name", // individuals' sites
            "net", // internet support infrastructure/business
            "org", // noncommercial organizations
            "pro", // credentialed professionals and entities
            "tel", // contact data for businesses and individuals
            "travel", // entities in the travel industry
            "gov", // United States Government
            "edu", // accredited postsecondary US education entities
            "mil", // United States Military
            "int" // organizations established by international treaty
    ));
    private static final Set<String> COUNTRY_CODE_TLDS = new HashSet<>(Arrays.asList("ac", // Ilha de Ascensão
            "ad", // Andorra
            "ae", // United Arab Emirates
            "af", // Afghanistan
            "ag", // Antigua and Barbuda
            "ai", // Anguilla
            "al", // Albania
            "am", // Armenia
            "an", // Netherlands Antilles
            "ao", // Angola
            "aq", // Antarctica
            "ar", // Argentina
            "as", // American Samoa
            "at", // Austria
            "au", // Australia (includes Ashmore and Cartier Islands and Coral Sea Islands)
            "aw", // Aruba
            "ax", // Åland
            "az", // Azerbaijan
            "ba", // Bosnia and Herzegovina
            "bb", // Barbados
            "bd", // Bangladesh
            "be", // Belgium
            "bf", // Burkina Faso
            "bg", // Bulgaria
            "bh", // Bahrain
            "bi", // Burundi
            "bj", // Benin
            "bm", // Bermuda
            "bn", // Brunei Darussalam
            "bo", // Bolivia
            "br", // Brazil
            "bs", // Bahamas
            "bt", // Bhutan
            "bv", // Bouvet Island
            "bw", // Botswana
            "by", // Belarus
            "bz", // Belize
            "ca", // Canada
            "cc", // Cocos (Keeling) Islands
            "cd", // Democratic Republic of the Congo (formerly Zaire)
            "cf", // Central African Republic
            "cg", // Republic of the Congo
            "ch", // Switzerland
            "ci", // Côte d'Ivoire
            "ck", // Cook Islands
            "cl", // Chile
            "cm", // Cameroon
            "cn", // China, mainland
            "co", // Colombia
            "cr", // Costa Rica
            "cu", // Cuba
            "cv", // Cape Verde
            "cx", // Christmas Island
            "cy", // Cyprus
            "cz", // Czech Republic
            "de", // Germany
            "dj", // Djibouti
            "dk", // Denmark
            "dm", // Dominica
            "do", // Dominican Republic
            "dz", // Algeria
            "ec", // Ecuador
            "ee", // Estonia
            "eg", // Egypt
            "er", // Eritrea
            "es", // Spain
            "et", // Ethiopia
            "eu", // European Union
            "fi", // Finland
            "fj", // Fiji
            "fk", // Falkland Islands
            "fm", // Federated States of Micronesia
            "fo", // Faroe Islands
            "fr", // France
            "ga", // Gabon
            "gb", // Great Britain (United Kingdom)
            "gd", // Grenada
            "ge", // Georgia
            "gf", // French Guiana
            "gg", // Guernsey
            "gh", // Ghana
            "gi", // Gibraltar
            "gl", // Greenland
            "gm", // The Gambia
            "gn", // Guinea
            "gp", // Guadeloupe
            "gq", // Equatorial Guinea
            "gr", // Greece
            "gs", // South Georgia and the South Sandwich Islands
            "gt", // Guatemala
            "gu", // Guam
            "gw", // Guinea-Bissau
            "gy", // Guyana
            "hk", // Hong Kong
            "hm", // Heard Island and McDonald Islands
            "hn", // Honduras
            "hr", // Croatia (Hrvatska)
            "ht", // Haiti
            "hu", // Hungary
            "id", // Indonesia
            "ie", // Ireland (Éire)
            "il", // Israel
            "im", // Isle of Man
            "in", // India
            "io", // British Indian Ocean Territory
            "iq", // Iraq
            "ir", // Iran
            "is", // Iceland
            "it", // Italy
            "je", // Jersey
            "jm", // Jamaica
            "jo", // Jordan
            "jp", // Japan
            "ke", // Kenya
            "kg", // Kyrgyzstan
            "kh", // Cambodia (Khmer)
            "ki", // Kiribati
            "km", // Comoros
            "kn", // Saint Kitts and Nevis
            "kp", // North Korea
            "kr", // South Korea
            "kw", // Kuwait
            "ky", // Cayman Islands
            "kz", // Kazakhstan
            "la", // Laos (currently being marketed as the official domain for Los Angeles)
            "lb", // Lebanon
            "lc", // Saint Lucia
            "li", // Liechtenstein
            "lk", // Sri Lanka
            "lr", // Liberia
            "ls", // Lesotho
            "lt", // Lithuania
            "lu", // Luxembourg
            "lv", // Latvia
            "ly", // Libya
            "ma", // Morocco
            "mc", // Monaco
            "md", // Moldova
            "me", // Montenegro
            "mg", // Madagascar
            "mh", // Marshall Islands
            "mk", // Republic of Macedonia
            "ml", // Mali
            "mm", // Myanmar
            "mn", // Mongolia
            "mo", // Macau
            "mp", // Northern Mariana Islands
            "mq", // Martinique
            "mr", // Mauritania
            "ms", // Montserrat
            "mt", // Malta
            "mu", // Mauritius
            "mv", // Maldives
            "mw", // Malawi
            "mx", // Mexico
            "my", // Malaysia
            "mz", // Mozambique
            "na", // Namibia
            "nc", // New Caledonia
            "ne", // Niger
            "nf", // Norfolk Island
            "ng", // Nigeria
            "ni", // Nicaragua
            "nl", // Netherlands
            "no", // Norway
            "np", // Nepal
            "nr", // Nauru
            "nu", // Niue
            "nz", // New Zealand
            "om", // Oman
            "pa", // Panama
            "pe", // Peru
            "pf", // French Polynesia With Clipperton Island
            "pg", // Papua New Guinea
            "ph", // Philippines
            "pk", // Pakistan
            "pl", // Poland
            "pm", // Saint-Pierre and Miquelon
            "pn", // Pitcairn Islands
            "pr", // Puerto Rico
            "ps", // Palestinian territories (PA-controlled West Bank and Gaza Strip)
            "pt", // Portugal
            "pw", // Palau
            "py", // Paraguay
            "qa", // Qatar
            "re", // Réunion
            "ro", // Romania
            "rs", // Serbia
            "ru", // Russia
            "rw", // Rwanda
            "sa", // Saudi Arabia
            "sb", // Solomon Islands
            "sc", // Seychelles
            "sd", // Sudan
            "se", // Sweden
            "sg", // Singapore
            "sh", // Saint Helena
            "si", // Slovenia
            "sj", // Svalbard and Jan Mayen Islands Not in use (Norwegian dependencies; see .no)
            "sk", // Slovakia
            "sl", // Sierra Leone
            "sm", // San Marino
            "sn", // Senegal
            "so", // Somalia
            "sr", // Suriname
            "st", // São Tomé and Príncipe
            "su", // Soviet Union (deprecated)
            "sv", // El Salvador
            "sy", // Syria
            "sz", // Swaziland
            "tc", // Turks and Caicos Islands
            "td", // Chad
            "tf", // French Southern and Antarctic Lands
            "tg", // Togo
            "th", // Thailand
            "tj", // Tajikistan
            "tk", // Tokelau
            "tl", // East Timor (deprecated old code)
            "tm", // Turkmenistan
            "tn", // Tunisia
            "to", // Tonga
            "tp", // East Timor
            "tr", // Turkey
            "tt", // Trinidad and Tobago
            "tv", // Tuvalu
            "tw", // Taiwan, Republic of China
            "tz", // Tanzania
            "ua", // Ukraine
            "ug", // Uganda
            "uk", // United Kingdom
            "um", // United States Minor Outlying Islands
            "us", // United States of America
            "uy", // Uruguay
            "uz", // Uzbekistan
            "va", // Vatican City State
            "vc", // Saint Vincent and the Grenadines
            "ve", // Venezuela
            "vg", // British Virgin Islands
            "vi", // U.S. Virgin Islands
            "vn", // Vietnam
            "vu", // Vanuatu
            "wf", // Wallis and Futuna
            "ws", // Samoa (formerly Western Samoa)
            "ye", // Yemen
            "yt", // Mayotte
            "yu", // Serbia and Montenegro (originally Yugoslavia)
            "za", // South Africa
            "zm", // Zambia
            "zw" // Zimbabwe
    ));
    private static final Set<String> LOCAL_TLDS = new HashSet<>(Arrays.asList("localhost", // RFC2606 definido
            "localdomain" // Também amplamente usado como localhost.localdomain
    ));
    private boolean allowLocal;

    /**
     * Retorna verdadeiro se a <code> String </code> especificada corresponder a qualquer
     * Domínio de nível superior da infraestrutura definida pela IANA. Os pontos principais são
     * ignorado se presente. A pesquisa diferencia maiúsculas de minúsculas.
     *
     * @param iTld o parâmetro para verificar o status do TLD de infraestrutura
     * @return true se o parâmetro for um TLD de infraestrutura
     */
    static boolean isValidInfrastructureTld(String iTld) {
        return INFRASTRUCTURE_TLDS.contains(iTld);
    }

    /**
     * Retorna verdadeiro se a <code> String </code> especificada corresponder a qualquer
     * Domínio genérico de primeiro nível definido pela IANA. Os pontos iniciais são ignorados
     * se presente. A pesquisa diferencia maiúsculas de minúsculas.
     *
     * @param gTld o parâmetro para verificar o status do TLD genérico
     * @return true se o parâmetro for um TLD genérico
     */
    static boolean isValidGenericTld(String gTld) {
        return GENERIC_TLDS.contains(gTld);
    }

    /**
     * Retorna verdadeiro se a <code> String </code> especificada corresponder a qualquer
     * Domínio de nível superior de código de país definido pela IANA. Os pontos iniciais são
     * ignorado se presente. A pesquisa diferencia maiúsculas de minúsculas.
     *
     * @param ccTld o parâmetro para verificar o status do TLD do código do país
     * @return true se o parâmetro for um TLD com código de país
     */
    static boolean isValidCountryCodeTld(String ccTld) {
        return COUNTRY_CODE_TLDS.contains(ccTld);
    }

    // ---------------------------------------------
    // ----- TLDs definido por IANA
    // ----- Lista confiável e abrangente em:
    // ----- http://data.iana.org/TLD/tlds-alpha-by-domain.txt

    /**
     * Retorna verdadeiro se a <code> String </code> especificada corresponder a qualquer
     * domínios "locais" amplamente usados ​​(localhost ou localdomain). Os pontos iniciais são
     * ignorado se presente. A pesquisa diferencia maiúsculas de minúsculas.
     *
     * @param iTld o parâmetro para verificar o status do TLD local
     * @return true se o parâmetro for um TLD local
     */
    static boolean isValidLocalTld(String iTld) {
        return LOCAL_TLDS.contains(iTld);
    }

    private static String chompLeadingDot(String str) {
        if (str.charAt(0) == '.') {
            return str.substring(1);
        }
        return str;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid(CharSequence domain, ConstraintValidatorContext context) {
        Matcher matcher = DOMAIN_NAME_REGEX.matcher(domain);
        if (matcher.matches()) {
            domain = matcher.group(1);
            return isValidTld(domain.toString());
        }
        return allowLocal && DOMAIN_LABEL.matcher(domain).matches();
    }

    /**
     * Retorna verdadeiro se a <code> String </code> especificada corresponder a qualquer
     * Domínio de nível superior definido pela IANA. Os pontos iniciais são ignorados, se presentes.
     * A pesquisa diferencia maiúsculas de minúsculas.
     *
     * @param tld o parâmetro para verificar o status do TLD
     * @return true se o parâmetro for um TLD
     */
    boolean isValidTld(String tld) {
        if (allowLocal && isValidLocalTld(tld)) {
            return true;
        }
        tld = chompLeadingDot(tld).toLowerCase();
        return isValidInfrastructureTld(tld) || isValidGenericTld(tld) || isValidCountryCodeTld(tld);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(Domain domain) {
        allowLocal = domain.allowLocal();
    }

}