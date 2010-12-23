//$HeadURL$
/*----------------------------------------------------------------------------
 This file is part of deegree, http://deegree.org/
 Copyright (C) 2001-2009 by:
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
package org.deegree.tools.crs.georeferencing.communication.dialog.coordinatejump;

import static org.deegree.tools.crs.georeferencing.communication.GUIConstants.DIM_COORDINATEJUMPER;
import static org.deegree.tools.crs.georeferencing.i18n.Messages.get;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JDialog;
import javax.swing.JTextField;

import org.deegree.tools.crs.georeferencing.communication.dialog.ButtonPanel;

/**
 * Dialog for jumping to a specific coordinate. It is set to modal == true to disable any other userInteraction.
 * <p>
 * NOTE: This version of jumping to coordinates is not used at the moment.
 * 
 * @author <a href="mailto:thomas@lat-lon.de">Steffen Thomas</a>
 * @author last edited by: $Author$
 * 
 * @version $Revision$, $Date$
 */
public class CoordinateJumperTextfieldDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 6002369721730648141L;

    private JTextField coordinateJumper;

    private ButtonPanel buttons;

    private double[] coords;

    private boolean wasOk;

    public CoordinateJumperTextfieldDialog( Component parent ) {
        this.setLayout( new BorderLayout() );

        setTitle( get( "ZOOM_COORDINATE" ) );

        this.setPreferredSize( DIM_COORDINATEJUMPER );
        this.setBounds(
                        new Double( parent.getBounds().getCenterX() - ( DIM_COORDINATEJUMPER.getWidth() / 2 ) ).intValue(),
                        new Double( parent.getBounds().getCenterY() - ( DIM_COORDINATEJUMPER.getHeight() / 2 ) ).intValue(),
                        new Double( DIM_COORDINATEJUMPER.getWidth() ).intValue(),
                        new Double( DIM_COORDINATEJUMPER.getHeight() ).intValue() );
        this.setModal( true );
        this.setResizable( false );
        buttons = new ButtonPanel();
        buttons.addListeners( this );

        coordinateJumper = new JTextField( 15 );
        this.add( coordinateJumper, BorderLayout.CENTER );
        this.add( buttons, BorderLayout.SOUTH );
        this.pack();
    }

    public double[] getCoords() {
        return coords;
    }

    public boolean wasOk() {
        return wasOk;
    }

    private void parse( String s1, String s2 ) {
        coords[0] = Double.parseDouble( s1 );
        coords[1] = Double.parseDouble( s2 );
    }

    public void actionPerformed( ActionEvent evt ) {
        if ( evt.getSource() == buttons.getOk() ) {
            wasOk = true;
            String s = coordinateJumper.getText();
            if ( s == null || s.isEmpty() ) {
                setVisible( false );
                return;
            }
            String[] ss = s.split( " " );
            if ( ss.length == 2 ) {
                try {
                    coords = new double[2];
                    parse( ss[0], ss[1] );
                } catch ( NumberFormatException e ) {
                    try {
                        parse( ss[0].replace( ",", "." ), ss[1].replace( ",", "." ) );
                    } catch ( NumberFormatException e1 ) {
                        coords = null;
                    }
                }
            } else {
                ss = s.split( "," );
                if ( ss.length == 2 ) {
                    try {
                        coords = new double[2];
                        parse( ss[0], ss[1] );
                    } catch ( NumberFormatException e ) {
                        coords = null;
                    }
                }
            }
        }
        if ( evt.getSource() == buttons.getCancel() ) {
            wasOk = false;
        }
        setVisible( false );
    }

}
