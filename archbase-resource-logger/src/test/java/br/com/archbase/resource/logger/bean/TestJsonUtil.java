package br.com.archbase.resource.logger.bean;

import br.com.archbase.resource.logger.utils.JsonUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestJsonUtil {

    @Test
    public void compareSerializzedDeserializedObject() {
        User user = new User(1, "joao@example.com.br", "password");
        String serializedUser = JsonUtil.toJson(user);
        User deserialziedUser = JsonUtil.fromJson(serializedUser, User.class);

        assertEquals(user, deserialziedUser);
    }

}
