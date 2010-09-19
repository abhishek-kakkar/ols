/*
 * OpenBench LogicSniffer / SUMP project
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *
 * Copyright (C) 2006-2010 Michael Poppitz, www.sump.org
 * Copyright (C) 2010 J.W. Janssen, www.lxtreme.nl
 */
package nl.lxtreme.ols.tool.uart;


import static nl.lxtreme.ols.util.ExportUtils.HtmlExporter.*;

import java.awt.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.List;
import java.util.logging.*;

import javax.swing.*;

import org.osgi.service.prefs.*;

import nl.lxtreme.ols.tool.base.*;
import nl.lxtreme.ols.util.*;
import nl.lxtreme.ols.util.ExportUtils.*;
import nl.lxtreme.ols.util.swing.*;


/**
 * The Dialog Class
 * 
 * @author Frank Kunz The dialog class draws the basic dialog with a grid
 *         layout. The dialog consists of three main parts. A settings panel, a
 *         table panel and three buttons.
 */
public final class UARTProtocolAnalysisDialog extends BaseAsyncToolDialog<UARTDataSet, UARTAnalyserWorker>
{
  // CONSTANTS

  private static final long serialVersionUID = 1L;

  private static final Logger LOG = Logger.getLogger( UARTProtocolAnalysisDialog.class.getName() );

  // VARIABLES

  private final JComboBox rxd;
  private final JComboBox txd;
  private final JComboBox cts;
  private final JComboBox rts;
  private final JComboBox dtr;
  private final JComboBox dsr;
  private final JComboBox dcd;
  private final JComboBox ri;
  private final JComboBox parity;
  private final JComboBox bits;
  private final JComboBox stop;
  private final JCheckBox inv;
  private final JEditorPane outText;

  private final RestorableAction runAnalysisAction;
  private final Action exportAction;
  private final Action closeAction;

  // CONSTRUCTORS

  /**
   * @param aOwner
   * @param aName
   */
  public UARTProtocolAnalysisDialog( final Window aOwner, final String aName )
  {
    super( aOwner, aName );

    setMinimumSize( new Dimension( 640, 480 ) );

    setLayout( new GridBagLayout() );
    getRootPane().setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );

    /*
     * add protocol settings elements
     */
    final JPanel panSettings = new JPanel();
    panSettings.setLayout( new GridLayout( 12, 2, 5, 5 ) );
    panSettings.setBorder( BorderFactory.createCompoundBorder( BorderFactory.createTitledBorder( "Settings" ),
        BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) ) );

    final String channels[] = new String[33];
    for ( int i = 0; i < 32; i++ )
    {
      channels[i] = new String( "Channel " + i );
    }
    channels[channels.length - 1] = new String( "unused" );

    panSettings.add( new JLabel( "RxD" ) );
    this.rxd = new JComboBox( channels );
    panSettings.add( this.rxd );

    panSettings.add( new JLabel( "TxD" ) );
    this.txd = new JComboBox( channels );
    panSettings.add( this.txd );

    panSettings.add( new JLabel( "CTS" ) );
    this.cts = new JComboBox( channels );
    this.cts.setSelectedItem( "unused" );
    panSettings.add( this.cts );

    panSettings.add( new JLabel( "RTS" ) );
    this.rts = new JComboBox( channels );
    this.rts.setSelectedItem( "unused" );
    panSettings.add( this.rts );

    panSettings.add( new JLabel( "DTR" ) );
    this.dtr = new JComboBox( channels );
    this.dtr.setSelectedItem( "unused" );
    panSettings.add( this.dtr );

    panSettings.add( new JLabel( "DSR" ) );
    this.dsr = new JComboBox( channels );
    this.dsr.setSelectedItem( "unused" );
    panSettings.add( this.dsr );

    panSettings.add( new JLabel( "DCD" ) );
    this.dcd = new JComboBox( channels );
    this.dcd.setSelectedItem( "unused" );
    panSettings.add( this.dcd );

    panSettings.add( new JLabel( "RI" ) );
    this.ri = new JComboBox( channels );
    this.ri.setSelectedItem( "unused" );
    panSettings.add( this.ri );

    panSettings.add( new JLabel( "Parity" ) );
    final String[] parityarray = new String[] { "None", "Odd", "Even" };
    this.parity = new JComboBox( parityarray );
    panSettings.add( this.parity );

    panSettings.add( new JLabel( "Bits" ) );
    final String[] bitarray = new String[4];
    for ( int i = 0; i < bitarray.length; i++ )
    {
      bitarray[i] = new String( "" + ( i + 5 ) );
    }
    this.bits = new JComboBox( bitarray );
    this.bits.setSelectedItem( "8" );
    panSettings.add( this.bits );

    panSettings.add( new JLabel( "Stopbit" ) );
    final String[] stoparray = new String[] { "1", "1.5", "2" };
    this.stop = new JComboBox( stoparray );
    panSettings.add( this.stop );

    this.inv = new JCheckBox();
    panSettings.add( new JLabel( "Invert" ) );
    panSettings.add( this.inv );

    add( panSettings, createConstraints( 0, 0, 1, 1, 0, 0 ) );

    /*
     * add an empty output view
     */
    final JPanel panTable = new JPanel( new GridLayout( 1, 1, 5, 5 ) );
    this.outText = new JEditorPane( "text/html", getEmptyHtmlPage() );
    this.outText.setMargin( new Insets( 5, 5, 5, 5 ) );
    panTable.add( new JScrollPane( this.outText ) );
    add( panTable, createConstraints( 1, 0, 1, 1, 1.0, 1.0 ) );

    /*
     * add buttons
     */
    final JButton runAnalysisButton = createRunAnalysisButton();
    this.runAnalysisAction = ( RestorableAction )runAnalysisButton.getAction();

    final JButton exportButton = createExportButton();
    this.exportAction = exportButton.getAction();
    this.exportAction.setEnabled( false );

    final JButton closeButton = createCloseButton();
    this.closeAction = closeButton.getAction();

    final JPanel buttons = new JPanel();
    final BoxLayout layoutMgr = new BoxLayout( buttons, BoxLayout.LINE_AXIS );
    buttons.setLayout( layoutMgr );
    buttons.add( Box.createHorizontalGlue() );
    buttons.add( runAnalysisButton );
    buttons.add( Box.createHorizontalStrut( 8 ) );
    buttons.add( exportButton );
    buttons.add( Box.createHorizontalStrut( 16 ) );
    buttons.add( closeButton );

    add( buttons, //
        new GridBagConstraints( 0, 1, 2, 1, 1.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
            COMP_INSETS, 0, 0 ) );

    pack();
  }

  // METHODS

  /**
   * @param x
   * @param y
   * @param w
   * @param h
   * @param wx
   * @param wy
   * @return
   */
  private static GridBagConstraints createConstraints( final int x, final int y, final int w, final int h,
      final double wx, final double wy )
  {
    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.BOTH;
    gbc.insets = new Insets( 4, 4, 4, 4 );
    gbc.gridx = x;
    gbc.gridy = y;
    gbc.gridwidth = w;
    gbc.gridheight = h;
    gbc.weightx = wx;
    gbc.weighty = wy;
    return ( gbc );
  }

  /**
   * This is the UART protocol decoder core The decoder scans for a decode start
   * event like CS high to low edge or the trigger of the captured data. After
   * this the decoder starts to decode the data by the selected mode, number of
   * bits and bit order. The decoded data are put to a JTable object directly.
   */
  @Override
  public void onToolWorkerReady( final UARTDataSet aAnalysisResult )
  {
    super.onToolWorkerReady( aAnalysisResult );

    try
    {
      final String htmlPage;
      if ( aAnalysisResult != null )
      {
        htmlPage = toHtmlPage( null /* aFile */, aAnalysisResult );
        this.exportAction.setEnabled( !aAnalysisResult.isEmpty() );
      }
      else
      {
        htmlPage = getEmptyHtmlPage();
        this.exportAction.setEnabled( false );
      }

      this.outText.setText( htmlPage );
      this.outText.setEditable( false );

      this.runAnalysisAction.restore();
    }
    catch ( final IOException exception )
    {
      // Make sure to handle IO-interrupted exceptions properly!
      HostUtils.handleInterruptedException( exception );

      // Should not happen in this situation!
      throw new RuntimeException( exception );
    }
  }

  /**
   * @see nl.lxtreme.ols.api.Configurable#readPreferences(org.osgi.service.prefs.Preferences)
   */
  public void readPreferences( final Preferences aPrefs )
  {
    SwingComponentUtils.setSelectedIndex( this.rxd, aPrefs.getInt( "rxd", -1 ) );
    SwingComponentUtils.setSelectedIndex( this.txd, aPrefs.getInt( "txd", -1 ) );
    SwingComponentUtils.setSelectedIndex( this.cts, aPrefs.getInt( "cts", -1 ) );
    SwingComponentUtils.setSelectedIndex( this.rts, aPrefs.getInt( "rts", -1 ) );
    SwingComponentUtils.setSelectedIndex( this.dtr, aPrefs.getInt( "dtr", -1 ) );
    SwingComponentUtils.setSelectedIndex( this.dsr, aPrefs.getInt( "dsr", -1 ) );
    SwingComponentUtils.setSelectedIndex( this.dcd, aPrefs.getInt( "dcd", -1 ) );
    SwingComponentUtils.setSelectedIndex( this.ri, aPrefs.getInt( "ri", -1 ) );
    SwingComponentUtils.setSelectedIndex( this.parity, aPrefs.getInt( "parity", -1 ) );
    SwingComponentUtils.setSelectedIndex( this.bits, aPrefs.getInt( "bits", -1 ) );
    SwingComponentUtils.setSelectedIndex( this.stop, aPrefs.getInt( "stop", -1 ) );
    SwingComponentUtils.setSelected( this.inv, aPrefs.getBoolean( "inverted", Boolean.FALSE ) );
  }

  /**
   * @see nl.lxtreme.ols.tool.base.ToolDialog#reset()
   */
  @Override
  public void reset()
  {
    this.outText.setText( getEmptyHtmlPage() );
    this.outText.setEditable( false );

    this.exportAction.setEnabled( false );

    this.runAnalysisAction.restore();

    setControlsEnabled( true );
  }

  /**
   * @see nl.lxtreme.ols.api.Configurable#writePreferences(org.osgi.service.prefs.Preferences)
   */
  public void writePreferences( final Preferences aProperties )
  {
    aProperties.putInt( "rxd", this.rxd.getSelectedIndex() );
    aProperties.putInt( "txd", this.txd.getSelectedIndex() );
    aProperties.putInt( "cts", this.cts.getSelectedIndex() );
    aProperties.putInt( "rts", this.rts.getSelectedIndex() );
    aProperties.putInt( "dtr", this.dtr.getSelectedIndex() );
    aProperties.putInt( "dsr", this.dsr.getSelectedIndex() );
    aProperties.putInt( "dcd", this.dcd.getSelectedIndex() );
    aProperties.putInt( "ri", this.ri.getSelectedIndex() );
    aProperties.putInt( "parity", this.parity.getSelectedIndex() );
    aProperties.putInt( "bits", this.bits.getSelectedIndex() );
    aProperties.putInt( "stop", this.stop.getSelectedIndex() );
    aProperties.putBoolean( "inverted", this.inv.isSelected() );
  }

  /**
   * set the controls of the dialog enabled/disabled
   * 
   * @param aEnable
   *          status of the controls
   */
  @Override
  protected void setControlsEnabled( final boolean aEnable )
  {
    this.rxd.setEnabled( aEnable );
    this.txd.setEnabled( aEnable );
    this.cts.setEnabled( aEnable );
    this.rts.setEnabled( aEnable );
    this.dtr.setEnabled( aEnable );
    this.dsr.setEnabled( aEnable );
    this.dcd.setEnabled( aEnable );
    this.ri.setEnabled( aEnable );
    this.parity.setEnabled( aEnable );
    this.bits.setEnabled( aEnable );
    this.stop.setEnabled( aEnable );
    this.inv.setEnabled( aEnable );

    this.closeAction.setEnabled( aEnable );
  }

  /**
   * @see nl.lxtreme.ols.tool.base.BaseAsyncToolDialog#setupToolWorker(nl.lxtreme.ols.tool.base.BaseAsyncToolWorker)
   */
  @Override
  protected void setupToolWorker( final UARTAnalyserWorker aToolWorker )
  {
    if ( !"unused".equals( this.rxd.getSelectedItem() ) )
    {
      aToolWorker.setRxdIndex( this.rxd.getSelectedIndex() );
    }

    if ( !"unused".equals( this.txd.getSelectedItem() ) )
    {
      aToolWorker.setTxdIndex( this.txd.getSelectedIndex() );
    }

    if ( !"unused".equals( this.cts.getSelectedItem() ) )
    {
      aToolWorker.setCtsIndex( this.cts.getSelectedIndex() );
    }

    if ( !"unused".equals( this.rts.getSelectedItem() ) )
    {
      aToolWorker.setRtsIndex( this.rts.getSelectedIndex() );
    }

    if ( !"unused".equals( this.dcd.getSelectedItem() ) )
    {
      aToolWorker.setDcdIndex( this.dcd.getSelectedIndex() );
    }

    if ( !"unused".equals( this.ri.getSelectedItem() ) )
    {
      aToolWorker.setRiIndex( this.ri.getSelectedIndex() );
    }

    if ( !"unused".equals( this.dsr.getSelectedItem() ) )
    {
      aToolWorker.setDsrIndex( this.dsr.getSelectedIndex() );
    }

    if ( !"unused".equals( this.dtr.getSelectedItem() ) )
    {
      aToolWorker.setDtrIndex( this.dtr.getSelectedIndex() );
    }

    // Other properties...
    aToolWorker.setInverted( this.inv.isSelected() );
    aToolWorker.setParity( UARTParity.parse( this.parity.getSelectedItem() ) );
    aToolWorker.setStopBits( UARTStopBits.parse( this.stop.getSelectedItem() ) );
    aToolWorker.setBitCount( NumberUtils.smartParseInt( ( String )this.bits.getSelectedItem(), 8 ) );
  }

  /**
   * exports the data to a CSV file
   * 
   * @param aFile
   *          File object
   */
  @Override
  protected void storeToCsvFile( final File aFile, final UARTDataSet aDataSet )
  {
    try
    {
      final CsvExporter exporter = ExportUtils.createCsvExporter( aFile );

      exporter.setHeaders( "index", "start-time", "end-time", "event?", "event-type", "RxD event", "TxD event",
          "RxD data", "TxD data" );

      final List<UARTData> decodedData = aDataSet.getData();
      for ( int i = 0; i < decodedData.size(); i++ )
      {
        final UARTData ds = decodedData.get( i );

        final String startTime = aDataSet.getDisplayTime( ds.getStartSampleIndex() );
        final String endTime = aDataSet.getDisplayTime( ds.getEndSampleIndex() );

        String eventType = null;
        String rxdEvent = null;
        String txdEvent = null;
        String rxdData = null;
        String txdData = null;

        switch ( ds.getType() )
        {
          case UARTData.UART_TYPE_EVENT:
            eventType = ds.getEventName();
            break;

          case UARTData.UART_TYPE_RXEVENT:
            rxdEvent = ds.getEventName();
            break;

          case UARTData.UART_TYPE_TXEVENT:
            txdEvent = ds.getEventName();
            break;

          case UARTData.UART_TYPE_RXDATA:
            rxdData = Integer.toString( ds.getData() );
            break;

          case UARTData.UART_TYPE_TXDATA:
            txdData = Integer.toString( ds.getData() );
            break;

          default:
            break;
        }

        exporter.addRow( i, startTime, endTime, ds.isEvent(), eventType, rxdEvent, txdEvent, rxdData, txdData );
      }

      exporter.close();
    }
    catch ( final IOException exception )
    {
      // Make sure to handle IO-interrupted exceptions properly!
      HostUtils.handleInterruptedException( exception );

      if ( LOG.isLoggable( Level.WARNING ) )
      {
        LOG.log( Level.WARNING, "CSV export failed!", exception );
      }
    }
  }

  /**
   * stores the data to a HTML file
   * 
   * @param aFile
   *          file object
   */
  @Override
  protected void storeToHtmlFile( final File aFile, final UARTDataSet aDataSet )
  {
    try
    {
      toHtmlPage( aFile, aDataSet );
    }
    catch ( final IOException exception )
    {
      // Make sure to handle IO-interrupted exceptions properly!
      HostUtils.handleInterruptedException( exception );

      if ( LOG.isLoggable( Level.WARNING ) )
      {
        LOG.log( Level.WARNING, "HTML export failed!", exception );
      }
    }
  }

  /**
   * Creates the HTML template for exports to HTML.
   * 
   * @param aExporter
   *          the HTML exporter instance to use, cannot be <code>null</code>.
   * @return a HTML exporter filled with the template, never <code>null</code>.
   */
  private HtmlExporter createHtmlTemplate( final HtmlExporter aExporter )
  {
    aExporter.addCssStyle( "body { font-family: sans-serif; } " );
    aExporter.addCssStyle( "table { border-width: 1px; border-spacing: 0px; border-color: gray;"
        + " border-collapse: collapse; border-style: solid; margin-bottom: 15px; } " );
    aExporter.addCssStyle( "table th { border-width: 1px; padding: 2px; border-style: solid; border-color: gray;"
        + " background-color: #C0C0FF; text-align: left; font-weight: bold; font-family: sans-serif; } " );
    aExporter.addCssStyle( "table td { border-width: 1px; padding: 2px; border-style: solid; border-color: gray;"
        + " font-family: monospace; } " );
    aExporter.addCssStyle( ".error { color: red; } " );
    aExporter.addCssStyle( ".warning { color: orange; } " );
    aExporter.addCssStyle( ".date { text-align: right; font-size: x-small; margin-bottom: 15px; } " );
    aExporter.addCssStyle( ".w100 { width: 100%; } " );
    aExporter.addCssStyle( ".w35 { width: 35%; } " );
    aExporter.addCssStyle( ".w30 { width: 30%; } " );
    aExporter.addCssStyle( ".w15 { width: 15%; } " );
    aExporter.addCssStyle( ".w10 { width: 10%; } " );
    aExporter.addCssStyle( ".w8 { width: 8%; } " );
    aExporter.addCssStyle( ".w7 { width: 7%; } " );

    final Element body = aExporter.getBody();
    body.addChild( H1 ).addContent( "UART Analysis results" );
    body.addChild( HR );
    body.addChild( DIV ).addAttribute( "class", "date" ).addContent( "Generated: ", "{date-now}" );

    Element table, tr, thead, tbody;

    table = body.addChild( TABLE ).addAttribute( "class", "w100" );

    tbody = table.addChild( TBODY );
    tr = tbody.addChild( TR );
    tr.addChild( TH ).addAttribute( "colspan", "2" ).addContent( "Statistics" );
    tr = tbody.addChild( TR );
    tr.addChild( TD ).addAttribute( "class", "w30" ).addContent( "Decoded bytes" );
    tr.addChild( TD ).addContent( "{decoded-bytes}" );
    tr = tbody.addChild( TR );
    tr.addChild( TD ).addAttribute( "class", "w30" ).addContent( "Detected bus errors" );
    tr.addChild( TD ).addContent( "{detected-bus-errors}" );
    tr = tbody.addChild( TR );
    tr.addChild( TD ).addAttribute( "class", "w30" ).addContent( "Baudrate" );
    tr.addChild( TD ).addContent( "{baudrate}" );

    table = body.addChild( TABLE ).addAttribute( "class", "w100" );
    thead = table.addChild( THEAD );
    tr = thead.addChild( TR );
    tr.addChild( TH ).addAttribute( "class", "w30" ).addAttribute( "colspan", "2" );
    tr.addChild( TH ).addAttribute( "class", "w35" ).addAttribute( "colspan", "4" ).addContent( "RxD" );
    tr.addChild( TH ).addAttribute( "class", "w35" ).addAttribute( "colspan", "4" ).addContent( "TxD" );
    tr = thead.addChild( TR );
    tr.addChild( TH ).addAttribute( "class", "w15" ).addContent( "Index" );
    tr.addChild( TH ).addAttribute( "class", "w15" ).addContent( "Time" );
    tr.addChild( TH ).addAttribute( "class", "w10" ).addContent( "Hex" );
    tr.addChild( TH ).addAttribute( "class", "w10" ).addContent( "Bin" );
    tr.addChild( TH ).addAttribute( "class", "w8" ).addContent( "Dec" );
    tr.addChild( TH ).addAttribute( "class", "w7" ).addContent( "ASCII" );
    tr.addChild( TH ).addAttribute( "class", "w10" ).addContent( "Hex" );
    tr.addChild( TH ).addAttribute( "class", "w10" ).addContent( "Bin" );
    tr.addChild( TH ).addAttribute( "class", "w8" ).addContent( "Dec" );
    tr.addChild( TH ).addAttribute( "class", "w7" ).addContent( "ASCII" );
    tbody = table.addChild( TBODY );
    tbody.addContent( "{decoded-data}" );

    return aExporter;
  }

  /**
   * generate a HTML page
   * 
   * @param empty
   *          if this is true an empty output is generated
   * @return String with HTML data
   */
  private String getEmptyHtmlPage()
  {
    final HtmlExporter exporter = createHtmlTemplate( ExportUtils.createHtmlExporter() );
    return exporter.toString( new MacroResolver()
    {
      @Override
      public Object resolve( final String aMacro, final Element aParent )
      {
        if ( "date-now".equals( aMacro ) )
        {
          final DateFormat df = DateFormat.getDateInstance( DateFormat.LONG );
          return df.format( new Date() );
        }
        else if ( "decoded-bytes".equals( aMacro ) || "detected-bus-errors".equals( aMacro )
            || "baudrate".equals( aMacro ) )
        {
          return "-";
        }
        else if ( "decoded-data".equals( aMacro ) )
        {
          return null;
        }
        return null;
      }
    } );
  }

  /**
   * generate a HTML page
   * 
   * @param empty
   *          if this is true an empty output is generated
   * @return String with HTML data
   */
  private String toHtmlPage( final File aFile, final UARTDataSet aDataSet ) throws IOException
  {
    final int bitCount = Integer.parseInt( ( String )this.bits.getSelectedItem() );
    final int bitAdder = ( bitCount % 4 != 0 ) ? 1 : 0;

    final MacroResolver macroResolver = new MacroResolver()
    {
      @Override
      public Object resolve( final String aMacro, final Element aParent )
      {
        if ( "date-now".equals( aMacro ) )
        {
          final DateFormat df = DateFormat.getDateInstance( DateFormat.LONG );
          return df.format( new Date() );
        }
        else if ( "decoded-bytes".equals( aMacro ) )
        {
          return aDataSet.getDecodedSymbols();
        }
        else if ( "detected-bus-errors".equals( aMacro ) )
        {
          return aDataSet.getDetectedErrors();
        }
        else if ( "baudrate".equals( aMacro ) )
        {
          final String baudrate;
          if ( aDataSet.getBitLength() <= 0 )
          {
            baudrate = "<span class='error'>Baudrate calculation failed!</span>";
          }
          else
          {
            baudrate = String.format( "%d (exact: %d)", aDataSet.getBaudRate(), aDataSet.getBaudRateExact() );
            if ( aDataSet.getBitLength() < 15 )
            {
              return baudrate
                  .concat( " <span class='warning'>The baudrate may be wrong, use a higher samplerate to avoid this!</span>" );
            }

            return baudrate;
          }
        }
        else if ( "decoded-data".equals( aMacro ) )
        {
          final List<UARTData> decodedData = aDataSet.getData();
          Element tr;

          for ( int i = 0; i < decodedData.size(); i++ )
          {
            final UARTData ds = decodedData.get( i );

            if ( ds.isEvent() )
            {
              String rxEventData = "";
              String txEventData = "";

              String bgColor;
              if ( UARTData.UART_TYPE_EVENT == ds.getType() )
              {
                rxEventData = txEventData = ds.getEventName();
                bgColor = "#e0e0e0";
              }
              else if ( UARTData.UART_TYPE_RXEVENT == ds.getType() )
              {
                rxEventData = ds.getEventName();
                bgColor = "#c0ffc0";
              }
              else if ( UARTData.UART_TYPE_TXEVENT == ds.getType() )
              {
                txEventData = ds.getEventName();
                bgColor = "#c0ffc0";
              }
              else
              {
                // unknown event
                bgColor = "#ff8000";
              }

              if ( txEventData.endsWith( "_ERR" ) || rxEventData.endsWith( "_ERR" ) )
              {
                bgColor = "#ff8000";
              }

              tr = aParent.addChild( TR ).addAttribute( "style", "background-color: " + bgColor + ";" );
              tr.addChild( TD ).addContent( String.valueOf( i ) );
              tr.addChild( TD ).addContent( aDataSet.getDisplayTime( ds.getStartSampleIndex() ) );
              tr.addChild( TD ).addContent( rxEventData );
              tr.addChild( TD );
              tr.addChild( TD );
              tr.addChild( TD );
              tr.addChild( TD ).addContent( txEventData );
              tr.addChild( TD );
              tr.addChild( TD );
              tr.addChild( TD );
            }
            else
            {
              String rxDataHex = "", rxDataBin = "", rxDataDec = "", rxDataASCII = "";
              String txDataHex = "", txDataBin = "", txDataDec = "", txDataASCII = "";

              // Normal data...
              if ( UARTData.UART_TYPE_RXDATA == ds.getType() )
              {
                final int rxData = ds.getData();

                rxDataHex = "0x" + DisplayUtils.integerToHexString( rxData, bitCount / 4 + bitAdder );
                rxDataBin = "0b" + DisplayUtils.integerToBinString( rxData, bitCount );
                rxDataDec = String.valueOf( rxData );
                if ( ( rxData >= 32 ) && ( bitCount == 8 ) )
                {
                  rxDataASCII = String.valueOf( ( char )rxData );
                }
              }
              else
              /* if ( UARTData.UART_TYPE_TXDATA == ds.getType() ) */
              {
                final int txData = ds.getData();

                txDataHex = "0x" + DisplayUtils.integerToHexString( txData, bitCount / 4 + bitAdder );
                txDataBin = "0b" + DisplayUtils.integerToBinString( txData, bitCount );
                txDataDec = String.valueOf( txData );
                if ( ( txData >= 32 ) && ( bitCount == 8 ) )
                {
                  txDataASCII = String.valueOf( ( char )txData );
                }
              }

              tr = aParent.addChild( TR );
              tr.addChild( TD ).addContent( String.valueOf( i ) );
              tr.addChild( TD ).addContent( aDataSet.getDisplayTime( ds.getStartSampleIndex() ) );
              tr.addChild( TD ).addContent( rxDataHex );
              tr.addChild( TD ).addContent( rxDataBin );
              tr.addChild( TD ).addContent( rxDataDec );
              tr.addChild( TD ).addContent( rxDataASCII );
              tr.addChild( TD ).addContent( txDataHex );
              tr.addChild( TD ).addContent( txDataBin );
              tr.addChild( TD ).addContent( txDataDec );
              tr.addChild( TD ).addContent( txDataASCII );
            }
          }
        }
        return null;
      }
    };

    if ( aFile == null )
    {
      final HtmlExporter exporter = createHtmlTemplate( ExportUtils.createHtmlExporter() );
      return exporter.toString( macroResolver );
    }
    else
    {
      final HtmlFileExporter exporter = ( HtmlFileExporter )createHtmlTemplate( ExportUtils.createHtmlExporter( aFile ) );
      exporter.write( macroResolver );
      exporter.close();
    }

    return null;
  }
}
