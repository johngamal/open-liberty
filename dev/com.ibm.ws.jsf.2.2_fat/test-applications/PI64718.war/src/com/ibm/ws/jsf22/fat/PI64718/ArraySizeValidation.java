/*
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package com.ibm.ws.jsf22.fat.PI64718;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ArraySizeValidation implements ConstraintValidator<ArraySizeValidator, String[]> {

    @Override
    public void initialize(ArraySizeValidator constraintAnnotation) {
    }

    @Override
    public boolean isValid(String[] value, ConstraintValidatorContext context) {
        return value != null && value.length == 2;
    }

}
