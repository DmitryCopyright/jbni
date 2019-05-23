package com.tuneit.jackalope.integration.babelnet.cli;

import com.tuneit.jackalope.utils.annotation.GroovyAccessible;
import it.uniroma1.lcl.babelnet.BabelNet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@GroovyAccessible(value = "term")
public class Terminal {

    @Autowired
    private BabelNet bn;

    // test methods here...

}
