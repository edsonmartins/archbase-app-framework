package br.com.archbase.ddd.domain.base;

import br.com.archbase.ddd.domain.contracts.Identifier;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
@EqualsAndHashCode
public class ArchbaseIdentifier implements Identifier {

    private final String id;

    public ArchbaseIdentifier() {
        this.id = UUID.randomUUID().toString();
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
