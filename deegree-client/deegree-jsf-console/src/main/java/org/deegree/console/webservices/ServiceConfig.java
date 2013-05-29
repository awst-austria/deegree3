/*----------------------------------------------------------------------------
 This file is part of deegree
 Copyright (C) 2001-2013 by:
 - Department of Geography, University of Bonn -
 and
 - lat/lon GmbH -
 and
 - Occam Labs UG (haftungsbeschränkt) -
 and others

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

 e-mail: info@deegree.org
 website: http://www.deegree.org/
----------------------------------------------------------------------------*/
package org.deegree.console.webservices;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.deegree.console.Config;
import org.deegree.services.OWS;
import org.deegree.services.OWSProvider;
import org.deegree.services.metadata.OWSMetadataProviderManager;
import org.deegree.services.metadata.provider.OWSMetadataProviderProvider;
import org.deegree.workspace.ResourceIdentifier;
import org.deegree.workspace.ResourceLocation;
import org.deegree.workspace.ResourceManager;
import org.deegree.workspace.ResourceMetadata;
import org.deegree.workspace.standard.DefaultResourceIdentifier;
import org.deegree.workspace.standard.DefaultResourceLocation;

public class ServiceConfig extends Config {

    public ServiceConfig( ResourceMetadata<?> metadata, ResourceManager<?> resourceManager ) {
        super( metadata, resourceManager, "/console/webservices/index", true );
    }

    public String editMetadata()
                            throws IOException {
        ResourceIdentifier ident = new DefaultResourceIdentifier( OWSMetadataProviderProvider.class, id );
        ResourceLocation loc = new DefaultResourceLocation( null, ident );
        workspace.add( loc );
        ResourceMetadata md = workspace.getResourceMetadata( ident.getProvider(), id );
        ResourceManager mgr = workspace.getResourceManager( OWSMetadataProviderManager.class );
        Config metadataConfig = new Config( md, mgr, "/console/webservices/index", true );
        return metadataConfig.edit();
    }

    public String getCapabilitiesUrl() {
        OWS ows = workspace.getResource( OWSProvider.class, id );
        String type = ( (OWSProvider) ows.getMetadata().getProvider() ).getImplementationMetadata().getImplementedServiceName()[0];

        HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        StringBuffer sb = req.getRequestURL();

        // HACK HACK HACK
        int index = sb.indexOf( "/console" );
        return sb.substring( 0, index ) + "/services/" + id + "?service=" + type + "&request=GetCapabilities";
    }
}
