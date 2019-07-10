/*
 This file is part of the BlueJ program. 
 Copyright (C) 2017  Michael Kolling and John Rosenberg
 
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
package bluej.pkgmgr.t4rget;

import bluej.compiler.CompileReason;
import bluej.compiler.CompileType;
import bluej.pkgmgr.Package;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A target that is editable, but only as normal text (i.e. not as Java code), such as README and CSS files.
 */
public abstract class NonCodeEditableTarget extends EditableTarget {
    protected NonCodeEditableTarget(Package pkg, String name) {
        super(pkg, name);
    }

    @Override
    public void generateDoc() {
        // meaningless
    }

    @Override
    public String getProperty(String key) {
        return null;
    }

    @Override
    public void setProperty(String key, String value) {
    }

    @Override
    @OnThread(Tag.Any)
    public void scheduleCompilation(boolean immediate, CompileReason reason, CompileType type) {
    }

    @Override
    public void showingInterface(boolean showingInterface) {
        // Not applicable
    }
}
