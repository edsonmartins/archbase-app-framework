package teste.offline.app;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Pedido {
    @Id
    public String id;

    public Pedido() {
    }

    public Pedido(String id) {
        this.id = id;
    }
}
