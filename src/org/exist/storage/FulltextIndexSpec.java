/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2001-04,  The eXist Project
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 *  $Id$
 */
package org.exist.storage;

import java.util.ArrayList;
import java.util.Map;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * IndexPaths contains information about which parts of a document should be
 * fulltext-indexed for a specified doctype. It basically keeps a list of paths
 * to include and exclude from indexing. Paths are specified using
 * simple XPath syntax, e.g. //SPEECH will select any SPEECH elements,
 * //title/@id will select all id attributes being children of title elements.
 *
 * @author Wolfgang Meier
 */
public class FulltextIndexSpec {
	
    private static final String PATH_ATTRIB = "path";
    private static final String PRESERVE_CONTENT_ELEMENT = "preserveContent";
    private static final String EXCLUDE_INTERFACE = "exclude";
    private static final String INCLUDE_ELEMENT = "include";
    private static final String ALPHANUM_ATTRIB = "alphanum";
    private static final String ATTRIBUTES_ATTRIB = "attributes";
    private static final String DEFAULT_ATTRIB = "default";
    
    private static final NodePath[] ARRAY_TYPE = new NodePath[0];
    
    private final static Logger LOG = Logger.getLogger(FulltextIndexSpec.class);
    
    protected NodePath[] includePath;
    protected NodePath[] excludePath;
    protected NodePath[] preserveContent;
    
    protected boolean includeByDefault = true;
    protected boolean includeAttributes = true;
    protected boolean includeAlphaNum = true;
	
    /**
     * Constructor for the IndexPaths object
     *
     * @param def if set to true, include everything by default. In this case
     * use exclude elements to specify the excluded parts.
     */
    public FulltextIndexSpec(Map namespaces, Element node) {
        includeByDefault = true;
        ArrayList includeList = new ArrayList(  );
        ArrayList excludeList = new ArrayList(  );
        ArrayList preserveList = new ArrayList(  );
        String def = node.getAttribute(DEFAULT_ATTRIB);
        if(def != null && def.length() > 0) {
            includeByDefault = def.equals("all");
        }
        String indexAttributes = node.getAttribute(ATTRIBUTES_ATTRIB);
		if (indexAttributes != null && indexAttributes.length() > 0) {
			setIncludeAttributes(indexAttributes.equals("true") || indexAttributes.equals("yes"));
		}

		String indexAlphaNum = node.getAttribute(ALPHANUM_ATTRIB);
		if (indexAlphaNum != null && indexAlphaNum.length() > 0)
			setIncludeAlphaNum(indexAlphaNum.equals("true") || indexAlphaNum.equals("yes"));

		// check paths to include/exclude
		NodeList children = node.getChildNodes();
		String ps;
		Node next;
		for(int j = 0; j < children.getLength(); j++) {
		    next = children.item(j);
		    if(INCLUDE_ELEMENT.equals(next.getLocalName())) {
		        ps = ((Element) next).getAttribute(PATH_ATTRIB);
                includeList.add( new NodePath(namespaces, ps) );
		    } else if(EXCLUDE_INTERFACE.equals(next.getLocalName())) {
		        ps = ((Element) next).getAttribute(PATH_ATTRIB);
                excludeList.add( new NodePath(namespaces, ps) );
		    } else if(PRESERVE_CONTENT_ELEMENT.equals(next.getLocalName())) {
		        ps = ((Element) next).getAttribute(PATH_ATTRIB);
                preserveList.add( new NodePath(namespaces, ps) );
		    }
		}
        includePath = (NodePath[]) includeList.toArray(ARRAY_TYPE);
        excludePath = (NodePath[]) excludeList.toArray(ARRAY_TYPE);
        preserveContent = (NodePath[]) preserveList.toArray(ARRAY_TYPE);
    }

	/**
	 * Returns false if all elements are indexed, true 
	 * if indexation is selective.
	 * 
	 * @return
	 */
	public boolean isSelective() {
		if((includeByDefault && excludePath.length > 0) ||
			((!includeByDefault) && includePath.length > 0))
			return true;
		return false;
	}
	
    /**
     * Include attribute values?
     *
     * @param index The new includeAttributes value
     */
    private void setIncludeAttributes( boolean index ) {
        includeAttributes = index;
    }

    /**
     * Include attribute values?
     *
     * @return The includeAttributes value
     */
    public boolean getIncludeAttributes(  ) {
        return includeAttributes;
    }

    /**
     * Include alpha-numeric data, i.e. numbers, serials, URLs and so on?
     *
     * @param index include alpha-numeric data
     */
    private void setIncludeAlphaNum( boolean index ) {
        includeAlphaNum = index;
    }

    /**
     * Include alpha-numeric data?
     *
     * @return 
     */
    public boolean getIncludeAlphaNum(  ) {
        return includeAlphaNum;
    }
	
    /**
     * Check if a given path should be indexed.
     *
     * @param path path to the node
     *
     * @return Description of the Return Value
     */
    public boolean match( NodePath path ) {
        if ( includeByDefault ) {
            // check exclusions
            for (int i = 0; i < excludePath.length; i++)
                if( excludePath[i].match(path) )
                    return false;
                
            return true;
        }

        for (int i = 0; i < includePath.length; i++) {
            if( includePath[i].match(path) )
                return true;
        }
        return false;
    }
    
	/**
     * Check if a given path should be indexed.
     *
     * @param path path to the node
     *
     * @return Description of the Return Value
     */
    public boolean matchAttribute( NodePath path ) {
        if ( includeAttributes) {
            // check exclusions
            for (int i = 0; i < excludePath.length; i++)
                if( excludePath[i].match(path) )
                    return false;
                
            return true;
        }

        for (int i = 0; i < includePath.length; i++) {
            if( includePath[i].match(path) )
                return true;
        }
        return false;
    }
	
    /**
     * Check if a given path should be preserveContent.
     *
     * @param path path to the node
     *
     * @return Description of the Return Value
     */

      public boolean preserveContent( NodePath path ) {
          for (int i = 0; i < preserveContent.length; i++) { 
              if( preserveContent[i].match(path) )
                  return true;
          }
          return false;
    }



}
