//$HeadURL$
/*----------------    FILE HEADER  ------------------------------------------

 This file is part of deegree.
 Copyright (C) 2001-2009 by:
 EXSE, Department of Geography, University of Bonn
 http://www.giub.uni-bonn.de/deegree/
 lat/lon GmbH
 http://www.lat-lon.de

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

 Contact:

 Andreas Poth  
 lat/lon GmbH 
 Aennchenstr. 19
 53115 Bonn
 Germany
 E-Mail: poth@lat-lon.de

 Prof. Dr. Klaus Greve
 Department of Geography
 University of Bonn
 Meckenheimer Allee 166
 53115 Bonn
 Germany
 E-Mail: greve@giub.uni-bonn.de


 ---------------------------------------------------------------------------*/
package org.deegree.feature.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.xerces.xs.XSModel;
import org.deegree.feature.i18n.Messages;
import org.deegree.feature.types.property.FeaturePropertyType;
import org.deegree.feature.types.property.PropertyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines a number of {@link FeatureType}s and their substitution relations.
 * 
 * @author <a href="mailto:schneider@lat-lon.de">Markus Schneider </a>
 * @author last edited by: $Author:$
 * 
 * @version $Revision:$, $Date:$
 */
public class ApplicationSchema {

    private static final Logger LOG = LoggerFactory.getLogger( ApplicationSchema.class );

    private final Map<QName, FeatureType> ftNameToFt = new HashMap<QName, FeatureType>();

    // key: feature type A, value: feature type B (A is in substitutionGroup B)
    private final Map<FeatureType, FeatureType> ftToSubstitutionGroup = new HashMap<FeatureType, FeatureType>();

    // key: feature type A, value: feature types B0...Bn (A is in substitutionGroup B0,
    // B0 is in substitutionGroup B1, ..., B(n-1) is in substitutionGroup Bn)
    private final Map<FeatureType, List<FeatureType>> ftToSubstitutionGroups = new HashMap<FeatureType, List<FeatureType>>();

    private final XSModel model;

    /**
     * Creates a new <code>ApplicationSchema</code> from the given {@link FeatureType}s and their substitution group
     * relation.
     * 
     * @param fts
     *            all feature types (abstract and non-abstract)
     * @param ftSubstitutionGroupRelation
     *            key: feature type A, value: feature type B (A is in substitutionGroup B)
     * @param model
     * @throws IllegalArgumentException
     *             if a feature type cannot be resolved (i.e. it is referenced but not defined)
     */
    public ApplicationSchema( FeatureType[] fts, Map<FeatureType, FeatureType> ftSubstitutionGroupRelation,
                              XSModel model ) throws IllegalArgumentException {
        for ( FeatureType ft : fts ) {
            ftNameToFt.put( ft.getName(), ft );
        }

        // build substitution group lookup maps
        for ( FeatureType ft : ftSubstitutionGroupRelation.keySet() ) {
            this.ftToSubstitutionGroup.put( ft, ftSubstitutionGroupRelation.get( ft ) );
        }
        for ( FeatureType ft : fts ) {
            List<FeatureType> substitutionGroups = new ArrayList<FeatureType>();
            FeatureType substitutionGroup = ftToSubstitutionGroup.get( ft );
            while ( substitutionGroup != null ) {
                substitutionGroups.add( substitutionGroup );
                substitutionGroup = ftToSubstitutionGroup.get( substitutionGroup );
            }
            ftToSubstitutionGroups.put( ft, substitutionGroups );
        }

        // resolve values in feature property declarations
        for ( FeatureType ft : fts ) {
            for ( PropertyType pt : ft.getPropertyDeclarations() ) {
                if ( pt instanceof FeaturePropertyType ) {
                    QName referencedFtName = ( (FeaturePropertyType) pt ).getFTName();
                    FeatureType referencedFt = ftNameToFt.get( referencedFtName );
                    if ( referencedFt == null ) {
                        String msg = Messages.getMessage( "ERROR_SCHEMA_UNKNOWN_FEATURE_TYPE_IN_PROPERTY",
                                                          referencedFtName, pt.getName() );
                        throw new IllegalArgumentException( msg );
                    }
                }
            }
        }

        this.model = model;
    }

    /**
     * Returns all feature types that are defined in this application schema.
     * 
     * @return all feature types that are defined in this application schema
     */
    public FeatureType[] getFeatureTypes() {
        FeatureType[] fts = new FeatureType[ftNameToFt.values().size()];
        int i = 0;
        for ( FeatureType ft : ftNameToFt.values() ) {
            fts[i++] = ft;
        }
        return fts;
    }

    /**
     * Retrieves the feature type with the given name.
     * 
     * @param ftName
     *            feature type name to look up
     * @return the feature type with the given name, or null if no such feature type exists
     */
    public FeatureType getFeatureType( QName ftName ) {
        return ftNameToFt.get( ftName );
    }

    /**
     * Retrieves all substitutions (abstract and non-abstract ones) for the given feature type.
     * 
     * @param ft
     * @return all substitutions for the given feature type
     */
    public FeatureType getSubstitutions( FeatureType ft ) {
        return null;
    }

    /**
     * Retrieves all concrete substitutions for the given feature type.
     * 
     * @param ft
     * @return all concrete substitutions for the given feature type
     */    
    public FeatureType getConcreteSubstitutions( FeatureType ft ) {
        return null;
    }

    /**
     * Determines whether a feature type is substitutable for another feature type according to the schema.
     * <p>
     * This is true, iff <code>substitution</code> is either:
     * <ul>
     * <li>equal to <code>ft</code></li>
     * <li>in the substitutionGroup of <code>ft</code></li>
     * <li>transititively substitutable for <code>ft</code></li>
     * </ul>
     * 
     * @param ft
     * @param substitution
     * @return true, if the first feature type is a valid substitution for the second
     */
    public boolean isValidSubstitution( FeatureType ft, FeatureType substitution ) {
        LOG.debug( "ft: " + ft.getName() + ", substitution: " + substitution.getName() );
        if ( ft == substitution ) {
            return true;
        }
        List<FeatureType> substitutionGroups = ftToSubstitutionGroups.get( substitution );
        if ( substitutionGroups != null ) {
            for ( FeatureType substitutionGroup : substitutionGroups ) {
                if ( ft == substitutionGroup ) {
                    return true;
                }
            }
        }
        return false;
    }

    public XSModel getXSModel() {
        return model;
    }
}
