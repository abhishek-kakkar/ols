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
 * 
 * Copyright (C) 2010-2011 - J.W. Janssen, http://www.lxtreme.nl
 */
package nl.lxtreme.ols.runner;


import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.jar.*;

import org.apache.felix.framework.*;
import org.apache.felix.framework.util.*;
import org.osgi.framework.*;
import org.osgi.service.log.*;
import org.osgi.service.packageadmin.*;


/**
 * Provides a main entry point for starting the OLS client from the command
 * line.
 */
public final class Runner
{
  // INNER CLASSES

  static class CmdLineOptions
  {
    // VARIABLES

    final File pluginDir;
    final File cacheDir;
    final boolean cleanCache;
    final boolean logToConsole;
    final int logLevel;

    // CONSTRUCTORS

    public CmdLineOptions( String... aCmdLineArgs ) throws IOException
    {
      String _pluginDir = getPluginDir();
      boolean _cleanCache = false;
      boolean _logToConsole = false;
      int _logLevel = 2;

      for ( String cmdLineArg : aCmdLineArgs )
      {
        if ( "-clean".equals( cmdLineArg ) )
        {
          _cleanCache = true;
        }
        else if ( "-logToConsole".equals( cmdLineArg ) )
        {
          _logToConsole = true;
        }
        else if ( cmdLineArg.startsWith( "-logLevel=" ) )
        {
          String arg = cmdLineArg.substring( 10 );
          _logLevel = Integer.parseInt( arg );
        }
        else if ( cmdLineArg.startsWith( "-pluginDir=" ) )
        {
          String arg = cmdLineArg.substring( 11 );
          _pluginDir = arg;
        }
      }

      if ( _logLevel < 0 || _logLevel > 6 )
      {
        throw new IllegalArgumentException( "Invalid log level, should be between 0 and 5!" );
      }
      if ( !new File( _pluginDir ).exists() )
      {
        throw new IllegalArgumentException( "Invalid plugin directory: no such directory!" );
      }

      this.pluginDir = new File( _pluginDir ).getCanonicalFile();
      this.cacheDir = new File( _pluginDir.concat( "/../.fwcache" ) ).getCanonicalFile();
      this.cleanCache = _cleanCache;
      this.logToConsole = _logToConsole;
      this.logLevel = _logLevel;
    }

    // METHODS

    /**
     * Searches for the plugins directory.
     * <p>
     * This method will take the system property
     * <tt>nl.lxtreme.ols.bundle.dir</tt> into consideration.
     * </p>
     * 
     * @return the fully qualified path to the directory with plugins, never
     *         <code>null</code>.
     * @throws IOException
     *           in case an I/O problem occurred during determining the plugins
     *           path.
     */
    private static String getPluginDir()
    {
      File pluginDir;

      String pluginProperty = System.getProperty( "nl.lxtreme.ols.bundle.dir", "./plugins" );
      pluginDir = new File( pluginProperty );
      if ( pluginDir.exists() && pluginDir.isDirectory() )
      {
        return pluginDir.getAbsolutePath();
      }
      else
      {
        pluginDir = new File( pluginDir, "plugins" );
        if ( pluginDir.exists() && pluginDir.isDirectory() )
        {
          return pluginDir.getAbsolutePath();
        }
      }

      throw new RuntimeException( "Failed to find plugins folder! Is '-Dnl.lxtreme.ols.bundle.dir' specified?" );
    }
  }

  // VARIABLES

  private final CmdLineOptions options;

  private Felix framework;
  private HostActivator hostActivator;

  // CONSTRUCTORS

  /**
   * Creates a new {@link Runner} instance.
   * 
   * @throws IOException
   */
  public Runner( CmdLineOptions aOptions ) throws IOException
  {
    this.options = aOptions;
  }

  // METHODS

  /**
   * MAIN ENTRY POINT
   * 
   * @param aArgs
   *          the (optional) command line arguments, can be empty but never
   *          <code>null</code>.
   */
  public static void main( final String[] aArgs ) throws Exception
  {
    String applicationName = getApplicationName();
    System.setProperty( "com.apple.mrj.application.apple.menu.about.name", applicationName );

    final Runner runner = new Runner( new CmdLineOptions( aArgs ) );
    runner.run();
    runner.waitForStop();
  }

  /**
   * Bootstraps the OSGi framework and bootstraps the application.
   */
  public void run() throws Exception
  {
    try
    {
      this.framework = new Felix( createConfig() );
      this.framework.init();
      this.framework.start();

      // Issue #36: log something about where we're trying to read/store stuff,
      // makes offline debugging a bit easier...
      log( LogService.LOG_INFO, "Framework started..." );
      log( LogService.LOG_INFO, "  plugin dir: " + this.options.pluginDir );
      log( LogService.LOG_INFO, "  cache dir : " + this.options.cacheDir );

      bootstrap( this.framework.getBundleContext() );

      log( LogService.LOG_INFO, "Bootstrap complete..." );
    }
    catch ( Exception exception )
    {
      log( LogService.LOG_ERROR, "Failed to start OSGi framework! Possible reason: " + exception.getMessage(),
          exception );

      throw exception;
    }
  }

  /**
   * @return the application name, never <code>null</code>.
   */
  private static String getApplicationName()
  {
    Class<Runner> clazz = Runner.class;

    URL loc = clazz.getProtectionDomain().getCodeSource().getLocation();

    JarInputStream jis = null;
    try
    {
      jis = new JarInputStream( loc.openStream() );
      Manifest manifest = jis.getManifest();
      if ( manifest != null )
      {
        return manifest.getMainAttributes().getValue( "X-AppName" );
      }
    }
    catch ( Exception exception )
    {
      exception.printStackTrace();
    }
    finally
    {
      try
      {
        if ( jis != null )
        {
          jis.close();
        }
      }
      catch ( IOException exception )
      {
        // Ignore...
      }
    }
    
    return "Runner";
  }

  /**
   * Waits until the OSGi framework is shut down.
   * 
   * @throws InterruptedException
   */
  public void waitForStop() throws InterruptedException
  {
    FrameworkEvent event = this.framework.waitForStop( 0 );
    switch ( event.getType() )
    {
      case FrameworkEvent.STOPPED:
        System.exit( 0 );
      case FrameworkEvent.ERROR:
      case FrameworkEvent.WARNING:
        System.exit( 1 );
      default:
        System.exit( -1 );
    }
  }

  /**
   * Bootstraps the application by installing the new/updated bundles and
   * removing all stale bundles.
   * 
   * @param aContext
   *          the OSGi bundle context to use, cannot be <code>null</code>.
   */
  private void bootstrap( BundleContext aContext ) throws InterruptedException
  {
    Map<String, Bundle> installed = getInstalledBundles( aContext );
    List<Bundle> toBeStarted = new ArrayList<Bundle>();

    List<String> available = getBundles( this.options.pluginDir );
    for ( String bundleLocation : available )
    {
      Bundle bundle = installed.get( bundleLocation );
      if ( bundle == null )
      {
        // New plugin...
        bundle = installBundle( aContext, bundleLocation );
        if ( bundle != null )
        {
          installed.put( bundleLocation, bundle );
          toBeStarted.add( bundle );
        }
      }
      else
      {
        // Plugin exists...
        File file = new File( URI.create( bundleLocation ) );
        if ( file.lastModified() >= bundle.getLastModified() )
        {
          bundle = updateBundle( bundle );
          if ( bundle != null )
          {
            toBeStarted.add( bundle );
          }
        }
      }
    }

    // Remove all installed plugins that are no longer available as plugin...
    List<String> removed = new ArrayList<String>( installed.keySet() );
    removed.remove( Constants.SYSTEM_BUNDLE_LOCATION );
    removed.removeAll( available );
    for ( String plugin : removed )
    {
      Bundle bundle = installed.remove( plugin );
      uninstallBundle( bundle );
    }

    refreshAll( aContext );

    // Start all remaining bundles...
    for ( Bundle bundle : toBeStarted )
    {
      startBundle( bundle );
    }
  }

  private Map<String, Object> createConfig()
  {
    this.hostActivator = new HostActivator( this.options.logToConsole, this.options.logLevel );

    final Map<String, Object> config = new HashMap<String, Object>();

    config.put( Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, "com.apple.mrj,com.apple.eawt,javax.swing,javax.media.jai" );
    // Issue #36: explicitly set the location to the bundle cache directory,
    // otherwise it is created /relatively/ to the current working directory,
    // which is problematic when you start the client with a relative path...
    config.put( Constants.FRAMEWORK_STORAGE, this.options.cacheDir.getPath() );
    if ( this.options.cleanCache )
    {
      config.put( Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT );
    }
    // Felix specific configuration options...
    config.put( FelixConstants.SYSTEMBUNDLE_ACTIVATORS_PROP, Arrays.asList( this.hostActivator ) );
    config.put( FelixConstants.LOG_LEVEL_PROP, "2" );

    return config;
  }

  private List<String> getBundles( final File pluginDir )
  {
    final List<String> plugins = new ArrayList<String>();
    pluginDir.listFiles( new FilenameFilter()
    {

      @Override
      public boolean accept( File aDir, String aName )
      {
        if ( aName.endsWith( ".jar" ) )
        {
          plugins.add( new File( aDir, aName ).toURI().toString() );
        }
        return false;
      }
    } );
    return plugins;
  }

  private Map<String, Bundle> getInstalledBundles( final BundleContext context )
  {
    Map<String, Bundle> installed = new HashMap<String, Bundle>();
    for ( Bundle bundle : context.getBundles() )
    {
      installed.put( bundle.getLocation(), bundle );
    }
    return installed;
  }

  private Bundle installBundle( BundleContext context, String plugin )
  {
    log( LogService.LOG_DEBUG, "Installing plugin: '" + plugin + "'..." );

    try
    {
      return context.installBundle( plugin );
    }
    catch ( BundleException exception )
    {
      log( LogService.LOG_WARNING, "Failed to install bundle: " + plugin + "...", exception );
      return null;
    }
  }

  /**
   * @param aBundle
   *          the bundle to test, can be <code>null</code>.
   * @return <code>true</code> if the given bundle is a fragment bundle,
   *         <code>false</code> otherwise.
   */
  private boolean isFragment( Bundle aBundle )
  {
    if ( aBundle == null )
    {
      return false;
    }
    return aBundle.getHeaders().get( Constants.FRAGMENT_HOST ) != null;
  }

  private void log( int aLevel, String aMessage )
  {
    this.hostActivator.getLogger().log( aLevel, aMessage );
  }

  private void log( int aLevel, String aMessage, Throwable aException )
  {
    this.hostActivator.getLogger().log( aLevel, aMessage, aException );
  }

  @SuppressWarnings( "deprecation" )
  private void refreshAll( BundleContext aContext ) throws InterruptedException
  {
    ServiceReference<?> ref = aContext.getServiceReference( PackageAdmin.class.getName() );
    if ( ref != null )
    {
      PackageAdmin packageAdm = ( PackageAdmin )aContext.getService( ref );

      final CountDownLatch packagesRefreshed = new CountDownLatch( 1 );
      FrameworkListener fwListener = new FrameworkListener()
      {
        @Override
        public void frameworkEvent( FrameworkEvent aEvent )
        {
          switch ( aEvent.getType() )
          {
            case FrameworkEvent.ERROR:
            case FrameworkEvent.WAIT_TIMEDOUT:
            case FrameworkEvent.PACKAGES_REFRESHED:
              packagesRefreshed.countDown();
              break;
          }
        }
      };

      try
      {
        aContext.addFrameworkListener( fwListener );

        packageAdm.refreshPackages( null );

        if ( packagesRefreshed.await( 15, TimeUnit.SECONDS ) )
        {
          if ( !packageAdm.resolveBundles( null ) )
          {
            log( LogService.LOG_WARNING, "Not all bundles resolve correctly!" );
          }
        }
        else
        {
          throw new RuntimeException( "Refresh packages took longer than expected!" );
        }
      }
      finally
      {
        aContext.removeFrameworkListener( fwListener );
        aContext.ungetService( ref );
      }
    }
  }

  private void startBundle( Bundle aBundle )
  {
    if ( !isFragment( aBundle ) )
    {
      log( LogService.LOG_DEBUG, "Starting bundle: " + aBundle.getSymbolicName() + "..." );

      try
      {
        aBundle.start( Bundle.START_ACTIVATION_POLICY );
      }
      catch ( BundleException exception )
      {
        log( LogService.LOG_WARNING, "Failed to start bundle: " + aBundle.getSymbolicName() + "...", exception );
      }
    }
  }

  private void uninstallBundle( Bundle aBundle )
  {
    log( LogService.LOG_DEBUG, "Removing stale plugin: " + aBundle.getSymbolicName() + "..." );

    try
    {
      aBundle.stop();
    }
    catch ( BundleException exception )
    {
      log( LogService.LOG_WARNING, "Failed to stop bundle: " + aBundle.getSymbolicName() + "...", exception );
    }
    try
    {
      aBundle.uninstall();
    }
    catch ( BundleException exception )
    {
      log( LogService.LOG_WARNING, "Failed to uninstall bundle: " + aBundle.getSymbolicName() + "...", exception );
    }
  }

  private Bundle updateBundle( Bundle aBundle )
  {
    log( LogService.LOG_DEBUG, "Updating plugin: " + aBundle.getSymbolicName() + "..." );

    Bundle result = null;
    if ( !isFragment( aBundle ) && ( aBundle.getState() & Bundle.ACTIVE ) != 0 )
    {
      log( LogService.LOG_DEBUG, "Stopping plugin: " + aBundle.getSymbolicName() + " for update..." );

      try
      {
        aBundle.stop();
      }
      catch ( BundleException exception )
      {
        log( LogService.LOG_WARNING, "Failed to stop bundle: " + aBundle.getSymbolicName() + "...", exception );
      }
      result = aBundle;
    }

    try
    {
      aBundle.update();
    }
    catch ( BundleException exception )
    {
      log( LogService.LOG_WARNING, "Failed to update bundle: " + aBundle.getSymbolicName() + "...", exception );
    }
    return result;
  }
}

/* EOF */