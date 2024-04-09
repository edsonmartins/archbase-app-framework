package br.com.archbase.shared.kernel.types;

import br.com.archbase.ddd.domain.annotations.DomainValueObject;
import br.com.archbase.ddd.domain.contracts.ValueObject;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.StringUtils;

import javax.swing.text.MaskFormatter;
import java.text.ParseException;

@DomainValueObject
public class CNPJ  implements ValueObject {

    public static final String MASCARA = "##.###.###/####-##";

    @Size(max = 20)
    private String internalCnpj;

    protected CNPJ() {
    }

    private CNPJ(String cnpj) {
        if (validarCNPJ(cnpj)) {
            this.internalCnpj = cnpj;
        } else {
            throw new IllegalArgumentException("CNPJ inválido");
        }
    }

    public static CNPJ instance() {
        return new CNPJ();
    }

    public static CNPJ instance(String cnpj) {
        return new CNPJ(cnpj);
    }

    public String getInternalCnpj() {
        return internalCnpj;
    }

    public void setInternalCnpj(String internalCnpj) {
        internalCnpj = removerMascara(internalCnpj);
        if (validarCNPJ(internalCnpj)) {
            this.internalCnpj = internalCnpj;
        } else {
            throw new IllegalArgumentException("CNPJ inválido");
        }
    }

    public String getCnpjFormatado() {
        return aplicarMascaraCnpj();
    }

    private boolean validarCNPJ(String cnpj) {
        if (cnpj == null || cnpj.length() != 14) {
            return false;
        }
        // Implemente a lógica de validação do CNPJ aqui, incluindo os cálculos dos dígitos verificadores
        // Retorna true se o CNPJ for válido, caso contrário, retorna false
        // Você pode encontrar algoritmos de validação do CNPJ disponíveis na web
        return true; // Substitua por sua lógica de validação real
    }

    private String aplicarMascaraCnpj() {
        try {
            if (StringUtils.isBlank(internalCnpj)) {
                return null;
            }
            MaskFormatter mf = new MaskFormatter(MASCARA);
            mf.setValueContainsLiteralCharacters(false);
            return mf.valueToString(internalCnpj);
        } catch (NullPointerException | ParseException ex) {
            return null;
        }
    }

    private String removerMascara(String cnpj) {
        if (StringUtils.isBlank(cnpj)) {
            return null;
        }
        String cnpjSemPontos = cnpj;
        cnpjSemPontos = cnpjSemPontos.replace(".", "");
        cnpjSemPontos = cnpjSemPontos.replace("/", "");
        cnpjSemPontos = cnpjSemPontos.replace("-", "");
        cnpjSemPontos = cnpjSemPontos.replace(" ", "");
        return cnpjSemPontos;
    }
}

