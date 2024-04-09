package br.com.archbase.ddd.infraestructure.persistence.jpa.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.*;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class SimpleFilterPredicateFactory {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public static List<Field> getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                fields.add(field);
            }
        }
        fields.sort(new Comparator<Field>() {
            @Override
            public int compare(Field o1, Field o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return fields;
    }

    public static Optional<Field> getFieldByName(Class<?> sourceClass, String fieldName) {
        List<Field> allFields = getAllFields(sourceClass);
        return allFields.stream().filter((field)->field.getName().equalsIgnoreCase(fieldName)).findFirst();
    }

    public static <T> BooleanBuilder createPredicate(PathBuilder<T> entityPath, Class<T> entityClass, String fieldName, String value) {
        BooleanBuilder builder = new BooleanBuilder();
        if (fieldName.contains(".")) {
            processNestedFields(builder, entityPath, fieldName, value);
        } else {
            Optional<Field> fieldOptional = getFieldByName(entityClass,fieldName);
            if (fieldOptional.isPresent()) {
                Field field = fieldOptional.get();
                Class<?> fieldType = field.getType();

                if (fieldType.equals(String.class)) {
                    processString(builder, entityPath.getString(fieldName), value);
                } else if (Arrays.asList(Integer.class, Long.class, BigInteger.class, BigDecimal.class).contains(fieldType)) {

                    if (fieldType.equals(Integer.class)) {
                        NumberPath<Integer> numberPath = entityPath.getNumber(fieldName, Integer.class);
                        processInteger(builder, numberPath, value, fieldType);
                    } else if (fieldType.equals(Long.class)) {
                        NumberPath<Long> numberPath = entityPath.getNumber(fieldName, Long.class);
                        processLong(builder, numberPath, value, fieldType);
                    } else if (fieldType.equals(BigDecimal.class)) {
                        NumberPath<BigDecimal> numberPath = entityPath.getNumber(fieldName, BigDecimal.class);
                        processBigDecimal(builder, numberPath, value, fieldType);
                    } else if (fieldType.equals(BigInteger.class)) {
                        NumberPath<BigInteger> numberPath = entityPath.getNumber(fieldName, BigInteger.class);
                        processBigInteger(builder, numberPath, value, fieldType);
                    }
                } else if (fieldType.equals(LocalDate.class)) {
                    processLocalDate(builder, entityPath.getDate(fieldName, LocalDate.class), value);
                } else if (fieldType.equals(LocalDateTime.class)) {
                    processLocalDateTime(builder, entityPath.getDateTime(fieldName, LocalDateTime.class), value);
                } else if (fieldType.equals(Date.class)) {
                    processDate(builder, entityPath.getDate(fieldName, Date.class), value);
                }
            }
        }

        return builder;
    }

    private static void processString(BooleanBuilder builder, StringPath campo, String valor) {
        if (valor.contains(",")) {
            Arrays.stream(valor.split(","))
                    .forEach(str -> builder.or(campo.containsIgnoreCase(str)));
        } else {
            builder.and(campo.containsIgnoreCase(valor));
        }
    }

    private static void processInteger(BooleanBuilder builder, NumberPath<Integer> campo, String valor, Class<?> fieldType) {
        try {
            if (valor.contains(":")) {
                String[] valores = valor.split(":");
                Integer inicio = (Integer) convertToNumber(valores[0], fieldType);
                Integer fim = (Integer) convertToNumber(valores[1], fieldType);
                builder.and(campo.between(inicio, fim));
            } else if (valor.contains(",")) {
                Arrays.stream(valor.split(","))
                        .map(v -> (Integer) convertToNumber(v, fieldType))
                        .forEach(num -> builder.or(campo.eq(num)));
            } else {
                Integer num = (Integer) convertToNumber(valor, fieldType);
                builder.and(campo.eq(num));
            }
        } catch (Exception ex) {
            //
        }
    }

    private static void processLong(BooleanBuilder builder, NumberPath<Long> campo, String valor, Class<?> fieldType) {
        try {
            if (valor.contains(":")) {
                String[] valores = valor.split(":");
                Long inicio = (Long) convertToNumber(valores[0], fieldType);
                Long fim = (Long) convertToNumber(valores[1], fieldType);
                builder.and(campo.between(inicio, fim));
            } else if (valor.contains(",")) {
                Arrays.stream(valor.split(","))
                        .map(v -> (Long) convertToNumber(v, fieldType))
                        .forEach(num -> builder.or(campo.eq(num)));
            } else {
                Long num = (Long) convertToNumber(valor, fieldType);
                builder.and(campo.eq(num));
            }
        } catch (Exception ex) {
            //
        }
    }

    private static void processBigInteger(BooleanBuilder builder, NumberPath<BigInteger> campo, String valor, Class<?> fieldType) {
        try {
            if (valor.contains(":")) {
                String[] valores = valor.split(":");
                BigInteger inicio = (BigInteger) convertToNumber(valores[0], fieldType);
                BigInteger fim = (BigInteger) convertToNumber(valores[1], fieldType);
                builder.and(campo.between(inicio, fim));
            } else if (valor.contains(",")) {
                Arrays.stream(valor.split(","))
                        .map(v -> (BigInteger) convertToNumber(v, fieldType))
                        .forEach(num -> builder.or(campo.eq(num)));
            } else {
                BigInteger num = (BigInteger) convertToNumber(valor, fieldType);
                builder.and(campo.eq(num));
            }
        } catch (Exception ex) {
            //
        }
    }

    private static void processBigDecimal(BooleanBuilder builder, NumberPath<BigDecimal> campo, String valor, Class<?> fieldType) {
        try {
            if (valor.contains(":")) {
                String[] valores = valor.split(":");
                BigDecimal inicio = (BigDecimal) convertToNumber(valores[0], fieldType);
                BigDecimal fim = (BigDecimal) convertToNumber(valores[1], fieldType);
                builder.and(campo.between(inicio, fim));
            } else if (valor.contains(",")) {
                Arrays.stream(valor.split(","))
                        .map(v -> (BigDecimal) convertToNumber(v, fieldType))
                        .forEach(num -> builder.or(campo.eq(num)));
            } else {
                BigDecimal num = (BigDecimal) convertToNumber(valor, fieldType);
                builder.and(campo.eq(num));
            }
        } catch (Exception ex) {
            //
        }
    }

    private static Number convertToNumber(String valor, Class<?> tipo) {
        if (tipo.equals(Integer.class) || tipo.equals(int.class)) {
            return Integer.parseInt(valor);
        } else if (tipo.equals(Long.class) || tipo.equals(long.class)) {
            return Long.parseLong(valor);
        } else if (tipo.equals(BigInteger.class)) {
            return new BigInteger(valor);
        } else if (tipo.equals(BigDecimal.class)) {
            return new BigDecimal(valor);
        }
        throw new IllegalArgumentException("Tipo numérico não suportado: " + tipo);
    }

    private static LocalDate converterLocalDate(String value) {
        return LocalDate.parse(value, DATE_FORMAT);
    }

    private static void processLocalDate(BooleanBuilder builder, DatePath<LocalDate> campo, String valor) {
        if (valor.contains(":")) {
            String[] datas = valor.split(":");
            try {
                LocalDate inicio = converterLocalDate(datas[0]);
                LocalDate fim = converterLocalDate(datas[1]);
                builder.and(campo.between(inicio, fim));
            } catch (DateTimeParseException e) {
                //
            }
        } else if (valor.contains(",")) {
            Arrays.stream(valor.split(","))
                    .map(SimpleFilterPredicateFactory::converterLocalDate)
                    .forEach(num -> builder.or(campo.eq(num)));
        } else {
            try {
                LocalDate data = LocalDate.parse(valor, DATE_FORMAT);
                builder.and(campo.eq(data));
            } catch (DateTimeParseException e) {
                //
            }
        }
    }

    private static void processDate(BooleanBuilder builder, DatePath<Date> campo, String valor) {
        if (valor.contains(":")) {
            String[] datas = valor.split(":");
            try {
                LocalDate inicio = LocalDate.parse(datas[0], DATE_FORMAT);
                LocalDate fim = LocalDate.parse(datas[1], DATE_FORMAT);
                builder.and(campo.between(convertToDateViaInstant(inicio), convertToDateViaInstant(fim)));
            } catch (DateTimeParseException e) {
                //
            }
        } else if (valor.contains(",")) {
            Arrays.stream(valor.split(","))
                    .map(v -> convertToDateViaInstant(converterLocalDate(v)))
                    .forEach(num -> builder.or(campo.eq(num)));
        } else {
            try {
                LocalDate data = LocalDate.parse(valor, DATE_FORMAT);
                builder.and(campo.eq(convertToDateViaInstant(data)));
            } catch (DateTimeParseException e) {
                //
            }
        }
    }

    public static Date convertToDateViaInstant(LocalDate dateToConvert) {
        return java.util.Date.from(dateToConvert.atStartOfDay()
                .atZone(ZoneId.systemDefault())
                .toInstant());
    }

    private static void processLocalDateTime(BooleanBuilder builder, DateTimePath<LocalDateTime> field, String valor) {
        try {
            if (valor.contains(":") && seemsToBeInterval(valor)) {
                String[] datas = valor.split(":", 2);
                LocalDateTime inicio = LocalDateTime.parse(datas[0], DATE_TIME_FORMAT);
                LocalDateTime fim = LocalDateTime.parse(datas[1], DATE_TIME_FORMAT);
                builder.and(field.between(inicio, fim));
            } else if (valor.contains(",")) {
                Arrays.stream(valor.split(","))
                        .map(v -> LocalDateTime.parse(v, DATE_TIME_FORMAT))
                        .forEach(num -> builder.or(field.eq(num)));
            } else {
                LocalDateTime dataHora = LocalDateTime.parse(valor, DATE_TIME_FORMAT);
                builder.and(field.eq(dataHora));
            }
        } catch (DateTimeParseException e) {
            //
        }
    }

    private static boolean seemsToBeInterval(String value) {
        // Regex para uma data no formato dd/MM/yyyy HH:mm:ss
        String regexDateTime = "\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2}";

        // Regex para um intervalo: duas datas separadas por ':'
        String regexInterval = regexDateTime + ":" + regexDateTime;

        // Retorna verdadeiro se a string corresponder ao padrão de intervalo
        return value.matches(regexInterval);
    }


    private static <T> void processNestedFields(BooleanBuilder builder, PathBuilder<?> entityPath, String fieldName, String value) {
        String[] parts = fieldName.split("\\.");

        // Começa com o path base
        PathBuilder<?> nestedPath = entityPath;

        // Constrói o caminho completo para o campo aninhado
        for (int i = 0; i < parts.length - 1; i++) {
            nestedPath = nestedPath.get(parts[i], Object.class);
        }

        // O último componente do campo aninhado
        String nestedFieldName = parts[parts.length - 1];

        // Cria o predicado com o último campo
        builder.and(nestedPath.getString(nestedFieldName).containsIgnoreCase(value));
    }
}
