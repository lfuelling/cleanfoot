/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2012,2013,2014,2015,2016,2017,2018  Michael Kolling and John Rosenberg
 
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
package bluej;

import bluej.extensions.event.ApplicationEvent;
import bluej.extmgr.ExtensionsManager;
import bluej.pkgmgr.Project;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.javafx.JavaFXUtil;
import com.apple.eawt.Application;
import com.apple.eawt.QuitResponse;
import de.codecentric.centerdevice.MenuToolkit;
import de.codecentric.centerdevice.dialogs.about.AboutStageBuilder;
import greenfoot.guifx.GreenfootGuiHandler;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import threadchecker.OnThread;
import threadchecker.Tag;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * BlueJ starts here. The Boot class, which is responsible for dealing with
 * specialised class loaders, constructs an object of this class to initiate the
 * "real" BlueJ.
 *
 * @author Michael Kolling
 */
public class Main {
    /**
     * Whether we've officially launched yet. While false "open file" requests only
     * set initialProject.
     */
    private static boolean launched = false;

    /**
     * On MacOS X, this will be set to the project we should open (if any)
     */
    private static List<File> initialProjects;

    private static QuitResponse macEventResponse = null;  // used to respond to external quit events on MacOS

    /**
     * Only used on Mac.  For some reason, executing the AppleJavaExtensions open
     * file handler (that is set from Boot.main) on initial load (e.g. because
     * the user double-clicked a project.greenfoot file) means that later on,
     * the context class loader is null on the JavaFX thread.
     * Honestly, I [NB] have no idea what the hell is going on there.
     * But the work-around is apparent: store the context class loader early on,
     * then if it is null later, restore it.  (There's no problem if the file
     * handler is not executed; the context class loader is the same early on as
     * later on the FX thread).
     */
    private static ClassLoader storedContextClassLoader;

    /**
     * The mechanism to show the initial GUI
     */
    private static GuiHandler guiHandler = null;

    /**
     * Entry point to starting up the system. Initialise the system and start
     * the first package manager frame.
     */
    @OnThread(Tag.Any)
    public Main() {
        Boot boot = Boot.getInstance();
        final String[] args = Boot.cmdLineArgs;
        Properties commandLineProps = boot.getCommandLineProperties();
        File bluejLibDir = Boot.getBluejLibDir();

        Config.initialise(bluejLibDir, commandLineProps, boot.isGreenfoot());

        if (guiHandler == null) {
            if(Config.isGreenfoot()) {
                guiHandler = new GreenfootGuiHandler();
            } else {
                guiHandler = new BlueJGuiHandler();
            }
        }

        // Note we must do this OFF the AWT dispatch thread. On MacOS X, if the
        // application was started by double-clicking a project file, an "open file"
        // event will be generated once we add a listener and will be delivered on
        // the dispatch thread. It will then be processed before the call to
        // processArgs() (just below) is called.
        if (Config.isMacOS()) {
            prepareMacOSApp();
        }

        // process command line arguments, start BlueJ!
        SwingUtilities.invokeLater(() -> Platform.runLater(() -> processArgs(args)));

        // Send usage data back to bluej.org
        new Thread(Main::updateStats).start();
    }

    /**
     * Start everything off. This is used to open the projects specified on the
     * command line when starting BlueJ. Any parameters starting with '-' are
     * ignored for now.
     */
    @OnThread(Tag.FXPlatform)
    private static void processArgs(String[] args) {
        launched = true;

        boolean oneOpened = false;

        // Open any projects specified on the command line
        if (args.length > 0) {
            for (String arg : args) {
                if (!arg.startsWith("-")) {
                    oneOpened |= guiHandler.tryOpen(new File(arg), true);
                }
            }
        }

        // Open a project if requested by the OS (Mac OS)
        if (initialProjects != null) {
            for (File initialProject : initialProjects) {
                oneOpened |= guiHandler.tryOpen(initialProject, true);
            }
        }

        // if we have orphaned packages, these are re-opened
        if (!oneOpened) {
            // check for orphans...
            boolean openOrphans = "true".equals(Config.getPropString("bluej.autoOpenLastProject"));
            if (openOrphans && hadOrphanPackages()) {
                String exists = "";
                // iterate through unknown number of orphans
                for (int i = 1; exists != null; i++) {
                    exists = Config.getPropString(Config.BLUEJ_OPENPACKAGE + i, null);
                    if (exists != null) {
                        oneOpened |= guiHandler.tryOpen(new File(exists), false);
                    }
                }
            }
        }

        guiHandler.initialOpenComplete(oneOpened);

        Boot.getInstance().disposeSplashWindow();
        ExtensionsManager.getInstance().delegateEvent(new ApplicationEvent(ApplicationEvent.APP_READY_EVENT));
    }

    /**
     * Prepare MacOS specific behaviour (About menu, Preferences menu, Quit
     * menu)
     */
    private static void prepareMacOSApp() {
        storedContextClassLoader = Thread.currentThread().getContextClassLoader();
        initialProjects = Boot.getMacInitialProjects();
        Application macApp = Application.getApplication();

        // Even though BlueJ is JavaFX, the open-files handling still goes
        // through the com.eawt.*/AppleJavaExtensions handling, once it is loaded
        // So we must do the handler set up for AWT/Swing even though we're running
        // in JavaFX.  May need revisiting in Java 9 or whenever they fix the Mac
        // open files handling issue:
        prepareMacOSMenuSwing(macApp);

        // We are using the NSMenuFX library to fix Mac Application menu only when it is a FX
        // menu. When the JDK APIs (i.e. handleAbout() etc) are fixed, both should go back to
        // the way as in prepareMacOSMenuSwing().
        if (macApp != null) {
            prepareMacOSMenuFX();
        }

        // This is not included in the above condition to avoid future bugs,
        // as this is not related to the application menu and will not be affected
        // when the above condition will.
        if (Config.isGreenfoot()) {
            Debug.message("Disabling App Nap");
            try {
                Runtime.getRuntime().exec("defaults write org.greenfoot NSAppSleepDisabled -bool YES");
            } catch (IOException e) {
                Debug.reportError("Error disabling App Nap", e);
            }
        }
    }

    /**
     * Prepare Mac Application FX menu using the NSMenuFX library.
     * This is needed for due to a bug in the JDK APIs, not responding to
     * handleAbout() etc, when the menu is on FX.
     */
    private static void prepareMacOSMenuFX() {
        Platform.runLater(() -> {
            // Sets the JavaFX fxml Default Class Loader to avoid a fxml LoadException.
            // This used to be fired only in the release not while running from the repository.
            FXMLLoader.setDefaultClassLoader(AboutStageBuilder.class.getClassLoader());
            // Get the toolkit
            MenuToolkit menuToolkit = MenuToolkit.toolkit();
            // Create the default Application menu
            Menu defaultApplicationMenu = menuToolkit.createDefaultApplicationMenu(Config.getApplicationName());
            // Update the existing Application menu
            menuToolkit.setApplicationMenu(defaultApplicationMenu);

            // About
            defaultApplicationMenu.getItems().get(0).setOnAction(event -> guiHandler.handleAbout());

            // Preferences
            // It has been added without a separator due to a bug in the library used
            MenuItem preferences = new MenuItem(Config.getString("menu.tools.preferences"));
            if (Config.hasAcceleratorKey("menu.tools.preferences")) {
                preferences.setAccelerator(Config.getAcceleratorKeyFX("menu.tools.preferences"));
            }
            preferences.setOnAction(event -> guiHandler.handlePreferences());
            defaultApplicationMenu.getItems().add(1, preferences);

            // Quit
            defaultApplicationMenu.getItems().get(defaultApplicationMenu.getItems().size() - 1).
                    setOnAction(event -> guiHandler.handleQuit());
        });
    }

    /**
     * Prepare Mac Application Swing menu using the com.apple.eawt APIs.
     */
    private static void prepareMacOSMenuSwing(Application macApp) {
        if (macApp != null) {
            macApp.setAboutHandler(e -> Platform.runLater(() -> guiHandler.handleAbout()));

            macApp.setPreferencesHandler(e -> Platform.runLater(() -> guiHandler.handlePreferences()));

            macApp.setQuitHandler((e, response) -> {
                macEventResponse = response;
                Platform.runLater(() -> guiHandler.handleQuit());
                // response.confirmQuit() does not need to be called, since System.exit(0) is called explcitly
                // response.cancelQuit() is called to cancel (in wantToQuit())
            });

            macApp.setOpenFileHandler(e -> {
                if (launched) {
                    List<File> files = e.getFiles();
                    Platform.runLater(() ->
                    {
                        for (File file : files) {
                            guiHandler.tryOpen(file, true);
                        }
                    });
                } else {
                    initialProjects = e.getFiles();
                }
            });
        }

        Boot.getInstance().setQuitHandler(() -> Platform.runLater(() -> guiHandler.handleQuit()));
    }

    /**
     * Handle the "quit" command: if any projects are open, prompt user to make sure, and quit if
     * confirmed.
     */
    @OnThread(Tag.FXPlatform)
    public static void wantToQuit() {
        int projectCount = Project.getOpenProjectCount();
        // We set a null owner here to make the dialog come to the front of all windows;
        // the user may have triggered the quit shortcut from any window, not just a PkgMgrFrame:
        int answer = projectCount <= 1 ? 0 : DialogManager.askQuestionFX(null, "quit-all");
        if (answer == 0) {
            doQuit();
        } else {
            SwingUtilities.invokeLater(() ->
            {
                if (macEventResponse != null) {
                    macEventResponse.cancelQuit();
                    macEventResponse = null;
                }
            });
        }
    }

    /**
     * Perform the closing down and quitting of BlueJ, including unloading
     * extensions.
     */
    @OnThread(Tag.FXPlatform)
    public static void doQuit() {
        guiHandler.doExitCleanup();

        SwingUtilities.invokeLater(() -> {
            ExtensionsManager extMgr = ExtensionsManager.getInstance();
            extMgr.unloadExtensions();
            Platform.runLater(Main::exit);
        });
    }

    /**
     * Checks if there were orphan packages on last exit by looking for
     * existence of a valid BlueJ project among the saved values for the
     * orphaned packages.
     *
     * @return whether a valid orphaned package exist.
     */
    public static boolean hadOrphanPackages() {
        String dir = "";
        // iterate through unknown number of orphans
        for (int i = 1; dir != null; i++) {
            dir = Config.getPropString(Config.BLUEJ_OPENPACKAGE + i, null);
            if (dir != null) {
                if (Project.isProject(dir)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void updateStats() {
        System.out.println("Data collection would have happened now, but its removed.");
    }

    /**
     * Exit BlueJ.
     * <p>
     * The open frame count should be zero by this point as PkgMgrFrame is
     * responsible for cleaning itself up before getting here.
     */
    @OnThread(Tag.FXPlatform)
    private static void exit() {
        // save configuration properties
        Config.handleExit();
        // exit with success status

        // We wrap this in a Platform.runLater/Swing.invokeLater to make sure it
        // runs after any pending FX actions or Swing actions:
        JavaFXUtil.runAfterCurrent(() -> SwingUtilities.invokeLater(() -> System.exit(0)));
    }

    // See comment on the field.
    public static ClassLoader getStoredContextClassLoader() {
        return storedContextClassLoader;
    }

    /**
     * Set the inital GUI, created after the initial project is opened.
     *
     * @param initialGUI A consume which displays the GUI for the initial project.
     */
    public static void setGuiHandler(GuiHandler initialGUI) {
        Main.guiHandler = initialGUI;
    }
}
