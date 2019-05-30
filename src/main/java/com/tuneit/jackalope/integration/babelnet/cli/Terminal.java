package com.tuneit.jackalope.integration.babelnet.cli;

import com.tuneit.jackalope.api.Jackalope;
import com.tuneit.jackalope.utils.annotation.GroovyAccessible;
import it.uniroma1.lcl.babelnet.BabelNet;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
@GroovyAccessible(value = "term")
public class Terminal implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if(this.applicationContext != null) {
            return;
        }
        this.applicationContext = applicationContext;
    }

    /**
     * Shutdown application from groovy shell
     */
    public void shutdown() {
        ((ConfigurableApplicationContext)(applicationContext)).close();
    }

    /*                      */
    /*  Terminal functions  */
    /*                      */

    @Autowired private BabelNet bn;
    @Autowired private Jackalope jack;

    // test methods here...

}
