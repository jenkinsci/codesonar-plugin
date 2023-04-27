package org.jenkinsci.plugins.codesonar.parsers;

import java.io.IOException;

public abstract class AbstractJsonParser<T> {

    public abstract T parseObject() throws IOException;
    
}
