/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2018  Poul Henriksen and Michael Kolling
 
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
package greenfoot.guifx;

import bluej.GuiHandler;
import bluej.Main;
import bluej.pkgmgr.Project;
import greenfoot.core.ProjectManager;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.File;

/**
 * A GUI handler for Greenfoot.
 *
 * @author Davin McCall
 */
@OnThread(Tag.FXPlatform)
public class GreenfootGuiHandler implements GuiHandler {
    @Override
    public boolean tryOpen(File path, boolean displayError) {
        Project project = Project.openProject(path.toString());
        if (project != null) {
            ProjectManager.instance().launchProject(project);
            return true;
        } else {
            return GreenfootStage.openArchive(path, null);
            // TODO: display error dialog if displayError == true
        }
    }

    @Override
    public void handleAbout() {
        GreenfootStage.aboutGreenfoot(null);
    }

    @Override
    public void handlePreferences() {
        GreenfootStage.showPreferences();
    }

    @Override
    public void handleQuit() {
        Main.wantToQuit();
    }

    @Override
    public void initialOpenComplete(boolean projectOpen) {
        if (!projectOpen) {
            GreenfootStage.makeStage(null, null).show();
        }
    }

    @Override
    public void doExitCleanup() {
        GreenfootStage.closeAll();
    }
}
