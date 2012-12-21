package org.mycore.webcli.cli.command;

import java.lang.reflect.Method;
import java.text.Format;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

import org.mycore.common.MCRConfigurationException;
import org.mycore.frontend.cli.MCRCommand;

public abstract class MCRWebCLICommand extends MCRCommand {
    
    public MCRWebCLICommand() {
        setClassName();
        setMethod();
        try {
            setMethodName();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        setParameterTypes();
        setSuffix();
        setMessageFormat();
        setHelpText();
    }
    
    protected abstract String commandName();
    protected abstract String helpText();
    
    private void setHelpText() {
        if (helpText() != null && !helpText().equals("")) {
            this.help = helpText();
        } else {
            this.help = "No help text available for this command";
        }
    }



    protected void setSuffix() {
        this.suffix = commandName();
    }
    
    protected void setMessageFormat() {
        StringBuilder formatParam = new StringBuilder();
        ArrayList<Format> formats = new ArrayList<Format>();
        for (int i = 0; i < this.parameterTypes.length; i++) {
            formatParam.append(" {" + i + "}");
            if (parameterTypes[i].equals(Integer.TYPE)){
                formats.add(NumberFormat.getIntegerInstance());
            } else if (parameterTypes[i].equals(String.class)){
                formats.add(null);
            } else {
                throw new MCRConfigurationException("Error while parsing command definitions for command line interface:\n" + "Unsupported argument type '"
                        + parameterTypes[i].getName() + "' in command " + this.suffix);
            }
        }
        
        String format = this.suffix + formatParam.toString();
        this.messageFormat = new MessageFormat(format);
        
        if (formats.size() > 0){
            this.messageFormat.setFormats(formats.toArray(new Format[formats.size()]));
        }
    }

    protected void setParameterTypes() {
        this.parameterTypes = this.method.getParameterTypes();
    }

    protected void setMethodName() throws NoSuchMethodException {
        if (this.method != null) {
            this.methodName = this.method.getName();
        } else {
            throw new NoSuchMethodException("The class " + this.getClass().getName() + " does not has a method which name start with \"cmd\"!");
        }
    }

    protected void setMethod() {
        if (this.method == null) {
            Method[] methods = this.getClass().getMethods();
            for (Method method1 : methods) {
                String methodName = method1.getName();
                if (methodName.startsWith("cmd")) {
                    this.method = method1;
                }
            }
        }
    }

    protected void setClassName() {
        className = this.getClass().getName();
    }
    
    @Override
    public String toString() {
        return suffix;
    }
}
