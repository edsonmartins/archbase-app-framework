package teste.offline.manual;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Cliente {
    @Id
    public String id;

    public Cliente() {
    }

    public Cliente(String id) {
        this.id = id;
    }
}
