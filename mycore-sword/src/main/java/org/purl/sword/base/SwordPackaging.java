/**
 * Copyright (c) 2007-2009, Aberystwyth University
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above
 *    copyright notice, this list of conditions and the
 *    following disclaimer.
 *
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 *  - Neither the name of the Centre for Advanced Software and
 *    Intelligent Systems (CASIS) nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package org.purl.sword.base;

import java.util.Properties;

/**
 *
 * @author Neil Taylor (nst@aber.ac.uk)
 */
public class SwordPackaging extends BasicStringContentElement
{
    private static final XmlName XML_NAME =
            new XmlName(Namespaces.PREFIX_SWORD, "packaging", Namespaces.NS_SWORD);

    public SwordPackaging()
    {
        super(XML_NAME);
    }

    public SwordPackaging(String version)
    {
        this();
        setContent(version); 
    }

    /**
     * Get the name of this element.
     */
    public static XmlName elementName()
    {
        return XML_NAME;
    }

    /**
     * Validate the content for this element.
     *
     * @param validationContext The context for this validation step. Not used
     *                          in this object.
     * @return A SwordValidationInfo object. This will be null if there is
     * no validation information to return, i.e. the content does not contain
     * any warnings or errors. 
     */
    @Override
    protected SwordValidationInfo validateContent(Properties validationContext)
    {
        SwordValidationInfo result = super.validateContent(validationContext);
        if( result == null )
        {
           // content is not empty, so check that the value is a valid value from
           // sword Types
           SwordContentPackageTypes types = SwordContentPackageTypes.instance();
           if( types.isValidType(content) )
           {
              result = new SwordValidationInfo(xmlName,
                               "The packaging type does not match one of the approved SWORD Types",
                               SwordValidationInfoType.WARNING);
           }
        }
        return result;
    }
}
