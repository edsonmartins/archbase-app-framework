package br.com.archbase.ddd.infraestructure.resource;

import br.com.archbase.ddd.domain.contracts.AggregateRoot;
import br.com.archbase.ddd.domain.contracts.Identifier;
import br.com.archbase.ddd.domain.contracts.Repository;
import br.com.archbase.ddd.infraestructure.service.SimpleArchbaseService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;


public abstract class SimpleArchbaseRestController<T extends AggregateRoot<T, ID>, ID extends Serializable & Identifier, N extends Number & Comparable<N>> extends SimpleArchbaseQueryRestController<T, ID, N> {

    /**
     * Método abstrato que irá fornecer a classe de serviço para ser usada no
     * controller.
     *
     * @return
     */
    public abstract SimpleArchbaseService<T, ID, N> getService();

    /**
     * Método abstrato que irá fornecer a classe de repositório para ser usada no
     * controller.
     *
     * @return
     */
    public abstract Repository<T, ID, N> getRepository();

    /**
     * Insere ou atualiza um objeto.
     *
     * @param object Objeto a ser salvo
     * @return Objeto salvo
     * @throws Exception
     */
    @RequestMapping(value = "/", method = {RequestMethod.POST, RequestMethod.PUT})
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public T save(@RequestBody T object) {
        return getService().save(object);
    }

    /**
     * Valida um objeto.
     *
     * @param object Objeto a ser validado
     * @return Objeto validado
     * @throws Exception
     */

    @RequestMapping(value = "/validate", method = {RequestMethod.POST, RequestMethod.PUT})
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void validate(@RequestBody T object) {
        this.getService().validate(object);
    }

    /**
     * Valida um objeto apenas para os grupos informados.
     *
     * @param object Objeto a ser validado
     * @param groups Grupo de validação
     * @return Objeto validado
     * @throws Exception
     */

    @RequestMapping(value = "/validateGroup", method = {RequestMethod.POST, RequestMethod.PUT})
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void validateGroup(@RequestBody T object, Class<?>... groups) {
        this.getService().validateGroup(object, groups);
    }

    /**
     * Remove um objeto pelo ID.
     *
     * @param id Identificador do objeto
     * @return Objeto removido.
     * @throws Exception
     */
    @DeleteMapping(value = "/")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public T removeById(@RequestBody ID id) {
        return this.getService().removeById(id);
    }

}
