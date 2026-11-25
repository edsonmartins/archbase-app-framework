package br.com.archbase.shared.kernel.types;

import br.com.archbase.ddd.domain.annotations.DomainValueObject;
import br.com.archbase.ddd.domain.contracts.ValueObject;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.StringUtils;


/**
 * CNPJ, numérico ou alfanumérico (emitido pela Receita Federal desde julho de 2026).
 * <p>
 * {@link #instance(String)} e {@link #setInternalCnpj(String)} tiram a máscara, passam as letras para maiúsculas
 * e exigem 14 caracteres, mas não conferem os dígitos verificadores; use {@link #of(String)} para recusar CNPJ
 * inválido, ou {@link #isValido()} para conferir.
 */
@DomainValueObject
public class CNPJ  implements ValueObject {

    public static final String MASCARA = "##.###.###/####-##";

    @Size(max = 20)
    private String internalCnpj;

    protected CNPJ() {
    }

    private CNPJ(String cnpj) {
        cnpj = removerMascara(cnpj);
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

    /**
     * CNPJ com os dígitos verificadores conferidos.
     *
     * @throws IllegalArgumentException se o CNPJ for inválido
     */
    public static CNPJ of(String cnpj) {
        CNPJ resultado = new CNPJ(cnpj);
        if (!resultado.isValido()) {
            throw new IllegalArgumentException("CNPJ inválido");
        }
        return resultado;
    }

    /** Confere os dígitos verificadores (CNPJ numérico ou alfanumérico). */
    public boolean isValido() {
        return DigitosVerificadores.cnpjValido(internalCnpj);
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

    /**
     * Só o tamanho, como sempre foi: conferir os dígitos aqui passaria a recusar valores que hoje são aceitos.
     * Os dígitos são conferidos por {@link #of(String)} e {@link #isValido()}.
     */
    private boolean validarCNPJ(String cnpj) {
        return cnpj != null && cnpj.length() == 14;
    }

    private String aplicarMascaraCnpj() {
        try {
            if (StringUtils.isBlank(internalCnpj)) {
                return null;
            }
            // MaskFormatter com '#' só aceita dígito; o CNPJ alfanumérico é formatado à mão.
            if (internalCnpj.length() != 14) {
                return null;
            }
            return internalCnpj.substring(0, 2) + "." + internalCnpj.substring(2, 5) + "."
                    + internalCnpj.substring(5, 8) + "/" + internalCnpj.substring(8, 12) + "-"
                    + internalCnpj.substring(12, 14);
        } catch (NullPointerException ex) {
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
        return cnpjSemPontos.toUpperCase(java.util.Locale.ROOT);
    }
}

