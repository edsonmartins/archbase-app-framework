package br.com.archbase.ddd.domain.base;

import br.com.archbase.ddd.domain.contracts.Identifier;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@EqualsAndHashCode
public class ArchbaseIdentifier implements Identifier {

    private final String id;

    public ArchbaseIdentifier() {
        this.id = UUID.randomUUID().toString();
    }

    public ArchbaseIdentifier(String id) {
        this.id = id;
    }

    public String getIdentifier() {
        return id;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return id;
    }
}
