//$HeadURL: svn+ssh://lbuesching@svn.wald.intevation.de/deegree/base/trunk/resources/eclipse/files_template.xml $
/*----------------------------------------------------------------------------
 This file is part of deegree, http://deegree.org/
 Copyright (C) 2001-2010 by:
 - Department of Geography, University of Bonn -
 and
 - lat/lon GmbH -

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
package org.deegree.client.mdeditor.gui;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;

import org.deegree.client.mdeditor.configuration.ConfigurationException;
import org.deegree.client.mdeditor.configuration.form.FormConfigurationFactory;
import org.deegree.client.mdeditor.io.DataHandler;
import org.deegree.client.mdeditor.model.Dataset;
import org.deegree.client.mdeditor.model.DatasetInformation;
import org.deegree.client.mdeditor.model.FormConfiguration;
import org.deegree.client.mdeditor.model.FormField;
import org.deegree.client.mdeditor.model.FormFieldPath;
import org.slf4j.Logger;

/**
 * TODO add class documentation here
 * 
 * @author <a href="mailto:buesching@lat-lon.de">Lyn Buesching</a>
 * @author last edited by: $Author: lyn $
 * 
 * @version $Revision: $, $Date: $
 */
@ManagedBean
@SessionScoped
public class DatasetBean implements Serializable {

    private static final long serialVersionUID = 3712272954913623495L;

    private static final Logger LOG = getLogger( DatasetBean.class );

    private String selectedDataset;

    public void setSelectedDataset( String selectedDataset ) {
        this.selectedDataset = selectedDataset;
    }

    public String getSelectedDataset() {
        return selectedDataset;
    }

    public String getDatasetDescribtion()
                            throws ConfigurationException {
        if ( selectedDataset != null ) {
            for ( DatasetInformation di : getDatasetInformation() ) {
                if ( selectedDataset.equals( di.getIdentifier() ) ) {
                    return di.getDescribtion();
                }
            }
        }
        return null;
    }

    public List<DatasetInformation> getDatasetInformation()
                            throws ConfigurationException {
        List<DatasetInformation> datasetInformations = new ArrayList<DatasetInformation>();
        List<Dataset> datasets = DataHandler.getInstance().getDatasets();

        HttpSession session = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession( false );
        FormConfiguration manager = FormConfigurationFactory.getOrCreateFormConfiguration( session.getId() );

        for ( Dataset dataset : datasets ) {
            Map<String, Object> values = dataset.getValues();
            String id = dataset.getId();
            String title = null;
            String desc = null;
            if ( manager.getPathToIdentifier() != null ) {
                if ( manager.getPathToTitle() != null && values.get( manager.getPathToTitle().toString() ) != null ) {
                    title = values.get( manager.getPathToTitle().toString() ).toString();
                }
                if ( manager.getPathToDescription() != null
                     && values.get( manager.getPathToDescription().toString() ) != null ) {
                    desc = values.get( manager.getPathToDescription().toString() ).toString();
                }
            }
            datasetInformations.add( new DatasetInformation( id, title, desc ) );
        }
        return datasetInformations;
    }

    public Object loadDataset()
                            throws ConfigurationException {
        LOG.debug( "Load dataset with id " + selectedDataset );
        FacesContext fc = FacesContext.getCurrentInstance();
        String id = selectedDataset;
        if ( selectedDataset == null || selectedDataset.length() == 0 ) {
            FacesMessage msg = GuiUtils.getFacesMessage( fc, FacesMessage.SEVERITY_FATAL, "ERROR.LOAD.INVALID_ID", id );
            fc.addMessage( "LOAD_FAILED_INVALID_ID", msg );
            return "/page/form/errorPage.xhtml";
        }
        Dataset ds;
        try {
            ds = DataHandler.getInstance().getDataset( id );
        } catch ( Exception e ) {
            FacesMessage msg = GuiUtils.getFacesMessage( fc, FacesMessage.SEVERITY_FATAL, "ERROR.LOAD", id,
                                                         e.getMessage() );
            fc.addMessage( "LOAD_FAILED", msg );
            return "/page/form/errorPage.xhtml";
        }

        Map<String, Object> values = ds.getValues();

        FormFieldBean formfieldBean = (FormFieldBean) fc.getApplication().getELResolver().getValue( fc.getELContext(),
                                                                                                    null,
                                                                                                    "formFieldBean" );
        formfieldBean.clearFormFields();
        Map<String, FormField> formFields = formfieldBean.getFormFields();
        for ( String path : values.keySet() ) {
            if ( formFields.containsKey( path ) ) {
                formFields.get( path ).setValue( values.get( path ) );
            }
        }

        formfieldBean.setDataGroups( ds.getDataGroups() );

        boolean asTemplate = false;
        Map<String, String> params = fc.getExternalContext().getRequestParameterMap();
        for ( String reqName : params.keySet() ) {
            if ( reqName.endsWith( "asTemplate" ) ) {
                asTemplate = Boolean.parseBoolean( params.get( reqName ) );
                break;
            }
        }
        // set selected dataset to null => dataset is a NEW one
        if ( asTemplate ) {
            setSelectedDataset( null );

            HttpSession session = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(
                                                                                                                   false );
            FormConfiguration manager = FormConfigurationFactory.getOrCreateFormConfiguration( session.getId() );
            formfieldBean.clearFormField( manager.getPathToIdentifier() );

        }

        FacesMessage msg = GuiUtils.getFacesMessage( fc, FacesMessage.SEVERITY_INFO, "SUCCESS.LOAD", id );
        fc.addMessage( "LOAD_SUCCESS", msg );
        return "forms";
    }

    public Object newDataset() {
        LOG.debug( "create new dataset (clear forms)" );
        FacesContext fc = FacesContext.getCurrentInstance();
        FormFieldBean formfieldBean = (FormFieldBean) fc.getApplication().getELResolver().getValue( fc.getELContext(),
                                                                                                    null,
                                                                                                    "formFieldBean" );
        formfieldBean.clearFormFields();
        return "forms";
    }

    public Object validateDataset() {
        LOG.debug( "Validate dataset with id " + selectedDataset );
        return null;
    }

    public Object deleteDataset() {
        LOG.debug( "Delete dataset with id " + selectedDataset );
        FacesContext fc = FacesContext.getCurrentInstance();
        String id = selectedDataset;
        if ( selectedDataset == null || selectedDataset.length() == 0 ) {
            FacesMessage msg = GuiUtils.getFacesMessage( fc, FacesMessage.SEVERITY_FATAL,
                                                         "ERROR.DELETE_DATASET.INVALID_ID", id );
            fc.addMessage( "DELETE_DATASET_INVALID_ID", msg );
            return "/page/form/errorPage.xhtml";
        }

        DataHandler.getInstance().deleteDataset( id );

        FacesMessage msg = GuiUtils.getFacesMessage( fc, FacesMessage.SEVERITY_INFO, "SUCCESS.DELETE_DATASET", id );
        fc.addMessage( "DELETE_DATASET_SUCCESS", msg );
        return "/page/form/successPage.xhtml";
    }

    public Object saveDataset() {
        LOG.debug( "Save dataset" );
        String id;
        FacesContext fc = FacesContext.getCurrentInstance();
        FormFieldBean formfieldBean = (FormFieldBean) fc.getApplication().getELResolver().getValue( fc.getELContext(),
                                                                                                    null,
                                                                                                    "formFieldBean" );

        HttpSession session = (HttpSession) fc.getExternalContext().getSession( false );
        try {
            FormConfiguration manager = FormConfigurationFactory.getOrCreateFormConfiguration( session.getId() );

            FormFieldPath pathToIdentifier = manager.getPathToIdentifier();
            Object datasetId = formfieldBean.getFormFields().get( pathToIdentifier.toString() ).getValue();
            id = String.valueOf( datasetId );
            if ( datasetId == null || id == null || id.length() == 0 ) {
                FacesMessage msg = GuiUtils.getFacesMessage( fc, FacesMessage.SEVERITY_FATAL,
                                                             "ERROR.SAVE_DATASET.INVALID_ID" );
                fc.addMessage( "SAVE_FAILED_INVALID_ID", msg );
            } else {
                DataHandler.getInstance().writeDataset( id, formfieldBean.getFormGroups(),
                                                        formfieldBean.getDataGroups() );

                FacesMessage msg = GuiUtils.getFacesMessage( fc, FacesMessage.SEVERITY_INFO, "SUCCESS.SAVE_DATASET", id );
                fc.addMessage( "SAVE_SUCCESS", msg );

                // set the selected dataset
                setSelectedDataset( id );
            }

        } catch ( Exception e ) {
            FacesMessage msg = GuiUtils.getFacesMessage( fc, FacesMessage.SEVERITY_FATAL, "ERROR.SAVE_DATASET",
                                                         e.getMessage() );
            fc.addMessage( "SAVE_FAILED", msg );
        }
        return "forms";
    }

}
