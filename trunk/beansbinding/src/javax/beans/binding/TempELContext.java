package javax.beans.binding;

import javax.el.*;
import com.sun.el.lang.*;

/**
 * This class is temporary. Moving forward, we'll instead have a factory for
 * configuring this.
 *
 * @author Shannon Hickey
 */
public class TempELContext extends ELContext {
    private final CompositeELResolver resolver;
    private final VariableMapper variableMapper = new VariableMapperImpl();
    private final FunctionMapper functionMapper = new FunctionMapperImpl();
    
    public TempELContext() {
        resolver = new CompositeELResolver();
        // PENDING(shannonh) - EL also has an ArrayELResolver. Should that be added too?
        // PENDING(shannonh) - custom resolver to resolve special bean properties
        resolver.add(new MapELResolver());
        resolver.add(new BeanELResolver());
    }
    
    public ELResolver getELResolver() {
        return resolver;
    }
    
    public FunctionMapper getFunctionMapper() {
        return functionMapper;
    }
    
    public VariableMapper getVariableMapper() {
        return variableMapper;
    }
}
