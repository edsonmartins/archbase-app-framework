package br.com.archbase.resource.logger.helpers;

//import com.github.valfirst.slf4jtest.TestLogger;

import java.util.Base64;
import java.util.regex.Pattern;

public class Utils {

//    public static List<Map<String, String>> getFormattedLogEvents(@Nonnull TestLogger logger) {
//        return logger.getAllLoggingEvents()
//                .stream()
//                .map(x -> ImmutableMap.<String, String>of(
//                        "level", x.getLevel().toString(),
//                        "message", x.getMessage() + (x.getThrowable().isPresent() ? " " + x.getThrowable().get().toString() : "")
//                ))
//                .collect(Collectors.toList());
//    }

    public static String generateBasicAuthToken(String username, String password) {
        return "Basic " + Base64.getEncoder().encodeToString(("username" + ":" + "password").getBytes());
    }

    public static boolean lol(String expected, String actual) {
        return Pattern.compile(expected).matcher(actual).matches();
    }
}
