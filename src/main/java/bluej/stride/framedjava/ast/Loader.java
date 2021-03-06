/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016 Michael Kölling and John Rosenberg
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.stride.framedjava.ast;

import bluej.parser.entity.EntityResolver;
import bluej.stride.framedjava.elements.*;
import bluej.utility.Debug;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ParsingException;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;

public class Loader
{
    public static CodeElement loadElement(Element el)
    {
        switch (el.getLocalName())
        {
        case AssignElement.ELEMENT: return new AssignElement(el);
        case BlankElement.ELEMENT: return new BlankElement(el);
        case BreakElement.ELEMENT: return new BreakElement(el);
        case BreakpointElement.ELEMENT: return new BreakpointElement(el);
        case CallElement.ELEMENT: return new CallElement(el);
        case CaseElement.ELEMENT: return new CaseElement(el);
        case ClassElement.ELEMENT: throw new IllegalArgumentException("Cannot load top level element via loadElement: " + el.getLocalName());
        case CommentElement.ELEMENT: return new CommentElement(el);
        case ConstructorElement.ELEMENT: return new ConstructorElement(el);
        case ForeachElement.ELEMENT: return new ForeachElement(el);
        case IfElement.ELEMENT: return new IfElement(el);
        case ImportElement.ELEMENT: return new ImportElement(el);
        case InterfaceElement.ELEMENT: throw new IllegalArgumentException("Cannot load top level element via loadElement: " + el.getLocalName());
        case MethodProtoElement.ELEMENT: return new MethodProtoElement(el);
        case NormalMethodElement.ELEMENT: return new NormalMethodElement(el);
        case ReturnElement.ELEMENT: return new ReturnElement(el);
        case SwitchElement.ELEMENT: return new SwitchElement(el);
        case ThrowElement.ELEMENT: return new ThrowElement(el);
        case TryElement.ELEMENT: return new TryElement(el);
        case VarElement.ELEMENT: return new VarElement(el);
        case WhileElement.ELEMENT: return new WhileElement(el);
        default: return null;
        }
    }

    public static CodeElement loadElement(String elementString)
    {

        try {
            return loadElement(new Element( 
                    new Builder().build(new StringReader(elementString)).getRootElement()
                    ));
        }
        catch (ParsingException | IOException e) {
            Debug.reportError(e);
        }
        return null;
    }
    
    public static TopLevelCodeElement loadTopLevelElement(File file, EntityResolver resolver)
    {
        try {
            Document xml = new Builder().build(file);
            return Loader.loadTopLevelElement(xml.getRootElement(), resolver);
        }
        catch (ParsingException | IOException e) {
            Debug.reportError(e);
        }
        return null;
    }

    public static TopLevelCodeElement loadTopLevelElement(Element el, EntityResolver resolver)
    {
        switch (el.getLocalName()) {
            case ClassElement.ELEMENT: return new ClassElement(el, resolver);
            case InterfaceElement.ELEMENT: return new InterfaceElement(el, resolver);
            default: throw new IllegalArgumentException("Unknown top level element: " + el.getLocalName());
        }
    }

    public static TopLevelCodeElement buildTopLevelElement(String template, EntityResolver resolver, String topLevelName, String packageName)
    {
        switch (template) {
            case "stdclass": return  new ClassElement(resolver, false, topLevelName, packageName, Arrays.asList(new ConstructorElement("Constructor for objects of class " + topLevelName)));
            case "abstract": return new ClassElement(resolver, true, topLevelName, packageName, Collections.emptyList());
            case "interface": return new InterfaceElement(resolver, topLevelName, packageName);
            default: throw new IllegalArgumentException("Unknown template: " + template);
        }
    }
}
