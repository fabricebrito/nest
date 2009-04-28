/*
 * $Id: ParamException.java,v 1.1 2009-04-28 14:39:33 lveci Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */
package org.esa.beam.framework.param;

//@todo 1 se/** - add (more) class documentation

public class ParamException extends Exception {

    private final Parameter _parameter;

    public ParamException(Parameter parameter, String message) {
        super(message);
        _parameter = parameter;
    }

    public Parameter getParameter() {
        return _parameter;
    }
}