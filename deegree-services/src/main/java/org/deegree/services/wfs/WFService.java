//$HeadURL: svn+ssh://mschneider@svn.wald.intevation.org/deegree/base/trunk/resources/eclipse/files_template.xml $
/*----------------------------------------------------------------------------
 This file is part of deegree, http://deegree.org/
 Copyright (C) 2001-2009 by:
 Department of Geography, University of Bonn
 and
 lat/lon GmbH

 This library is free software; you can redistribute it and/or modify it under
 the terms of the GNU Lesser General Public License as published by the Free
 Software Foundation; either version 2.1 of the License, or (at your option)
 any later version.
 This library is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 details.
 You should have received a copy of the GNU Lesser General Public License
 along with this library; if not, write to the Free Software Foundation, Inc.,
 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

 Contact information:

 lat/lon GmbH
 Aennchenstr. 19, 53177 Bonn
 Germany
 http://lat-lon.de/

 Department of Geography, University of Bonn
 Prof. Dr. Klaus Greve
 Postfach 1147, 53001 Bonn
 Germany
 http://www.geographie.uni-bonn.de/deegree/

 e-mail: info@deegree.org
 ----------------------------------------------------------------------------*/

package org.deegree.services.wfs;

import static javax.xml.XMLConstants.DEFAULT_NS_PREFIX;
import static org.deegree.services.i18n.Messages.get;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import org.deegree.feature.persistence.FeatureStore;
import org.deegree.feature.persistence.FeatureStoreException;
import org.deegree.feature.persistence.FeatureStoreManager;
import org.deegree.feature.types.ApplicationSchema;
import org.deegree.feature.types.FeatureType;
import org.deegree.services.controller.wfs.WFSController;
import org.deegree.services.jaxb.wfs.ServiceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the {@link FeatureStore} instances for the {@link WFSController}.
 * 
 * @see WFSController
 * 
 * @author <a href="mailto:schneider@lat-lon.de">Markus Schneider</a>
 * @author last edited by: $Author: schneider $
 * 
 * @version $Revision: $, $Date: $
 */
public class WFService {

    private static final Logger LOG = LoggerFactory.getLogger( WFService.class );

    private final Map<QName, FeatureType> ftNameToFt = new HashMap<QName, FeatureType>();

    private final Map<ApplicationSchema, FeatureStore> schemaToStore = new HashMap<ApplicationSchema, FeatureStore>();

    private final Map<String, String> prefixToNs = new LinkedHashMap<String, String>();

    private Set<String> hintedNamespaces;

    private int indexPrefix = 0;

    private Map<String, String> targetNsToPrefix = new LinkedHashMap<String, String>();

    /**
     * @param sc
     * @param baseURL
     * @throws FeatureStoreException
     */
    public void init( ServiceConfiguration sc, String baseURL )
                            throws FeatureStoreException {

        LOG.info( "Adding configured feature stores." );

        // filling prefix map with the provided NamespaceHints
        hintedNamespaces = new HashSet<String>();

        for (FeatureStore fs : FeatureStoreManager.getAll() ) {
            addStore( fs );
            addNotYetHintedNamespaces( fs.getSchema().getFeatureTypes() );
        }

        LOG.debug( "The following prefix-to-namespace and namespace-to-prefix bindings are used for resolution..." );
        for ( String prefix : prefixToNs.keySet() ) {
            LOG.debug( prefix + " --> " + prefixToNs.get( prefix ) );
        }
        for ( String ns : targetNsToPrefix.keySet() ) {
            LOG.debug( ns + " <-- " + targetNsToPrefix.get( ns ) );
        }
    }

    private void addNotYetHintedNamespaces( FeatureType[] featureTypes ) {
        for ( int i = 0; i < featureTypes.length; i++ ) {
            if ( !hintedNamespaces.contains( featureTypes[i].getName().getNamespaceURI() ) ) {
                hintedNamespaces.add( featureTypes[i].getName().getNamespaceURI() );

                if ( featureTypes[i].getName().getPrefix() != null
                     && !featureTypes[i].getName().getPrefix().equals( "" ) ) {
                    // add the prefixes that were forgotten to be added in the NamespaceHint elements from the
                    // configuration
                    prefixToNs.put( featureTypes[i].getName().getPrefix(), featureTypes[i].getName().getNamespaceURI() );
                } else {
                    // the elements that have no prefix must be in an application schema namespace
                    targetNsToPrefix.put( featureTypes[i].getName().getNamespaceURI(), "app" + indexPrefix );
                    indexPrefix++;
                }
            }
        }
    }

    /**
     * 
     */
    public void destroy() {
        // TODO use manager to clear feature stores?
        // for ( FeatureStore fs : schemaToStore.values() ) {
        // LOG.debug( "Shutting down feature store: " + fs );
        // fs.destroy();
        // }
    }

    /**
     * Returns the qualified names of all served {@link FeatureType}s.
     * 
     * @return the qualified names, never <code>null</code>
     */
    public QName[] getFeatureTypeNames() {
        return ftNameToFt.keySet().toArray( new QName[ftNameToFt.size()] );
    }

    /**
     * Returns all {@link FeatureType}s.
     * 
     * @return served feature types, may be empty, but never <code>null</code>
     */
    public Collection<FeatureType> getFeatureTypes() {
        return ftNameToFt.values();
    }

    /**
     * Looks up the {@link FeatureType} with the given qualified name (in a namespace-tolerant manner).
     * <p>
     * This method is tolerant to improve interoperability with clients (especially WFS 1.0.0) that only provide the
     * local name or the prefixed name without a namespace binding.
     * </p>
     * 
     * @param ftName
     *            feature type to look up, must not be <code>null</code>
     * @return feature type with the given name, or <code>null</code> if no such feature type is served
     */
    public FeatureType lookupFeatureType( QName ftName ) {

        FeatureType ft = ftNameToFt.get( ftName );

        if ( ft == null ) {
            // try namespace tolerant matching
            if ( ftName.getPrefix() != null && !DEFAULT_NS_PREFIX.equals( ftName.getPrefix() ) ) {
                // try to match prefix and localPart
                for ( Entry<QName, FeatureType> candidate : ftNameToFt.entrySet() ) {
                    if ( ftName.getLocalPart().equals( candidate.getKey().getLocalPart() )
                         && ftName.getPrefix().equals( candidate.getKey().getPrefix() ) ) {
                        ft = candidate.getValue();
                        LOG.warn( "Feature type name " + ftName + " (without namespace) was matched with "
                                  + candidate.getKey()
                                  + " (it cannot be found among the served feature types otherwise)." );
                        break;
                    }
                }
            }
            if ( ft == null ) {
                // try to match localPart only
                for ( Entry<QName, FeatureType> candidate : ftNameToFt.entrySet() ) {
                    if ( ftName.getLocalPart().equals( candidate.getKey().getLocalPart() ) ) {
                        ft = candidate.getValue();
                        LOG.warn( "Feature type name " + ftName + " (without namespace) was matched with "
                                  + candidate.getKey()
                                  + " (it cannot be found among the served feature types otherwise)." );
                        break;
                    }
                }
            }
        }
        return ft;
    }

    /**
     * Returns the {@link FeatureStore} instance which is responsible for the specified feature type.
     * 
     * @param ftName
     *            name of the {@link FeatureType}
     * @return the responsible {@link FeatureStore} or <code>null</code> if no such store exists, i.e. the specified
     *         feature type is not served
     */
    public FeatureStore getStore( QName ftName ) {
        FeatureType ft = lookupFeatureType( ftName );
        if ( ft == null ) {
            return null;
        }
        return schemaToStore.get( ft.getSchema() );
    }

    /**
     * Get the prefix-to-namespace map that is constructed from the NamespaceHints in the configuration
     * 
     * @return the prefix-to-namespace map
     */
    public Map<String, String> getPrefixToNs() {
        return prefixToNs;
    }

    /**
     * Get the namespace-to-prefix bindings for the namespaces of the application schemas.
     * 
     * @return the namespace-to-prefix map
     */
    public Map<String, String> getTargetNsToPrefix() {
        return targetNsToPrefix;
    }

    /**
     * Returns all registered {@link FeatureStore} instances.
     * 
     * @return all registered feature stores
     */
    public FeatureStore[] getStores() {
        Set<FeatureStore> stores = new HashSet<FeatureStore>( schemaToStore.values() );
        return stores.toArray( new FeatureStore[stores.size()] );
    }

    /**
     * Registers a new {@link FeatureStore} to the WFS.
     * 
     * @param fs
     *            store to be registered
     */
    public void addStore( FeatureStore fs ) {
        synchronized ( this ) {
            if ( schemaToStore.containsValue( fs ) ) {
                String msg = get( "WFS_FEATURESTORE_ALREADY_REGISTERED", fs );
                LOG.error( msg );
                throw new IllegalArgumentException( msg );
            }
            for ( FeatureType ft : fs.getSchema().getFeatureTypes() ) {
                if ( ftNameToFt.containsKey( ft.getName() ) ) {
                    String msg = get( "WFS_FEATURETYPE_ALREADY_SERVED", ft.getName() );
                    LOG.error( msg );
                    throw new IllegalArgumentException( msg );
                }
            }
            schemaToStore.put( fs.getSchema(), fs );
            for ( FeatureType ft : fs.getSchema().getFeatureTypes() ) {
                ftNameToFt.put( ft.getName(), ft );
            }
        }
    }

    /**
     * Deregisters the specified {@link FeatureStore} from the WFS.
     * 
     * @param fs
     *            store to be registered
     */
    public void removeStore( FeatureStore fs ) {
        synchronized ( this ) {

        }
    }
}
