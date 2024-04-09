package br.com.archbase.validation.validators;

import br.com.archbase.validation.constraints.JsAssert;
import lombok.extern.slf4j.Slf4j;

import javax.script.*;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static javax.script.ScriptContext.ENGINE_SCOPE;

@Slf4j
public class JsAssertValidator implements ConstraintValidator<JsAssert, Object> {

    public static final String EXPRESSAO = "Expressão: ```";


    // Atua como um mecanismo de cache para compilar todas as expressões encontradas na anotação
    protected static Map<String, CompiledScript> cachedExpressions = new HashMap<>();

    protected ScriptEngine engine;
    protected ScriptContext currentContext;
    protected JsAssert annotation;

    @Override
    public void initialize(JsAssert constraintAnnotation) {
        this.engine = new ScriptEngineManager().getEngineByName("nashorn");
        this.currentContext = engine.getContext();
        this.annotation = constraintAnnotation;
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext validatorContext) {

        String attributeName = annotation.attributeName();
        String expression = annotation.value();

        CompiledScript expCompiled = cachedExpressions
                .get(expression);

        if (null == expCompiled) {
            // Expression is not compiled, it's evaluated for the first time
            TimeActionResponse<CompiledScript> ret = TimeAction.recordTimeAndDo(compile(expression));
            log.info("Expressão de script {} compilado em {} ms.", expression, ret.getTime());
            expCompiled = ret.getResponse();
            cachedExpressions.put(expression, expCompiled);
        }

        // O valor é vinculado ao contexto do motor com o nome 'valor'
        currentContext
                .getBindings(ENGINE_SCOPE)
                .put(attributeName, value);

        try {
            // Avaliando a expressão booleana incluída na anotação.
            // Expression está validando o valor.
            Object result = expCompiled.eval(currentContext);

            if (result instanceof Boolean) {
                return (Boolean) result;
            }

            throw new IllegalArgumentException(EXPRESSAO + expression + "``` não é uma expressão booleana.");

        } catch (ScriptException e) {
            throw new IllegalArgumentException(EXPRESSAO + expression + "``` falhou em avaliar.", e);
        }
    }

    public Supplier<CompiledScript> compile(String expression) {
        return () -> {
            try {
                return ((Compilable) engine).compile(expression);
            } catch (ScriptException e) {
                throw new IllegalArgumentException(EXPRESSAO + expression + "``` é inválido. A pré-compilação falhou.", e);
            }
        };
    }
}
