package br.com.archbase.shared.kernel.types;


import br.com.archbase.ddd.domain.annotations.DomainValueObject;
import br.com.archbase.ddd.domain.contracts.ValueObject;
import org.apache.commons.lang3.StringUtils;

import javax.swing.text.MaskFormatter;
import jakarta.validation.constraints.Size;
import java.text.ParseException;

@DomainValueObject
public class CPF  implements ValueObject {

    public static final String MASCARA = "###.###.###-##";


    @Size(max = 20)
    private String internalCpf;

    protected CPF() {
    }

    private CPF(String cpf) {
        this.internalCpf = cpf;
    }

    public static CPF instance() {
        return new CPF();
    }

    public static CPF instance(String cpf) {
        return new CPF(cpf);
    }

    public String getInternalCpf() {
        return internalCpf;
    }

    public void setInternalCpf(String internalCpf) {
        this.internalCpf = removerMascara(internalCpf);
    }

    public String getCpfFormadado() {
        return aplicarMascaraCpf();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((internalCpf == null) ? 0 : internalCpf.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CPF other = (CPF) obj;
        if (internalCpf == null) {
            if (other.internalCpf != null)
                return false;
        } else if (!internalCpf.equals(other.internalCpf))
            return false;
        return true;
    }

    private String aplicarMascaraCpf() {
        try {
            if (StringUtils.isBlank(internalCpf)) {
                return null;
            }
            MaskFormatter mf = null;
            mf = new MaskFormatter(MASCARA);
            mf.setValueContainsLiteralCharacters(false);
            return mf.valueToString(internalCpf);

        } catch (NullPointerException | ParseException ex) {
            return null;
        }
    }

    private String removerMascara(String cpf) {
        if (StringUtils.isBlank(cpf)) {
            return null;
        }
        String cpfSemPontos = cpf;
        cpfSemPontos = cpfSemPontos.replace(".", "");
        cpfSemPontos = cpfSemPontos.replace("-", "");
        cpfSemPontos = cpfSemPontos.replace(" ", "");
        return cpfSemPontos;
    }

}